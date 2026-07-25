import { db, isFirebaseConfigured } from './firebase';
import { 
  doc, 
  setDoc, 
  updateDoc, 
  collection, 
  addDoc, 
  onSnapshot, 
  query, 
  orderBy, 
  getDocs,
  deleteDoc
} from 'firebase/firestore';

// In-memory fallback for local tab testing
let localChannel = null;
const listeners = {
  session: null,
  history: null
};

// Initialize BroadcastChannel for local synchronization
function getLocalChannel(roomId) {
  if (typeof window === 'undefined') return null;
  if (!localChannel || localChannel.name !== `interpreter-room-${roomId}`) {
    if (localChannel) localChannel.close();
    localChannel = new BroadcastChannel(`interpreter-room-${roomId}`);
    
    localChannel.onmessage = (event) => {
      const { type, data } = event.data;
      if (type === 'SESSION_UPDATE' && listeners.session) {
        listeners.session(data);
      } else if (type === 'HISTORY_ADD' && listeners.history) {
        // Load full history from localStorage to pass to listener
        const history = getLocalHistory(roomId);
        listeners.history(history);
      } else if (type === 'HISTORY_CLEAR' && listeners.history) {
        listeners.history([]);
      }
    };
  }
  return localChannel;
}

// Local Storage helpers
function getLocalSession(roomId) {
  if (typeof window === 'undefined') return null;
  const saved = localStorage.getItem(`session_${roomId}`);
  return saved ? JSON.parse(saved) : {
    buyerInfo: { name: '', company: '', extra: '' },
    sellerKeywords: '',
    status: 'ready', // ready, active, ended
    languages: { seller: 'ko-KR', buyer: 'zh-CN' },
    liveTranscript: null,
    glossary: [],
    timer: { seconds: 0, startTime: null, timerId: null }
  };
}

function saveLocalSession(roomId, data) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(`session_${roomId}`, JSON.stringify(data));
}

function getLocalHistory(roomId) {
  if (typeof window === 'undefined') return [];
  const saved = localStorage.getItem(`history_${roomId}`);
  return saved ? JSON.parse(saved) : [];
}

function saveLocalHistory(roomId, history) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(`history_${roomId}`, JSON.stringify(history));
}

/**
 * Subscribes to room session metadata (e.g. buyerInfo, timer, live transcript)
 */
export function subscribeSession(roomId, callback) {
  if (typeof window === 'undefined') return () => {};

  if (isFirebaseConfigured) {
    const docRef = doc(db, 'rooms', roomId);
    return onSnapshot(docRef, (docSnap) => {
      if (docSnap.exists()) {
        callback(docSnap.data());
      } else {
        // Init empty room if not exists
        const initial = getLocalSession(roomId);
        setDoc(docRef, initial);
        callback(initial);
      }
    }, (error) => {
      console.error("Firestore session subscription error:", error);
    });
  } else {
    listeners.session = callback;
    const channel = getLocalChannel(roomId);
    
    // Send current local state immediately
    const session = getLocalSession(roomId);
    callback(session);
    
    return () => {
      listeners.session = null;
    };
  }
}

/**
 * Subscribes to conversation history
 */
export function subscribeHistory(roomId, callback) {
  if (typeof window === 'undefined') return () => {};

  if (isFirebaseConfigured) {
    const historyRef = collection(db, 'rooms', roomId, 'history');
    const q = query(historyRef, orderBy('timestamp', 'asc'));
    return onSnapshot(q, (snapshot) => {
      const history = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
      callback(history);
    }, (error) => {
      console.error("Firestore history subscription error:", error);
    });
  } else {
    listeners.history = callback;
    getLocalChannel(roomId);
    
    // Return current local state immediately
    const history = getLocalHistory(roomId);
    callback(history);
    
    return () => {
      listeners.history = null;
    };
  }
}

/**
 * Updates room session properties (shallow merge)
 */
export async function updateSession(roomId, updates) {
  if (isFirebaseConfigured) {
    const docRef = doc(db, 'rooms', roomId);
    try {
      await updateDoc(docRef, updates);
    } catch (e) {
      // If doc doesn't exist, create it
      await setDoc(docRef, updates, { merge: true });
    }
  } else {
    const current = getLocalSession(roomId);
    const updated = { ...current, ...updates };
    saveLocalSession(roomId, updated);
    
    // Notify other local tabs
    const channel = getLocalChannel(roomId);
    if (channel) {
      channel.postMessage({ type: 'SESSION_UPDATE', data: updated });
    }
    
    // Trigger listener in current tab
    if (listeners.session) {
      listeners.session(updated);
    }
  }
}

/**
 * Appends a finalized message into history
 */
export async function addHistoryEntry(roomId, entry) {
  const newEntry = {
    ...entry,
    timestamp: Date.now()
  };

  if (isFirebaseConfigured) {
    const historyRef = collection(db, 'rooms', roomId, 'history');
    await addDoc(historyRef, newEntry);
  } else {
    const history = getLocalHistory(roomId);
    history.push(newEntry);
    saveLocalHistory(roomId, history);
    
    // Notify other tabs
    const channel = getLocalChannel(roomId);
    if (channel) {
      channel.postMessage({ type: 'HISTORY_ADD', data: newEntry });
    }
    
    // Trigger listener in current tab
    if (listeners.history) {
      listeners.history(history);
    }
  }
}

/**
 * Clears the dialogue history
 */
export async function clearHistory(roomId) {
  if (isFirebaseConfigured) {
    const historyRef = collection(db, 'rooms', roomId, 'history');
    const snapshot = await getDocs(historyRef);
    const deletePromises = snapshot.docs.map(doc => deleteDoc(doc.ref));
    await Promise.all(deletePromises);
  } else {
    saveLocalHistory(roomId, []);
    
    // Notify other tabs
    const channel = getLocalChannel(roomId);
    if (channel) {
      channel.postMessage({ type: 'HISTORY_CLEAR' });
    }
    
    if (listeners.history) {
      listeners.history([]);
    }
  }
}

/**
 * Retrieves all active/existing room sessions from Firestore or LocalStorage
 */
export async function getAllRooms() {
  if (isFirebaseConfigured) {
    try {
      const querySnapshot = await getDocs(collection(db, 'rooms'));
      return querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    } catch (e) {
      console.error("Error getting all rooms:", e);
      return [];
    }
  } else {
    if (typeof window === 'undefined') return [];
    const rooms = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key.startsWith('session_')) {
        const roomId = key.substring(8);
        try {
          const data = JSON.parse(localStorage.getItem(key));
          rooms.push({ id: roomId, ...data });
        } catch (e) {
          console.warn("Error parsing local session for room", roomId, e);
        }
      }
    }
    return rooms;
  }
}

/**
 * Completely deletes a room session and its history logs from database/local storage
 */
export async function deleteRoom(roomId) {
  if (isFirebaseConfigured) {
    try {
      const docRef = doc(db, 'rooms', roomId);
      await deleteDoc(docRef);

      // Clean history subcollection
      const historyRef = collection(db, 'rooms', roomId, 'history');
      const snapshot = await getDocs(historyRef);
      const deletePromises = snapshot.docs.map(doc => deleteDoc(doc.ref));
      await Promise.all(deletePromises);
    } catch (e) {
      console.error("Error deleting room document:", e);
    }
  } else {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(`session_${roomId}`);
    localStorage.removeItem(`history_${roomId}`);
  }
}

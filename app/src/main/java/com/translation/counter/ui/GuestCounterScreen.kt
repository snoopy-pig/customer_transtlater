package com.translation.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.translation.counter.data.CounterRoom
import com.translation.counter.data.SpeakerType
import com.translation.counter.data.TargetLanguage
import com.translation.counter.ui.theme.*

@Composable
fun GuestCounterScreen(
    room: CounterRoom,
    viewModel: MainViewModel
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isMicEnabled by viewModel.isMicEnabled.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val currentKoreanSub by viewModel.currentKoreanSubtitle.collectAsState()
    val currentGuestSub by viewModel.currentGuestSubtitle.collectAsState()
    val selectedLanguage by viewModel.guestTargetLanguage.collectAsState()

    val isSessionActive = currentSession?.isActive == true
    val listState = rememberLazyListState()
    var showLangSelectorDialog by remember { mutableStateOf(false) }

    val reversedMessages = remember(chatMessages) { chatMessages.reversed() }

    // Language Change Modal Dialog
    if (showLangSelectorDialog) {
        Dialog(onDismissRequest = { showLangSelectorDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌐 언어 선택 / Change Language",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TargetLanguage.values().forEach { lang ->
                            val isSelected = lang == selectedLanguage
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        viewModel.changeGuestLanguage(lang)
                                        showLangSelectorDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CuteTeal.copy(alpha = 0.3f) else SoftDark
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = lang.flagEmoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "${lang.nativeName} (${lang.displayName})",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftDark)
            .padding(12.dp)
    ) {
        // Cute Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            color = SoftCardBg,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.resetToSetup() },
                        modifier = Modifier.size(36.dp).background(SoftDark, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(color = CuteTeal.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "Room ${room.roomId}",
                            color = CuteTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dynamic Language Switch Button
                    Button(
                        onClick = { showLangSelectorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CuteTeal),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${selectedLanguage.flagEmoji} ${selectedLanguage.displayName}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Mic Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleMicState() },
                        modifier = Modifier.size(36.dp).background(if (isMicEnabled) ActiveGreen.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mic Toggle",
                            tint = if (isMicEnabled) ActiveGreen else Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (!isSessionActive) {
            // Initial View: 4 Large Cute Rounded Flag Capsule Buttons
            Card(
                modifier = Modifier.fillMaxSize().weight(1f).clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🌏 언어를 터치하시면 통역이 시작됩니다",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Touch your language to start instant AI translation",
                        color = TextSubtle,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TargetLanguage.values().forEach { lang ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { viewModel.selectGuestLanguageAndStartSession(lang) }
                                    .border(2.dp, CuteTeal.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                                colors = CardDefaults.cardColors(containerColor = SoftDark)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = lang.flagEmoji, fontSize = 36.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(text = lang.nativeName, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                                            Text(text = lang.displayName, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                        }
                                    }
                                    Surface(color = CuteTeal, shape = CircleShape) {
                                        Text(text = "START", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Chat View: Cute Reverse Scroll View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💬 1:1 실시간 대화 프롬프트", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${selectedLanguage.flagEmoji} ${selectedLanguage.displayName}", color = CuteTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SoftCardBorder)

                    if (reversedMessages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text(text = "말씀하시면 통역 자막이 상단에 즉시 표시됩니다.", color = TextSubtle, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(reversedMessages) { msg ->
                                val isGuest = msg.speaker == SpeakerType.GUEST.name
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isGuest) CuteTeal.copy(alpha = 0.18f) else CuteCoral.copy(alpha = 0.18f)
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = if (isGuest) "👨‍👩‍👧‍👦 You (관광객)" else "👨‍💼 Staff (직원)",
                                                color = if (isGuest) CuteTeal else CuteCoral,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(text = msg.formattedTime, color = Color.Gray, fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "🌐 ${msg.guestText}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "🇰🇷 ${msg.koreanText}", color = TextKoreanYellow.copy(alpha = 0.85f), fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Subtitle View
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(text = "[${selectedLanguage.displayName} AI 번역 자막]", color = CuteTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (currentGuestSub.isNotBlank()) currentGuestSub else "Waiting for speech...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(text = "[직원 한국어 원문]", color = TextKoreanYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = if (currentKoreanSub.isNotBlank()) currentKoreanSub else "직원의 대화 내용이 표시됩니다.", color = TextKoreanYellow.copy(alpha = 0.85f), fontSize = 15.sp)
                }
            }
        }
    }
}

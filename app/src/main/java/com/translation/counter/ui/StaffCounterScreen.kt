package com.translation.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.translation.counter.data.CounterRoom
import com.translation.counter.data.SpeakerType
import com.translation.counter.ui.components.MicPulseAnimation
import com.translation.counter.ui.theme.*

@Composable
fun StaffCounterScreen(
    room: CounterRoom,
    viewModel: MainViewModel
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentKoreanSub by viewModel.currentKoreanSubtitle.collectAsState()
    val currentGuestSub by viewModel.currentGuestSubtitle.collectAsState()

    val isSessionActive = currentSession?.isActive == true
    var manualInputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Reverse messages so NEWEST messages stay on TOP (최신 메시지 상단 노출)
    val reversedMessages = remember(chatMessages) { chatMessages.reversed() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Staff Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ROOM ${room.roomId}",
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "직원용 카운터 (Staff)",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                MicPulseAnimation(
                    isListening = isListening,
                    isSpeaking = isSpeaking
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.endSessionByStaff() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isSessionActive,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Session End",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "대화 종료",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // TOP 50% AREA: Real-time Chat History Feed (최신 메시지 상단 노출)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 실시간 대화 내역 (최신순 상단)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${chatMessages.size}개 메시지",
                        color = TextSubtle,
                        fontSize = 11.sp
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = CardBorder
                )

                if (reversedMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "대화 내역이 여기에 최신순(상단)으로 쌓입니다.",
                            color = TextSubtle,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reversedMessages) { msg ->
                            val isStaffMsg = msg.speaker == SpeakerType.STAFF.name
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isStaffMsg) ActiveGreen.copy(alpha = 0.15f) else PrimaryCyan.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isStaffMsg) "👨‍💼 직원 (Staff)" else "👨‍👩‍👧‍👦 손님 (Guest)",
                                            color = if (isStaffMsg) ActiveGreen else PrimaryCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = msg.formattedTime,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🇰🇷 ${msg.koreanText}",
                                        color = TextKoreanYellow,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "🌐 ${msg.guestText}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM 50% AREA: Latest Subtitles & Control Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Subtitle Display Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "[한국어 번역 최신 자막]",
                        color = TextKoreanYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentKoreanSub.isNotBlank()) currentKoreanSub else "대화 대기 중...",
                        color = TextKoreanYellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "[손님 언어 원문]",
                        color = PrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentGuestSub.isNotBlank()) currentGuestSub else "Guest original speech...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }

                // Text Input & Send to Guest
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        placeholder = { Text("손님에게 보낼 메시지 직접 입력...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (manualInputText.isNotBlank()) {
                                viewModel.sendManualSpeech(manualInputText)
                                manualInputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryCyan, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.translation.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.translation.counter.data.TargetLanguage
import com.translation.counter.ui.components.FlagButton
import com.translation.counter.ui.components.MicPulseAnimation
import com.translation.counter.ui.theme.*

@Composable
fun GuestCounterScreen(
    room: CounterRoom,
    viewModel: MainViewModel
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val currentKoreanSub by viewModel.currentKoreanSubtitle.collectAsState()
    val currentGuestSub by viewModel.currentGuestSubtitle.collectAsState()
    val selectedLanguage by viewModel.guestTargetLanguage.collectAsState()

    val isSessionActive = currentSession?.isActive == true
    val listState = rememberLazyListState()

    // Newest messages stay on TOP (최신 대화 최상단 노출)
    val reversedMessages = remember(chatMessages) { chatMessages.reversed() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Compact Header Bar
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
                    text = "손님용 (Guest)",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            MicPulseAnimation(
                isListening = isListening,
                isSpeaking = false
            )
        }

        if (!isSessionActive) {
            // Flag Selection Grid
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "언어를 선택하시면 대화가 시작됩니다",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Select language to start real-time prompt chat",
                        color = TextSubtle,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlagButton(
                                targetLanguage = TargetLanguage.ENGLISH,
                                isSelected = selectedLanguage == TargetLanguage.ENGLISH,
                                onClick = { viewModel.selectGuestLanguageAndStartSession(TargetLanguage.ENGLISH) },
                                modifier = Modifier.weight(1f)
                            )
                            FlagButton(
                                targetLanguage = TargetLanguage.SIMPLIFIED_CHINESE,
                                isSelected = selectedLanguage == TargetLanguage.SIMPLIFIED_CHINESE,
                                onClick = { viewModel.selectGuestLanguageAndStartSession(TargetLanguage.SIMPLIFIED_CHINESE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlagButton(
                                targetLanguage = TargetLanguage.TRADITIONAL_CHINESE,
                                isSelected = selectedLanguage == TargetLanguage.TRADITIONAL_CHINESE,
                                onClick = { viewModel.selectGuestLanguageAndStartSession(TargetLanguage.TRADITIONAL_CHINESE) },
                                modifier = Modifier.weight(1f)
                            )
                            FlagButton(
                                targetLanguage = TargetLanguage.JAPANESE,
                                isSelected = selectedLanguage == TargetLanguage.JAPANESE,
                                onClick = { viewModel.selectGuestLanguageAndStartSession(TargetLanguage.JAPANESE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            // TOP 50%: Live Log (Newest on TOP)
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
                            text = "💬 실시간 대화 프롬프트 (최신순 상단)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = selectedLanguage.displayName,
                            color = PrimaryCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
                                text = "음성으로 말하거나 메시지를 적으시면 상단에 즉시 표시됩니다.",
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
                                val isGuestMsg = msg.speaker == SpeakerType.GUEST.name
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isGuestMsg) PrimaryCyan.copy(alpha = 0.15f) else ActiveGreen.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isGuestMsg) "👨‍👩‍👧‍👦 You (손님)" else "👨‍💼 Staff (직원)",
                                                color = if (isGuestMsg) PrimaryCyan else ActiveGreen,
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
                                            text = "🌐 ${msg.guestText}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "🇰🇷 ${msg.koreanText}",
                                            color = TextKoreanYellow.copy(alpha = 0.8f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM 50%: Latest Subtitles View
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "[${selectedLanguage.displayName} - AI 번역 자막]",
                        color = PrimaryCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentGuestSub.isNotBlank()) currentGuestSub else "Waiting for speech...",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Text(
                        text = "[직원 한국어 원문]",
                        color = TextKoreanYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentKoreanSub.isNotBlank()) currentKoreanSub else "직원의 대화 내용이 표시됩니다.",
                        color = TextKoreanYellow.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

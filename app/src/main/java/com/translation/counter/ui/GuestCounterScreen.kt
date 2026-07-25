package com.translation.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
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
import com.translation.counter.ui.components.FlagButton
import com.translation.counter.ui.theme.*

@Composable
fun GuestCounterScreen(
    room: CounterRoom,
    viewModel: MainViewModel
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isMicEnabled by viewModel.isMicEnabled.collectAsState()
    val currentKoreanSub by viewModel.currentKoreanSubtitle.collectAsState()
    val currentGuestSub by viewModel.currentGuestSubtitle.collectAsState()
    val selectedLanguage by viewModel.guestTargetLanguage.collectAsState()

    val isSessionActive = currentSession?.isActive == true
    val listState = rememberLazyListState()
    var showLangSelectorDialog by remember { mutableStateOf(false) }

    val reversedMessages = remember(chatMessages) { chatMessages.reversed() }

    // Language Change Dialog Modal
    if (showLangSelectorDialog) {
        Dialog(onDismissRequest = { showLangSelectorDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌐 언어 변경 (Change Language)",
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
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.changeGuestLanguage(lang)
                                        showLangSelectorDialog = false
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryCyan else CardBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryCyan.copy(alpha = 0.25f) else DarkBg
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = lang.flagEmoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = lang.nativeName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = lang.displayName,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { showLangSelectorDialog = false }) {
                        Text("닫기 (Close)", color = TextSubtle)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Guest Header Bar with Back & Language Switch Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Back Button
                IconButton(
                    onClick = { viewModel.resetToSetup() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(CardBg, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to setup",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "손님용 (Guest)",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dynamic Language Change Button (상시 언어 변경 버튼)
                Button(
                    onClick = { showLangSelectorDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Change Language",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${selectedLanguage.flagEmoji} ${selectedLanguage.displayName}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Mic Toggle Button
                IconButton(
                    onClick = { viewModel.toggleMicState() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isMicEnabled) ActiveGreen.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
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
                            text = "${selectedLanguage.flagEmoji} ${selectedLanguage.displayName}",
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

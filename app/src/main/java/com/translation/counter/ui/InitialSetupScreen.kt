package com.translation.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.translation.counter.data.CounterRoom
import com.translation.counter.data.DeviceRole
import com.translation.counter.ui.theme.CardBg
import com.translation.counter.ui.theme.CardBorder
import com.translation.counter.ui.theme.DarkBg
import com.translation.counter.ui.theme.PrimaryCyan

@Composable
fun InitialSetupScreen(
    onSetupComplete: (CounterRoom, DeviceRole) -> Unit
) {
    var selectedRoom by remember { mutableStateOf(CounterRoom.ROOM_1) }
    var selectedRole by remember { mutableStateOf(DeviceRole.STAFF) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Title Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌐 1:1 창구 통역 시스템",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "스마트폰 기기 설정 (Room & Role)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Step 1: Counter Room Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STEP 1",
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(PrimaryCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "창구 번호 선택 (Counter Room)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CounterRoom.values().forEach { room ->
                            val isSelected = room == selectedRoom
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedRoom = room }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryCyan else CardBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryCyan.copy(alpha = 0.25f) else DarkBg
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Room",
                                        fontSize = 11.sp,
                                        color = if (isSelected) PrimaryCyan else Color.Gray
                                    )
                                    Text(
                                        text = "${room.roomId}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: Device Role Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STEP 2",
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(PrimaryCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "디바이스 역할 선택 (Role)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeviceRole.values().forEach { role ->
                            val isSelected = role == selectedRole
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedRole = role }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryCyan else CardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else DarkBg
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (role == DeviceRole.STAFF) Icons.Default.SupportAgent else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) PrimaryCyan else Color.Gray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (role == DeviceRole.STAFF) "직원용 카운터 (Staff)" else "손님용 카운터 (Guest)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (role == DeviceRole.STAFF) "한국어 고정 / 세션 종료 제어" else "4개 국기 언어 선택 카운터",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = PrimaryCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Start Button
            Button(
                onClick = { onSetupComplete(selectedRoom, selectedRole) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
            ) {
                Text(
                    text = "Room ${selectedRoom.roomId} [${if (selectedRole == DeviceRole.STAFF) "직원용" else "손님용"}] 입장하기",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

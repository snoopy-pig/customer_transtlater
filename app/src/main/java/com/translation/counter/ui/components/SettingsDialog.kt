package com.translation.counter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.translation.counter.data.AppPreferences
import com.translation.counter.ui.theme.CardBg
import com.translation.counter.ui.theme.CardBorder
import com.translation.counter.ui.theme.DarkBg
import com.translation.counter.ui.theme.PrimaryCyan

@Composable
fun SettingsDialog(
    currentApiKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Key",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI 모델 API 설정",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "개별 본인의 Google Gemini API Key를 입력하세요.\n(Google AI Studio에서 무료로 발급 가능합니다)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key", color = PrimaryCyan) },
                    placeholder = { Text("API Key를 붙여넣으세요", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset Default Button
                    OutlinedButton(
                        onClick = {
                            apiKeyInput = AppPreferences.DEFAULT_KEY
                        },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Default",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "기본값", fontSize = 12.sp)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            onSaveKey(apiKeyInput)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Text(
                            text = "저장하기",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

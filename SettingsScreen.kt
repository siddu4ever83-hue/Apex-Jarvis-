package com.elite.jarvishud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    keyStore: KeyStore,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = HudCyan)
                }
                Text(
                    text = "SETTINGS",
                    color = HudCyan,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== OpenRouter (AI brain) card, with model list =====
            ProviderCard(title = "OpenRouter (AI replies)") {
                var key by remember { mutableStateOf(keyStore.openRouterKey) }
                var selectedModel by remember { mutableStateOf(keyStore.openRouterModel) }
                var saved by remember { mutableStateOf(false) }

                HudLabel("API Key — get free at openrouter.ai/keys")
                HudTextField(value = key, onValueChange = { key = it; saved = false })

                Spacer(modifier = Modifier.height(14.dp))
                HudLabel("Choose Model")
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    ModelCatalog.openRouterModels.forEach { model ->
                        ModelRow(
                            model = model,
                            selected = model == selectedModel,
                            onSelect = { selectedModel = model; saved = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                SaveButton(saved = saved) {
                    keyStore.openRouterKey = key.trim()
                    keyStore.openRouterModel = selectedModel
                    saved = true
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== ElevenLabs (voice) card =====
            ProviderCard(title = "ElevenLabs (voice — falls back to phone TTS if it fails/limit reached)") {
                var key by remember { mutableStateOf(keyStore.elevenLabsKey) }
                var voiceId by remember { mutableStateOf(keyStore.elevenLabsVoiceId) }
                var saved by remember { mutableStateOf(false) }

                HudLabel("API Key — get at elevenlabs.io (leave blank to always use phone TTS)")
                HudTextField(value = key, onValueChange = { key = it; saved = false })

                Spacer(modifier = Modifier.height(14.dp))
                HudLabel("Voice ID (default = Rachel)")
                HudTextField(value = voiceId, onValueChange = { voiceId = it; saved = false })

                Spacer(modifier = Modifier.height(14.dp))
                SaveButton(saved = saved) {
                    keyStore.elevenLabsKey = key.trim()
                    keyStore.elevenLabsVoiceId = voiceId.trim().ifBlank { "21m00Tcm4TlvDq8ikWAM" }
                    saved = true
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== NewsAPI card =====
            ProviderCard(title = "NewsAPI (morning briefing)") {
                var key by remember { mutableStateOf(keyStore.newsApiKey) }
                var country by remember { mutableStateOf(keyStore.newsCountry) }
                var saved by remember { mutableStateOf(false) }

                HudLabel("API Key — get free at newsapi.org/register")
                HudTextField(value = key, onValueChange = { key = it; saved = false })

                Spacer(modifier = Modifier.height(14.dp))
                HudLabel("Country code (e.g. in, us, gb)")
                HudTextField(value = country, onValueChange = { country = it; saved = false })

                Spacer(modifier = Modifier.height(14.dp))
                SaveButton(saved = saved) {
                    keyStore.newsApiKey = key.trim()
                    keyStore.newsCountry = country.trim().ifBlank { "in" }
                    saved = true
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ProviderCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HudPanel)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = HudCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = HudCyanDim
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ModelRow(model: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = HudCyan, unselectedColor = HudCyanDim)
        )
        Text(
            text = model,
            color = if (selected) HudCyan else Color(0xFFAACCDD),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HudLabel(text: String) {
    Text(text = text, color = Color(0xFF667788), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun HudTextField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HudCyan,
            unfocusedBorderColor = HudCyanDim,
            cursorColor = HudCyan
        )
    )
}

@Composable
private fun SaveButton(saved: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = HudCyan, contentColor = Color.Black),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (saved) "SAVED ✓" else "SAVE", fontFamily = FontFamily.Monospace)
    }
}

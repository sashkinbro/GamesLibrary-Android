package com.sbro.gameslibrary.cyberpunk.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.sbro.gameslibrary.util.CYBERPUNK_MODE
import com.sbro.gameslibrary.util.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)

@Composable
fun CyberpunkThemeToggle() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isCyberpunkEnabled by context.dataStore.data
        .map { prefs -> prefs[CYBERPUNK_MODE] ?: false }
        .collectAsState(initial = false)

    if (isCyberpunkEnabled) {
        CyberStyleToggle(
            checked = true,
            onCheckedChange = {
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[CYBERPUNK_MODE] = false
                    }
                }
            }
        )
    } else {
        MaterialStyleToggle(
            checked = false,
            onCheckedChange = {
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[CYBERPUNK_MODE] = true
                    }
                }
            }
        )
    }
}

@Composable
private fun MaterialStyleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun CyberStyleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Toggle track
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(28.dp)
            .clip(CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
            .background(CyberDark)
            .border(1.dp, CyberBlue, CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        // Toggle thumb
        val thumbOffset by animateDpAsState(
            targetValue = if (checked) 24.dp else 0.dp,
            animationSpec = tween(300), 
            label = "thumb_offset"
        )
        
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(28.dp)
                .padding(4.dp)
                .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
                .background(CyberYellow)
                .border(1.dp, CyberRed, CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
        )
        
        // "ON" text for visual flair if needed, or simple graphics
        if (checked) {
             Text(
                text = "ON",
                color = CyberBlue.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            )
        }
    }
}

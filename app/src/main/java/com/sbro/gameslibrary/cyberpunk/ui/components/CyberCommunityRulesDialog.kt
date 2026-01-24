package com.sbro.gameslibrary.cyberpunk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sbro.gameslibrary.R
import androidx.compose.ui.res.stringResource

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)

@Composable
fun CyberCommunityRulesDialog(
    onAccept: () -> Unit
) {
    var agreed by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val scrollState = rememberScrollState()

        Surface(
            shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
            color = CyberDark,
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
            ),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBlack)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.rules_title).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = CyberYellow
                    )
                    Text(
                        text = stringResource(R.string.rules_intro),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                val rules = listOf(
                    Icons.Filled.Warning to stringResource(R.string.rules_point_no_fake_tests),
                    Icons.Filled.Warning to stringResource(R.string.rules_point_no_junk),
                    Icons.Filled.Gavel to stringResource(R.string.rules_point_no_fun_uploads),
                    Icons.Filled.Gavel to stringResource(R.string.rules_point_no_abuse),
                    Icons.Filled.Shield to stringResource(R.string.rules_point_moderation),
                    Icons.Filled.Shield to stringResource(R.string.rules_point_respect)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rules.forEach { (icon, text) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                                color = CyberBlack,
                                border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = CyberYellow
                                    )
                                }
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { agreed = !agreed }
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = { agreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberYellow,
                            uncheckedColor = CyberBlue,
                            checkmarkColor = CyberBlack
                        )
                    )
                    Text(
                        text = stringResource(R.string.rules_checkbox_agree),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onAccept,
                    enabled = agreed,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberYellow,
                        contentColor = CyberBlack,
                        disabledContainerColor = CyberYellow.copy(alpha = 0.4f),
                        disabledContentColor = CyberBlack.copy(alpha = 0.5f)
                    ),
                    shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.rules_accept_button).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }
    }
}

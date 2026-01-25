package com.sbro.gameslibrary.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sbro.gameslibrary.R
import kotlinx.coroutines.launch

@Composable
fun CommunityRulesDialog(
    onAccept: () -> Unit
) {
    var agreed by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val scrollState = rememberScrollState()
        val context = androidx.compose.ui.platform.LocalContext.current
        val isAtBottom by remember { derivedStateOf { !scrollState.canScrollForward } }
        var showScrollButton by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(48.dp)
                            .fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.rules_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = stringResource(R.string.rules_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                val rules = listOf(
                    Icons.Filled.Warning to stringResource(R.string.rules_point_no_fake_tests),
                    Icons.Filled.Warning to stringResource(R.string.rules_point_no_junk),
                    Icons.Filled.Gavel to stringResource(R.string.rules_point_no_fun_uploads),
                    Icons.Filled.Gavel to stringResource(R.string.rules_point_no_abuse),
                    Icons.Filled.Shield to stringResource(R.string.rules_point_moderation),
                    Icons.Filled.Shield to stringResource(R.string.rules_point_respect)
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    rules.forEach { (icon, text) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollButton && !isAtBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (isAtBottom) {
                            agreed = !agreed
                        } else {
                            showScrollButton = true
                            android.widget.Toast.makeText(context, R.string.rules_toast_read_first, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = {
                            if (isAtBottom) {
                                agreed = it
                            } else {
                                showScrollButton = true
                                android.widget.Toast.makeText(context, R.string.rules_toast_read_first, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = true
                    )
                    Text(
                        text = stringResource(R.string.rules_checkbox_agree),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAccept,
                    enabled = agreed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.rules_accept_button))
                }
                }
            }
        }
    }
}

package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import com.sbro.gameslibrary.R

private data class OnboardPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int
)

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn( ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val pages = remember {
        listOf(
            OnboardPage(Icons.Outlined.LibraryBooks, R.string.onboard_title_1, R.string.onboard_body_1),
            OnboardPage(Icons.Outlined.Tune, R.string.onboard_title_2, R.string.onboard_body_2),
            OnboardPage(Icons.Outlined.Bolt, R.string.onboard_title_3, R.string.onboard_body_3),
            OnboardPage(Icons.Outlined.SportsEsports, R.string.onboard_title_4, R.string.onboard_body_4)
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val background = if (isDark) Color(0xFF0B0C10) else Color(0xFFF7F8FB)
    val surface = if (isDark) Color(0xFF141826) else Color.White
    val surfaceVariant = if (isDark) Color(0xFF2A2E3A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDark) Color.White else Color(0xFF111827)
    val textSecondary = if (isDark) Color(0xFFB0BEC5) else Color(0xFF4B5563)
    val accent = if (isDark) Color(0xFF64B5F6) else Color(0xFF1E88E5)
    val tertiary = if (isDark) Color(0xFF7E57C2) else Color(0xFF6D28D9)

    val bgBrush = Brush.verticalGradient(
        listOf(
            background,
            if (isDark) Color(0xFF10131A) else Color(0xFFEFF1F6),
            background
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .systemBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        text = stringResource(R.string.onboard_skip),
                        color = textSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                beyondViewportPageCount = 1
            ) { page ->
                val item = pages[page]

                val appear = remember { Animatable(0f) }
                LaunchedEffect(page) {
                    appear.snapTo(0f)
                    appear.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                }

                val iconPulse by rememberInfiniteTransition(label = "iconPulse").animateFloat(
                    initialValue = 0.98f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        tween(1400, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp)
                        .alpha(appear.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        accent.copy(alpha = if (isDark) 0.18f else 0.12f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .size(152.dp)
                                .scale(iconPulse)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(accent, tertiary, accent))
                                )
                        )

                        AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                (slideInHorizontally { it / 2 } + fadeIn(tween(260)))
                                    .with(slideOutHorizontally { -it / 2 } + fadeOut(tween(260)))
                            },
                            label = "pageText"
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(item.titleRes),
                                    color = textPrimary,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 30.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(item.bodyRes),
                                    color = textSecondary,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Dots
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { i ->
                    val selected = pagerState.currentPage == i
                    val w by animateDpAsState(
                        targetValue = if (selected) 26.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "dotW"
                    )
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(if (selected) accent else surfaceVariant)
                    )
                    if (i != pages.lastIndex) Spacer(Modifier.width(8.dp))
                }
            }

            // Bottom bar
            val isLast = pagerState.currentPage == pages.lastIndex
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(if (isLast) R.string.onboard_start else R.string.onboard_next),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

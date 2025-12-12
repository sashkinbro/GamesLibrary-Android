package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import kotlinx.coroutines.launch

private data class OnboardPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int
)

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    isSystemInDarkTheme()

    val pages = remember {
        listOf(
            OnboardPage(Icons.AutoMirrored.Outlined.LibraryBooks, R.string.onboard_title_1, R.string.onboard_body_1),
            OnboardPage(Icons.Outlined.Tune, R.string.onboard_title_2, R.string.onboard_body_2),
            OnboardPage(Icons.Outlined.Bolt, R.string.onboard_title_3, R.string.onboard_body_3),
            OnboardPage(Icons.Outlined.SportsEsports, R.string.onboard_title_4, R.string.onboard_body_4)
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val bgBrush = Brush.verticalGradient(
        listOf(CyberBlack, CyberDark, CyberBlack)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                            .background(CyberDark)
                    ) {
                        Text(
                            text = stringResource(R.string.onboard_skip).uppercase(),
                            color = CyberBlue.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                        appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                    }

                    val iconPulse by rememberInfiniteTransition(label = "iconPulse")
                        .animateFloat(
                            initialValue = 0.97f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                tween(1400, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .alpha(appear.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        val ringShape = CutCornerShape(26.dp)
                        Surface(
                            shape = ringShape,
                            color = CyberDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Brush.radialGradient(
                                    listOf(
                                        CyberBlue.copy(alpha = 0.9f),
                                        CyberYellow.copy(alpha = 0.6f),
                                        CyberRed.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                            tonalElevation = 0.dp,
                            modifier = Modifier.size(240.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                CyberBlue.copy(alpha = 0.14f),
                                                Color.Transparent
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = CyberYellow,
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(iconPulse)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        val panelShape = CutCornerShape(
                            topStart = 0.dp,
                            topEnd = 18.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 18.dp
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = panelShape,
                            color = CyberDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        CyberRed.copy(alpha = 0.7f),
                                        CyberYellow.copy(alpha = 0.7f),
                                        CyberBlue.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {

                            Column {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(CyberYellow, CyberBlue, CyberYellow)
                                            )
                                        )
                                )

                                AnimatedContent(
                                    targetState = page,
                                    transitionSpec = {
                                        (slideInHorizontally { it / 2 } + fadeIn(tween(240))).togetherWith(
                                            slideOutHorizontally { -it / 2 } + fadeOut(tween(240)))
                                    },
                                    label = "pageText"
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GlitchText(
                                                text = stringResource(item.titleRes).uppercase(),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 2.sp,
                                                    lineHeight = 30.sp,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = stringResource(item.bodyRes),
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { i ->
                        val selected = pagerState.currentPage == i
                        val w by animateDpAsState(
                            targetValue = if (selected) 28.dp else 8.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "dotW"
                        )
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(w)
                                .clip(CircleShape)
                                .background(
                                    if (selected)
                                        Brush.horizontalGradient(listOf(CyberYellow, CyberBlue))
                                    else
                                        Brush.linearGradient(listOf(CyberGray, CyberGray))
                                )
                        )
                        if (i != pages.lastIndex) Spacer(Modifier.width(8.dp))
                    }
                }

                val isLast = pagerState.currentPage == pages.lastIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val btnShape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)

                    Surface(
                        shape = btnShape,
                        color = CyberBlack,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
                        ),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .clip(btnShape)
                            .clickable {
                                if (isLast) onFinish()
                                else scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            CyberDark,
                                            CyberGray,
                                            CyberDark
                                        )
                                    )
                                )
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(
                                    if (isLast) R.string.onboard_start else R.string.onboard_next
                                ).uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp,
                                color = CyberYellow
                            )
                        }
                    }
                }
            }
        }
    }
}

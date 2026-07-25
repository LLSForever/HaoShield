package com.haoshield.ui.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoTextLink
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.launch

@Composable
fun IntroScreen(
    onFinished: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel(),
) {
    val pages = IntroPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    fun finish() {
        viewModel.onComplete()
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Skip — quiet, top-right, gone on the last page where "Begin" takes over.
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = HaoTheme.spacing.screenH)) {
            if (!isLastPage) {
                HaoTextLink(
                    text = "Skip",
                    onClick = ::finish,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = HaoTheme.spacing.sm),
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkFaint,
                    alignEnd = true,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            IntroPageContent(page = pages[pageIndex])
        }

        PageDots(
            count = pages.size,
            selected = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.md),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HaoTheme.spacing.screenH,
                    vertical = HaoTheme.spacing.lg,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLastPage) {
                HaoPrimaryButton(
                    text = "Begin",
                    onClick = ::finish,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                ) {
                    Text(text = "Next", style = HaoTheme.type.body, color = HaoTheme.colors.ink)
                }
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    // Left-aligned and set at reading size: these pages carry real paragraphs, and centred display
    // type overflowed the shorter screens. Reads as a page of a book rather than a splash.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        if (page.showGlyph) {
            Text(text = "好", style = HaoTheme.type.glyphSmall, color = HaoTheme.colors.ink)
            Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
        }

        Text(
            text = page.title,
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )

        page.paragraphs.forEach { paragraph ->
            Text(
                text = paragraph,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
        }

        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
    }
}

@Composable
private fun PageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(HaoTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val active = index == selected
            val color by animateColorAsState(
                targetValue = if (active) HaoTheme.colors.ink else HaoTheme.colors.stone,
                animationSpec = tween(HaoMotion.STANDARD),
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind { drawCircle(color = color) },
            )
        }
    }
}

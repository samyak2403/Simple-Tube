package com.samyak.simpletube.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samyak.simpletube.constants.AppBarHeight
import com.samyak.simpletube.ui.utils.fadingEdge
import com.valentinilk.shimmer.shimmer

@Composable
fun ArtistPagePlaceholder() {
    ShimmerHost {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3)
        ) {
            Spacer(
                modifier = Modifier
                    .shimmer()
                    .background(MaterialTheme.colorScheme.onSurface)
                    .fadingEdge(
                        top = WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateTopPadding() + AppBarHeight,
                        bottom = 108.dp
                    )
            )
            TextPlaceholder(
                height = 56.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 48.dp)
            )
        }

        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            ButtonPlaceholder(Modifier.weight(1f))

            Spacer(Modifier.width(12.dp))

            ButtonPlaceholder(Modifier.weight(1f))
        }

        repeat(6) {
            ListItemPlaceHolder()
        }
    }
}
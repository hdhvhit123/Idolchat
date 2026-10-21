package com.fandom.auchatstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fandom.auchatstudio.core.ColorMath
import com.fandom.auchatstudio.core.NameRules
import java.io.File

/**
 * Circular avatar: the user's picture when set, otherwise the role colour with initials.
 * [model] may be a file path, a Uri (for an unsaved pick) or null.
 */
@Composable
fun Avatar(
    name: String,
    colorHex: Long,
    model: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    ringColor: Color? = null,
) {
    val base = Color(colorHex)
    val ringed = if (ringColor != null) modifier.border(2.dp, ringColor, CircleShape) else modifier
    Box(
        modifier = ringed
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.75f)))),
        contentAlignment = Alignment.Center,
    ) {
        val textColor = if (ColorMath.prefersDarkText(colorHex)) Color(0xFF1C1B22) else Color.White
        Text(
            text = NameRules.initialsOf(name),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
            maxLines = 1,
        )
        if (model != null) {
            val data = if (model is String) File(model) else model
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(data).crossfade(true).build(),
                contentDescription = "Ảnh đại diện của $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

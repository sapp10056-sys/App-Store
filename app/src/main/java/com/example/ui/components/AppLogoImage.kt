package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.util.ImageUrlResolver
import kotlin.math.abs

/**
 * Universal resilient App Logo Image component for NovaStore.
 * - Handles Google Drive, Imgur, GitHub, Dropbox, and Postimg direct resolution
 * - Adds browser User-Agent so image hosts don't return 403 Forbidden
 * - Provides immediate, beautiful branded gradient avatar with initial letter as fallback
 */
@Composable
fun AppLogoImage(
    logoUrl: String,
    appName: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 64.dp
) {
    val context = LocalContext.current
    val initialCleaned = remember(logoUrl) { ImageUrlResolver.cleanDirectImageUrl(logoUrl) }
    var resolvedUrl by remember(initialCleaned) { mutableStateOf(initialCleaned) }

    // If URL is an image host webpage (e.g. postimg.cc/xxx), resolve direct image in background
    LaunchedEffect(initialCleaned) {
        if (initialCleaned.isNotBlank() && (initialCleaned.contains("postimg.cc") || initialCleaned.contains("postimages.org"))) {
            val direct = ImageUrlResolver.resolveToDirectImageUrl(initialCleaned)
            if (direct.isNotBlank() && direct != resolvedUrl) {
                resolvedUrl = direct
            }
        }
    }

    Box(
        modifier = modifier
            .size(iconSize)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedUrl.isBlank()) {
            FallbackBrandedAvatar(appName = appName, shape = shape)
        } else {
            val imageRequest = remember(resolvedUrl) {
                ImageRequest.Builder(context)
                    .data(resolvedUrl)
                    .crossfade(true)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
            }

            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "$appName icon",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize / 3),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                error = {
                    FallbackBrandedAvatar(appName = appName, shape = shape)
                }
            )
        }
    }
}

@Composable
fun FallbackBrandedAvatar(
    appName: String,
    shape: Shape = RoundedCornerShape(14.dp),
    modifier: Modifier = Modifier
) {
    val initial = remember(appName) {
        val trimmed = appName.trim()
        if (trimmed.isNotEmpty()) trimmed.take(1).uppercase() else "A"
    }

    // Deterministic pleasant gradient colors based on appName hash
    val gradientColors = remember(appName) {
        val palettes = listOf(
            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)), // Royal Blue
            listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)), // Purple
            listOf(Color(0xFF0D9488), Color(0xFF0F766E)), // Teal
            listOf(Color(0xFFD97706), Color(0xFFB45309)), // Amber
            listOf(Color(0xFFE11D48), Color(0xFFBE123C)), // Rose
            listOf(Color(0xFF059669), Color(0xFF047857)), // Emerald
            listOf(Color(0xFF4F46E5), Color(0xFF3730A3)), // Indigo
            listOf(Color(0xFF0284C7), Color(0xFF0369A1))  // Sky
        )
        val index = abs(appName.hashCode()) % palettes.size
        palettes[index]
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

/**
 * Screenshot image component with direct link resolution and fallback.
 */
@Composable
fun AppScreenshotImage(
    url: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    val initialCleaned = remember(url) { ImageUrlResolver.cleanDirectImageUrl(url) }
    var resolvedUrl by remember(initialCleaned) { mutableStateOf(initialCleaned) }

    LaunchedEffect(initialCleaned) {
        if (initialCleaned.isNotBlank() && (initialCleaned.contains("postimg.cc") || initialCleaned.contains("postimages.org"))) {
            val direct = ImageUrlResolver.resolveToDirectImageUrl(initialCleaned)
            if (direct.isNotBlank() && direct != resolvedUrl) {
                resolvedUrl = direct
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val request = remember(resolvedUrl) {
            ImageRequest.Builder(context)
                .data(resolvedUrl)
                .crossfade(true)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = "Screenshot preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        )
    }
}


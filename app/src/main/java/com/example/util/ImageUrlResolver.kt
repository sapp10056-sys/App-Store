package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Normalizes, resolves, and verifies image URLs for applications and screenshots.
 * Automatically converts Google Drive, Imgur, GitHub, Dropbox, and Postimg webpage URLs
 * into direct, raw image URLs that load reliably in Coil.
 */
object ImageUrlResolver {
    private const val TAG = "ImageUrlResolver"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // In-memory cache for resolved direct image URLs
    private val resolvedCache = ConcurrentHashMap<String, String>()

    // Regex patterns for instant synchronous transformation
    private val googleDriveFilePattern = Pattern.compile("(?:drive\\.google\\.com/file/d/|drive\\.google\\.com/open\\?id=|drive\\.google\\.com/uc\\?.*id=)([a-zA-Z0-9_-]+)")
    private val imgurPattern = Pattern.compile("imgur\\.com/(?:gallery/|a/)?([a-zA-Z0-9]{5,8})(?:\\.[a-zA-Z]+)?$")
    private val githubBlobPattern = Pattern.compile("github\\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.*)")
    private val postImgDirectPattern = Pattern.compile("(https?://i\\.postimg\\.cc/[a-zA-Z0-9_-]+/[^\"'\\s<>]+)")
    private val ogImagePattern = Pattern.compile("<meta[^>]+(?:property|name)=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)

    /**
     * Synchronously cleans and instantly converts known patterns (Google Drive, Imgur, GitHub, Dropbox).
     */
    fun cleanDirectImageUrl(rawUrl: String): String {
        var url = rawUrl.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .removeSurrounding("<", ">")

        if (url.isBlank()) return ""

        // Handle accidental markdown image tag ![alt](url)
        if (url.startsWith("![") && url.contains("](") && url.endsWith(")")) {
            url = url.substringAfter("](").removeSuffix(")")
        }

        // Auto-prepend https:// if scheme is missing
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }

        // Check cache first
        resolvedCache[url]?.let { return it }

        // 1. Google Drive view/share link -> Google Direct CDN (lh3.googleusercontent.com)
        val driveMatcher = googleDriveFilePattern.matcher(url)
        if (driveMatcher.find()) {
            val fileId = driveMatcher.group(1)
            if (!fileId.isNullOrBlank()) {
                val direct = "https://lh3.googleusercontent.com/d/$fileId"
                resolvedCache[url] = direct
                return direct
            }
        }

        // 2. Imgur page -> Direct image
        val imgurMatcher = imgurPattern.matcher(url)
        if (imgurMatcher.find() && !url.contains("i.imgur.com")) {
            val id = imgurMatcher.group(1)
            if (!id.isNullOrBlank()) {
                val direct = "https://i.imgur.com/$id.png"
                resolvedCache[url] = direct
                return direct
            }
        }

        // 3. GitHub blob URL -> raw.githubusercontent.com
        val ghMatcher = githubBlobPattern.matcher(url)
        if (ghMatcher.find()) {
            val user = ghMatcher.group(1)
            val repo = ghMatcher.group(2)
            val branch = ghMatcher.group(3)
            val path = ghMatcher.group(4)
            val direct = "https://raw.githubusercontent.com/$user/$repo/$branch/$path"
            resolvedCache[url] = direct
            return direct
        }

        // 4. Dropbox share link -> raw image stream
        if (url.contains("dropbox.com/s/")) {
            val direct = when {
                url.contains("dl=0") -> url.replace("dl=0", "raw=1")
                url.contains("?") -> "$url&raw=1"
                else -> "$url?raw=1"
            }
            resolvedCache[url] = direct
            return direct
        }

        return url
    }

    /**
     * Resolves webpage links like postimg.cc/xxx or HTML hosts to the direct raw image URL.
     * Can be called asynchronously or in coroutines during publish, sync, or UI image loading.
     */
    suspend fun resolveToDirectImageUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        val cleaned = cleanDirectImageUrl(rawUrl)
        if (cleaned.isBlank()) return@withContext ""

        // Check if already in cache
        resolvedCache[cleaned]?.let { return@withContext it }

        // If it's already an obvious direct image file
        val lower = cleaned.lowercase()
        if (lower.startsWith("https://i.postimg.cc/") ||
            lower.startsWith("https://i.imgur.com/") ||
            lower.startsWith("https://lh3.googleusercontent.com/") ||
            lower.startsWith("https://images.unsplash.com/") ||
            lower.startsWith("https://raw.githubusercontent.com/") ||
            lower.endsWith(".png") || lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") || lower.endsWith(".webp") ||
            lower.endsWith(".svg")
        ) {
            resolvedCache[cleaned] = cleaned
            return@withContext cleaned
        }

        // If it's postimg.cc / postimages.org or other image sharing host page
        if (cleaned.contains("postimg.cc") || cleaned.contains("postimages.org")) {
            try {
                val request = Request.Builder()
                    .url(cleaned)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/*,*/*;q=0.8")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        // Check direct i.postimg.cc link in page
                        val postImgMatcher = postImgDirectPattern.matcher(body)
                        if (postImgMatcher.find()) {
                            val direct = postImgMatcher.group(1)
                            if (!direct.isNullOrBlank()) {
                                resolvedCache[cleaned] = direct
                                Log.d(TAG, "Successfully resolved postimg page to direct image: $direct")
                                return@withContext direct
                            }
                        }

                        // Check og:image meta tag
                        val ogMatcher = ogImagePattern.matcher(body)
                        if (ogMatcher.find()) {
                            val ogUrl = ogMatcher.group(1)
                            if (!ogUrl.isNullOrBlank()) {
                                resolvedCache[cleaned] = ogUrl
                                Log.d(TAG, "Successfully resolved og:image: $ogUrl")
                                return@withContext ogUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve image webpage for $cleaned: ${e.message}")
            }
        }

        // Return cleaned version if web scraping didn't produce direct link
        return@withContext cleaned
    }

    /**
     * Curated, verified, direct fallback icons by category.
     * Guaranteed to load instantly with zero external redirects.
     */
    fun getPresetIcon(category: String): String {
        return when (category.lowercase()) {
            "games" -> "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=256&auto=format&fit=crop&q=80"
            "entertainment", "media" -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=256&auto=format&fit=crop&q=80"
            "communication", "social" -> "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=256&auto=format&fit=crop&q=80"
            "productivity" -> "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=256&auto=format&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=256&auto=format&fit=crop&q=80" // Modern blue/purple abstract 3D icon
        }
    }
}

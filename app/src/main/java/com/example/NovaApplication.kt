package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.security.SecurityIntegrityChecker
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * NovaApplication
 * - Executes strict signature integrity and anti-modding checks at the earliest entry point
 * - Implements ImageLoaderFactory to configure Coil with standard WebP/PNG/JPEG decoding,
 *   preventing unsupported AVIF/HEIF stream errors (HeifDecoderImpl) on emulators and devices.
 */
class NovaApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        try {
            // Verify signature, package name, and anti-kill-sign integrity on launch
            SecurityIntegrityChecker.checkIntegrityOrCrash(this)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.w("NovaApplication", "Startup security note: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        val imageOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val originalUrl = original.url.toString()

                // Replace auto=format with fm=webp to prevent CDN returning AVIF format
                val newUrl = if (originalUrl.contains("auto=format")) {
                    original.url.newBuilder()
                        .removeAllQueryParameters("auto")
                        .setQueryParameter("fm", "webp")
                        .build()
                } else {
                    original.url
                }

                val newRequest = original.newBuilder()
                    .url(newUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/webp,image/png,image/jpeg,image/*;q=0.8")
                    .build()

                chain.proceed(newRequest)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("nova_image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L) // 50MB
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}

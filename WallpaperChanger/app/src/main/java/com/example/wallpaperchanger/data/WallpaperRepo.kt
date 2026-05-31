package com.example.wallpaperchanger.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class WallpaperRepo(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)

    suspend fun setWallpaper(imageUri: Uri, wallpaperMode: String = "scroll"): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(IOException("无法打开图片文件"))

                val original = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (original == null) {
                    return@withContext Result.failure(IOException("无法解码图片"))
                }

                val bitmap = if (wallpaperMode == SettingsStore.WALLPAPER_MODE_STATIC) {
                    // 静态模式：缩放裁剪至屏幕尺寸，壁纸不随桌面翻页
                    val scaled = scaleToScreen(original)
                    original.recycle()
                    scaled
                } else {
                    // 滚动模式：保持原图尺寸，壁纸随桌面翻页横向滚动
                    original
                }

                wallpaperManager.setBitmap(bitmap, null, false)
                bitmap.recycle()
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: SecurityException) {
                Result.failure(SecurityException("没有权限设置壁纸: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("设置壁纸失败: ${e.message}"))
            }
        }

    /**
     * 按 centerCrop 方式将图片缩放裁剪至屏幕尺寸。
     */
    private fun scaleToScreen(original: Bitmap): Bitmap {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels

        val scale = maxOf(
            screenW.toFloat() / original.width,
            screenH.toFloat() / original.height
        )
        val scaledW = (original.width * scale).toInt()
        val scaledH = (original.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(original, scaledW, scaledH, true)

        val cropX = (scaledW - screenW) / 2
        val cropY = (scaledH - screenH) / 2
        val cropped = Bitmap.createBitmap(scaled, cropX, cropY, screenW, screenH)

        if (scaled != cropped) scaled.recycle()
        return cropped
    }

    suspend fun setWallpaperFromFolder(
        folderRepo: FolderRepo,
        folderUri: Uri,
        sequenceMode: String,
        currentIndex: Int,
        wallpaperMode: String
    ): Result<Pair<Uri, Int>> = withContext(Dispatchers.IO) {
        val images = folderRepo.getImagesInFolder(folderUri)
        if (images.isEmpty()) {
            return@withContext Result.failure(Exception("文件夹中没有图片"))
        }

        val (selectedImage, newIndex) = if (sequenceMode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL) {
            val idx = currentIndex % images.size
            Pair(images[idx], (idx + 1) % images.size)
        } else {
            Pair(images.random(), 0)
        }

        val result = setWallpaper(selectedImage, wallpaperMode)
        result.map { Pair(selectedImage, newIndex) }
    }
}

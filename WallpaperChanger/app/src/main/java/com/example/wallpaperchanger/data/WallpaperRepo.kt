package com.example.wallpaperchanger.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperRepo(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)

    /**
     * 将指定 Uri 的图片设置为系统壁纸。
     *
     * @param imageUri 图片 URI
     * @param wallpaperMode "scroll" 滚动模式 / "static" 静态完全呈现模式
     */
    suspend fun setWallpaper(imageUri: Uri, wallpaperMode: String = "scroll"): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("无法打开图片文件"))

                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap == null) {
                    return@withContext Result.failure(Exception("无法解码图片"))
                }

                if (wallpaperMode == SettingsStore.WALLPAPER_MODE_STATIC) {
                    // 静态模式：将壁纸缩放至刚好填满屏幕，不随桌面翻页滚动
                    setStaticWallpaper(bitmap)
                } else {
                    // 滚动模式（默认）：壁纸宽度 > 屏幕宽度，随桌面翻页横向滚动
                    wallpaperManager.setBitmap(bitmap, null, false)
                }

                bitmap.recycle()
                Result.success(Unit)
            } catch (e: SecurityException) {
                Result.failure(Exception("没有权限设置壁纸: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("设置壁纸失败: ${e.message}"))
            }
        }

    /**
     * 设置静态不滚动壁纸。
     * 将图片缩放裁剪至与屏幕同宽高，使壁纸完全填充屏幕。
     */
    private fun setStaticWallpaper(original: Bitmap) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // 等比缩放填满屏幕（centerCrop 逻辑）
        val scale = maxOf(
            screenWidth.toFloat() / original.width.toFloat(),
            screenHeight.toFloat() / original.height.toFloat()
        )
        val scaledWidth = (original.width * scale).toInt()
        val scaledHeight = (original.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)

        // 居中裁剪到屏幕尺寸
        val cropX = (scaledWidth - screenWidth) / 2
        val cropY = (scaledHeight - screenHeight) / 2
        val croppedBitmap = Bitmap.createBitmap(scaledBitmap, cropX, cropY, screenWidth, screenHeight)

        // 设置期望尺寸为屏幕大小，壁纸不再滚动
        wallpaperManager.suggestDesiredDimensions(screenWidth, screenHeight)
        wallpaperManager.setBitmap(
            croppedBitmap,
            null,  // 不使用系统裁剪提示
            false, // 不备份
            WallpaperManager.FLAG_SYSTEM
        )

        if (scaledBitmap != croppedBitmap) scaledBitmap.recycle()
        if (croppedBitmap != original) croppedBitmap.recycle()
    }

    /**
     * 从文件夹选取一张图片并设为壁纸。
     *
     * @param folderRepo 文件夹仓库
     * @param folderUri 文件夹 URI
     * @param sequenceMode "random" 或 "sequential"
     * @param currentIndex 顺序模式下的当前位置
     * @param wallpaperMode "scroll" 或 "static"
     * @return 选中的图片 URI 和新的索引
     */
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
            // 顺序模式：按列表顺序取下一个
            val idx = currentIndex % images.size
            Pair(images[idx], (idx + 1) % images.size)
        } else {
            // 随机模式
            Pair(images.random(), 0)
        }

        val result = setWallpaper(selectedImage, wallpaperMode)
        result.map { Pair(selectedImage, newIndex) }
    }
}

package com.example.wallpaperchanger.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperRepo(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)

    /**
     * 将指定 Uri 的图片设置为系统壁纸（仅主屏幕）。
     */
    suspend fun setWallpaper(imageUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("无法打开图片文件"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("无法解码图片"))
            }

            wallpaperManager.setBitmap(bitmap, null, false)

            bitmap.recycle()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("没有权限设置壁纸: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("设置壁纸失败: ${e.message}"))
        }
    }

    /**
     * 从文件夹随机选取一张图片并设为壁纸。
     */
    suspend fun setRandomWallpaperFromFolder(folderRepo: FolderRepo, folderUri: Uri): Result<Uri> {
        val images = folderRepo.getImagesInFolder(folderUri)
        if (images.isEmpty()) {
            return Result.failure(Exception("文件夹中没有图片"))
        }
        val randomImage = images.random()
        return setWallpaper(randomImage).map { randomImage }
    }
}

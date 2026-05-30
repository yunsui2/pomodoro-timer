package com.example.wallpaperchanger.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns

data class ImageFolder(
    val name: String,
    val path: String,
    val uri: Uri,
    val count: Int
)

class FolderRepo(private val context: Context) {

    /**
     * 通过 MediaStore 扫描所有包含图片的文件夹。
     * 返回按图片数量降序排列的文件夹列表。
     */
    fun scanImageFolders(): List<ImageFolder> {
        val folders = mutableMapOf<String, MutableList<String>>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null, sortOrder
            )
        } else {
            @Suppress("DEPRECATION")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null, sortOrder
            )
        }

        cursor?.use {
            val bucketNameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val bucketIdIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (it.moveToNext()) {
                val bucketName = it.getString(bucketNameIndex) ?: "Unknown"
                val data = it.getString(dataIndex)
                val bucketId = it.getString(bucketIdIndex)
                val key = "$bucketName||$bucketId"
                folders.getOrPut(key) { mutableListOf() }.add(data)
            }
        }

        return folders.map { (key, images) ->
            val (name, bucketId) = key.split("||")
            val contentUri = Uri.parse("${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}?bucketId=$bucketId")
            ImageFolder(
                name = name,
                path = images.first().substringBeforeLast("/"),
                uri = contentUri,
                count = images.size
            )
        }.sortedByDescending { it.count }
    }

    /**
     * 获取指定文件夹 Uri 下的所有图片 Uri 列表。
     * 用于随机选图时构建池。
     */
    fun getImagesInFolder(folderUri: Uri): List<Uri> {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val bucketId = folderUri.getQueryParameter("bucketId")
        val selectionArgs = arrayOf(bucketId ?: return emptyList())

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                images.add(uri)
            }
        }

        return images
    }

    /**
     * 使用 SAF (Storage Access Framework) 让用户手动浏览，
     * 返回用户选择的文件夹 URI 和显示名称。
     */
    fun resolveFolderInfo(uri: Uri): Pair<String, Int> {
        var name = "已选文件夹"
        var count = 0

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    name = cursor.getString(displayNameIndex) ?: name
                }
            }
            count = cursor.count
        }

        return Pair(name, count)
    }
}

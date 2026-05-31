package com.example.wallpaperchanger.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

data class ImageFolder(
    val name: String,
    val path: String,
    val uri: Uri,
    val count: Int
)

class FolderRepo(private val context: Context) {

    companion object {
        /** 判断 URI 是否为 SAF 文件树 URI */
        fun isSafTreeUri(uri: Uri): Boolean {
            val str = uri.toString()
            return str.contains("/tree/") || str.contains("/document/tree")
        }
    }

    /**
     * 通过 MediaStore 扫描所有包含图片的文件夹。
     */
    fun scanImageFolders(): List<ImageFolder> {
        val folders = mutableMapOf<String, MutableList<String>>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        } catch (e: SecurityException) {
            return emptyList()
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
     * 获取文件夹下所有图片的 Uri 列表。
     * 自动区分 MediaStore URI 和 SAF 树 URI。
     */
    fun getImagesInFolder(folderUri: Uri): List<Uri> {
        return if (isSafTreeUri(folderUri)) {
            getSafImages(folderUri)
        } else {
            getMediaStoreImages(folderUri)
        }
    }

    /**
     * MediaStore 方式：通过 bucketId 查询图片。
     */
    private fun getMediaStoreImages(folderUri: Uri): List<Uri> {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)

        val bucketId = folderUri.getQueryParameter("bucketId")
            ?: return emptyList()

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)

        val cursor = try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
        } catch (e: Exception) {
            return emptyList()
        }

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
     * SAF 方式：通过 DocumentFile API 遍历文件树获取图片。
     */
    private fun getSafImages(treeUri: Uri): List<Uri> {
        val images = mutableListOf<Uri>()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        collectImages(rootDoc, images)
        return images
    }

    /** 递归收集 DocumentFile 树中的图片文件 */
    private fun collectImages(dir: DocumentFile, result: MutableList<Uri>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                collectImages(file, result)
            } else if (file.isFile) {
                val mimeType = file.type ?: ""
                if (mimeType.startsWith("image/")) {
                    result.add(file.uri)
                } else if (mimeType.isEmpty()) {
                    // 某些文件可能没有 MIME 类型，按扩展名判断
                    val name = file.name?.lowercase() ?: continue
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".png") || name.endsWith(".gif") ||
                        name.endsWith(".webp") || name.endsWith(".bmp")
                    ) {
                        result.add(file.uri)
                    }
                }
            }
        }
    }

    /**
     * 获取 SAF 文件夹的基本信息（名称和图片数量）。
     */
    fun resolveFolderInfo(uri: Uri): Pair<String, Int> {
        if (isSafTreeUri(uri)) {
            return resolveSafFolderInfo(uri)
        }

        // MediaStore URI
        var name = "已选文件夹"
        var count = 0
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        name = cursor.getString(displayNameIndex) ?: name
                    }
                }
                count = cursor.count
            }
        } catch (e: Exception) {
            // fallback
        }
        return Pair(name, count)
    }

    /**
     * 解析 SAF 文件树的基本信息。
     */
    private fun resolveSafFolderInfo(uri: Uri): Pair<String, Int> {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, uri)
                ?: return Pair("已选文件夹", 0)
            val name = rootDoc.name ?: "已选文件夹"
            val images = mutableListOf<Uri>()
            collectImages(rootDoc, images)
            return Pair(name, images.size)
        } catch (e: Exception) {
            return Pair("已选文件夹", 0)
        }
    }

    /**
     * 对 SAF URI 获取持久化权限，以便后续后台任务（定时换壁纸）也能访问。
     */
    fun takePersistablePermission(uri: Uri) {
        if (!isSafTreeUri(uri)) return
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // 权限已获取或不支持
        }
    }
}

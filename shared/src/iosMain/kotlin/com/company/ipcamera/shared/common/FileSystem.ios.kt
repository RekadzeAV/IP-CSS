package com.company.ipcamera.shared.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

actual class FileSystem actual constructor(context: Any?) {
    private val fileManager = NSFileManager.defaultManager
    
    private val documentsDirectory: String by lazy {
        val urls = fileManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        urls.firstOrNull()?.path ?: ""
    }
    
    private val cacheDirectory: String by lazy {
        val urls = fileManager.URLsForDirectory(
            directory = NSCachesDirectory,
            inDomains = NSUserDomainMask
        )
        urls.firstOrNull()?.path ?: ""
    }
    
    private val tempDirectory: String by lazy {
        NSTemporaryDirectory()
    }
    
    actual fun getDocumentsDirectory(): String {
        return documentsDirectory
    }
    
    actual fun getCacheDirectory(): String {
        return cacheDirectory
    }
    
    actual fun getTempDirectory(): String {
        return tempDirectory
    }
    
    actual fun exists(path: String): Boolean {
        return fileManager.fileExistsAtPath(path)
    }
    
    actual fun isDirectory(path: String): Boolean {
        val isDirectory = platform.objc.alloc<platform.objc.ObjCBool>()
        val exists = fileManager.fileExistsAtPath(path, isDirectory = isDirectory.ptr)
        return exists && isDirectory.value
    }
    
    actual suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.Default) {
        try {
            val url = NSURL.fileURLWithPath(path)
            val errorPtr = platform.objc.alloc<platform.objc.ObjCBool>()
            val success = fileManager.createDirectoryAtURL(
                url = url,
                withIntermediateDirectories = true,
                attributes = null,
                error = errorPtr.ptr
            )
            success
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun ensureDirectory(path: String): Boolean = withContext(Dispatchers.Default) {
        if (exists(path) && isDirectory(path)) {
            true
        } else {
            createDirectory(path)
        }
    }
    
    actual suspend fun readTextFile(path: String): String? = withContext(Dispatchers.Default) {
        try {
            val data = NSData.dataWithContentsOfFile(path)
            if (data != null) {
                NSString.create(data, NSUTF8StringEncoding)?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val data = NSData.dataWithContentsOfFile(path)
            if (data != null) {
                val bytes = ByteArray(data.length.toInt())
                data.getBytes(bytes, data.length)
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun writeTextFile(path: String, text: String, append: Boolean): Boolean = withContext(Dispatchers.Default) {
        try {
            val url = NSURL.fileURLWithPath(path)
            val parentDir = url.URLByDeletingLastPathComponent
            if (parentDir != null) {
                ensureDirectory(parentDir.path ?: "")
            }
            
            val nsString = NSString.create(string = text)
            val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)
            
            if (data != null) {
                if (append && exists(path)) {
                    val existingData = NSData.dataWithContentsOfFile(path)
                    if (existingData != null) {
                        val mutableData = NSMutableData.dataWithData(existingData)
                        mutableData.appendData(data)
                        mutableData.writeToFile(path, atomically = true)
                    } else {
                        data.writeToFile(path, atomically = true)
                    }
                } else {
                    data.writeToFile(path, atomically = true)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun writeBytes(path: String, bytes: ByteArray, append: Boolean): Boolean = withContext(Dispatchers.Default) {
        try {
            val url = NSURL.fileURLWithPath(path)
            val parentDir = url.URLByDeletingLastPathComponent
            if (parentDir != null) {
                ensureDirectory(parentDir.path ?: "")
            }
            
            val data = NSData.create(bytes, bytes.size.toULong())
            
            if (append && exists(path)) {
                val existingData = NSData.dataWithContentsOfFile(path)
                if (existingData != null) {
                    val mutableData = NSMutableData.dataWithData(existingData)
                    mutableData.appendData(data)
                    mutableData.writeToFile(path, atomically = true)
                } else {
                    data.writeToFile(path, atomically = true)
                }
            } else {
                data.writeToFile(path, atomically = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun delete(path: String, recursive: Boolean): Boolean = withContext(Dispatchers.Default) {
        try {
            val url = NSURL.fileURLWithPath(path)
            val errorPtr = platform.objc.alloc<platform.objc.ObjCBool>()
            val success = if (recursive) {
                fileManager.removeItemAtURL(url, error = errorPtr.ptr)
            } else {
                fileManager.removeItemAtPath(path, error = errorPtr.ptr)
            }
            success
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun listDirectory(path: String): List<String> = withContext(Dispatchers.Default) {
        try {
            val contents = fileManager.contentsOfDirectoryAtPath(path, error = null)
            if (contents != null) {
                val basePath = if (path.endsWith("/")) path else "$path/"
                contents.mapNotNull { item ->
                    val itemName = item.toString()
                    "$basePath$itemName"
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    actual suspend fun getFileSize(path: String): Long? = withContext(Dispatchers.Default) {
        try {
            val attributes = fileManager.attributesOfItemAtPath(path, error = null)
            if (attributes != null) {
                val size = attributes[NSFileSize] as? NSNumber
                size?.longValue
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun getAbsolutePath(path: String): String {
        return NSURL.fileURLWithPath(path).standardizedURL?.path ?: path
    }
    
    actual fun getFileName(path: String): String {
        return NSURL.fileURLWithPath(path).lastPathComponent ?: ""
    }
    
    actual fun getParentDirectory(path: String): String? {
        val url = NSURL.fileURLWithPath(path)
        val parent = url.URLByDeletingLastPathComponent
        return parent?.path
    }
    
    actual fun joinPath(vararg paths: String): String {
        if (paths.isEmpty()) return ""
        var result = NSURL.fileURLWithPath(paths[0])
        for (i in 1 until paths.size) {
            result = result.URLByAppendingPathComponent(paths[i])
        }
        return result.standardizedURL?.path ?: ""
    }
}


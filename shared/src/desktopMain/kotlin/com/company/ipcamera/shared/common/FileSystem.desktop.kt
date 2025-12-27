package com.company.ipcamera.shared.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

actual class FileSystem actual constructor(context: Any?) {
    private val userHome: String = System.getProperty("user.home") ?: ""
    
    private val documentsDirectory: String by lazy {
        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> {
                // Windows: Documents folder
                val documents = File(userHome, "Documents")
                File(documents, "IP-CSS").absolutePath
            }
            os.contains("mac") -> {
                // macOS: ~/Documents
                File(userHome, "Documents/IP-CSS").absolutePath
            }
            else -> {
                // Linux: ~/Documents
                File(userHome, "Documents/IP-CSS").absolutePath
            }
        }
    }
    
    private val cacheDirectory: String by lazy {
        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> {
                // Windows: %LOCALAPPDATA%\IP-CSS\Cache
                val localAppData = System.getenv("LOCALAPPDATA") ?: File(userHome, "AppData/Local").absolutePath
                File(localAppData, "IP-CSS/Cache").absolutePath
            }
            os.contains("mac") -> {
                // macOS: ~/Library/Caches/IP-CSS
                File(userHome, "Library/Caches/IP-CSS").absolutePath
            }
            else -> {
                // Linux: ~/.cache/IP-CSS
                val xdgCache = System.getenv("XDG_CACHE_HOME")
                val cacheBase = if (xdgCache != null) {
                    File(xdgCache)
                } else {
                    File(userHome, ".cache")
                }
                File(cacheBase, "IP-CSS").absolutePath
            }
        }
    }
    
    private val tempDirectory: String by lazy {
        System.getProperty("java.io.tmpdir") ?: "/tmp"
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
        return File(path).exists()
    }
    
    actual fun isDirectory(path: String): Boolean {
        return File(path).isDirectory
    }
    
    actual suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            dir.mkdirs()
            dir.exists() && dir.isDirectory
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun ensureDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.exists() && dir.isDirectory
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun readTextFile(path: String): String? = withContext(Dispatchers.IO) {
        try {
            File(path).readText()
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IOException) {
            null
        }
    }
    
    actual suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            File(path).readBytes()
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IOException) {
            null
        }
    }
    
    actual suspend fun writeTextFile(path: String, text: String, append: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            // Создаем родительские директории, если их нет
            file.parentFile?.mkdirs()
            
            if (append) {
                file.appendText(text)
            } else {
                file.writeText(text)
            }
            true
        } catch (e: IOException) {
            false
        }
    }
    
    actual suspend fun writeBytes(path: String, bytes: ByteArray, append: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            // Создаем родительские директории, если их нет
            file.parentFile?.mkdirs()
            
            if (append) {
                file.appendBytes(bytes)
            } else {
                file.writeBytes(bytes)
            }
            true
        } catch (e: IOException) {
            false
        }
    }
    
    actual suspend fun delete(path: String, recursive: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.isDirectory && recursive) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun listDirectory(path: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.map { it.absolutePath } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    actual suspend fun getFileSize(path: String): Long? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file.length()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun getAbsolutePath(path: String): String {
        return File(path).absolutePath
    }
    
    actual fun getFileName(path: String): String {
        return File(path).name
    }
    
    actual fun getParentDirectory(path: String): String? {
        val parent = File(path).parent
        return parent?.takeIf { it.isNotEmpty() }
    }
    
    actual fun joinPath(vararg paths: String): String {
        if (paths.isEmpty()) return ""
        var result = File(paths[0])
        for (i in 1 until paths.size) {
            result = File(result, paths[i])
        }
        return result.absolutePath
    }
}




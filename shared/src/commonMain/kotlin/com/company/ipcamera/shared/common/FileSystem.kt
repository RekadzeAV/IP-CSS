package com.company.ipcamera.shared.common

/**
 * Интерфейс для работы с файловой системой на разных платформах
 * 
 * Предоставляет кроссплатформенный API для:
 * - Работы с файлами (чтение, запись, удаление)
 * - Работы с директориями (создание, перечисление)
 * - Получения стандартных путей (документы, кэш, временные файлы)
 */
expect class FileSystem(context: Any? = null) {
    /**
     * Получить путь к директории документов приложения
     */
    fun getDocumentsDirectory(): String
    
    /**
     * Получить путь к директории кэша приложения
     */
    fun getCacheDirectory(): String
    
    /**
     * Получить путь к директории временных файлов
     */
    fun getTempDirectory(): String
    
    /**
     * Проверить существование файла или директории
     */
    fun exists(path: String): Boolean
    
    /**
     * Проверить, является ли путь директорией
     */
    fun isDirectory(path: String): Boolean
    
    /**
     * Создать директорию (включая все родительские директории)
     */
    suspend fun createDirectory(path: String): Boolean
    
    /**
     * Создать директорию, если она не существует
     */
    suspend fun ensureDirectory(path: String): Boolean
    
    /**
     * Прочитать содержимое файла как строку
     */
    suspend fun readTextFile(path: String): String?
    
    /**
     * Прочитать содержимое файла как байты
     */
    suspend fun readBytes(path: String): ByteArray?
    
    /**
     * Записать строку в файл
     * @param path путь к файлу
     * @param text содержимое для записи
     * @param append если true, добавляет к существующему содержимому
     */
    suspend fun writeTextFile(path: String, text: String, append: Boolean = false): Boolean
    
    /**
     * Записать байты в файл
     * @param path путь к файлу
     * @param bytes данные для записи
     * @param append если true, добавляет к существующему содержимому
     */
    suspend fun writeBytes(path: String, bytes: ByteArray, append: Boolean = false): Boolean
    
    /**
     * Удалить файл или директорию
     * @param path путь к файлу/директории
     * @param recursive если true, удаляет директорию рекурсивно
     */
    suspend fun delete(path: String, recursive: Boolean = false): Boolean
    
    /**
     * Получить список файлов и директорий в указанной директории
     */
    suspend fun listDirectory(path: String): List<String>
    
    /**
     * Получить размер файла в байтах
     */
    suspend fun getFileSize(path: String): Long?
    
    /**
     * Получить абсолютный путь из относительного
     */
    fun getAbsolutePath(path: String): String
    
    /**
     * Получить имя файла из пути
     */
    fun getFileName(path: String): String
    
    /**
     * Получить родительскую директорию из пути
     */
    fun getParentDirectory(path: String): String?
    
    /**
     * Объединить пути
     */
    fun joinPath(vararg paths: String): String
}


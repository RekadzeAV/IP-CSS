package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.StoredFace

/**
 * Репозиторий базы лиц для распознавания (блок 8.3).
 * Хранение эталонных лиц (label + embedding), поиск по сходству, группировка по label.
 */
interface FaceRepository {
    suspend fun insert(face: StoredFace): Result<Unit>

    suspend fun getById(id: String): StoredFace?

    suspend fun getByLabel(
        label: String,
        limit: Int = 100,
    ): List<StoredFace>

    /**
     * Поиск ближайших лиц по эмбеддингу (например, cosine similarity).
     * @param embedding вектор лица
     * @param topK количество лучших совпадений
     * @param minSimilarity минимальный порог сходства (0.0–1.0)
     */
    suspend fun findNearest(
        embedding: FloatArray,
        topK: Int = 10,
        minSimilarity: Float = 0.7f,
    ): List<Pair<StoredFace, Float>>

    suspend fun delete(id: String): Result<Unit>

    suspend fun listLabels(): List<String>

    suspend fun deleteByLabel(label: String): Result<Int>
}

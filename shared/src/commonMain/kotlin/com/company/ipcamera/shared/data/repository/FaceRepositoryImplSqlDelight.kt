package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.database.CameraDatabase
import com.company.ipcamera.shared.domain.model.StoredFace
import com.company.ipcamera.shared.domain.repository.FaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * SQLDelight-реализация репозитория базы лиц.
 */
class FaceRepositoryImplSqlDelight private constructor(
    private val database: CameraDatabase,
) : FaceRepository {
    constructor(databaseFactory: DatabaseFactory) : this(
        createDatabaseSync(databaseFactory.createDriver()),
    )

    internal constructor(testDatabase: CameraDatabase, marker: Unit = Unit) : this(testDatabase)

    override suspend fun insert(face: StoredFace): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.insertFace(
                    id = face.id,
                    label = face.label,
                    embedding_blob = encodeEmbedding(face.embedding),
                    camera_id = face.cameraId,
                    created_at = face.createdAt,
                    metadata = encodeMetadata(face.metadata),
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getById(id: String): StoredFace? =
        withContext(Dispatchers.Default) {
            database.cameraDatabaseQueries
                .selectFaceById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    StoredFace(
                        id = row.id,
                        label = row.label,
                        embedding = decodeEmbedding(row.embedding_blob),
                        cameraId = row.camera_id,
                        createdAt = row.created_at,
                        metadata = decodeMetadata(row.metadata),
                    )
                }
        }

    override suspend fun getByLabel(
        label: String,
        limit: Int,
    ): List<StoredFace> =
        withContext(Dispatchers.Default) {
            database.cameraDatabaseQueries
                .selectFacesByLabel(label)
                .executeAsList()
                .take(limit.coerceAtLeast(0))
                .map { row ->
                    StoredFace(
                        id = row.id,
                        label = row.label,
                        embedding = decodeEmbedding(row.embedding_blob),
                        cameraId = row.camera_id,
                        createdAt = row.created_at,
                        metadata = decodeMetadata(row.metadata),
                    )
                }
        }

    override suspend fun findNearest(
        embedding: FloatArray,
        topK: Int,
        minSimilarity: Float,
    ): List<Pair<StoredFace, Float>> =
        withContext(Dispatchers.Default) {
            if (embedding.isEmpty()) return@withContext emptyList()

            database.cameraDatabaseQueries
                .selectAllFaces()
                .executeAsList()
                .map { row ->
                    StoredFace(
                        id = row.id,
                        label = row.label,
                        embedding = decodeEmbedding(row.embedding_blob),
                        cameraId = row.camera_id,
                        createdAt = row.created_at,
                        metadata = decodeMetadata(row.metadata),
                    )
                }
                .mapNotNull { face ->
                    val similarity = cosineSimilarity(embedding, face.embedding)
                    if (similarity >= minSimilarity) face to similarity else null
                }
                .sortedByDescending { it.second }
                .take(topK.coerceAtLeast(0))
        }

    override suspend fun delete(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                database.cameraDatabaseQueries.deleteFaceById(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun listLabels(): List<String> =
        withContext(Dispatchers.Default) {
            database.cameraDatabaseQueries
                .selectDistinctFaceLabels()
                .executeAsList()
                .map { labelValue ->
                    // Для SELECT DISTINCT одного столбца SQLDelight возвращает значение столбца напрямую.
                    labelValue
                }
        }

    override suspend fun deleteByLabel(label: String): Result<Int> =
        withContext(Dispatchers.Default) {
            try {
                val before = database.cameraDatabaseQueries.selectFacesByLabel(label).executeAsList().size
                database.cameraDatabaseQueries.deleteFacesByLabel(label)
                Result.success(before)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun encodeEmbedding(embedding: FloatArray): String = embedding.joinToString(",")

    private fun decodeEmbedding(value: String): FloatArray {
        if (value.isBlank()) return floatArrayOf()
        return value.split(',')
            .mapNotNull { it.toFloatOrNull() }
            .toFloatArray()
    }

    private fun encodeMetadata(metadata: Map<String, String>): String? {
        if (metadata.isEmpty()) return null
        return metadata.entries.joinToString(";") { (k, v) ->
            "${escapeMeta(k)}=${escapeMeta(v)}"
        }
    }

    private fun decodeMetadata(metadata: String?): Map<String, String> {
        if (metadata.isNullOrBlank()) return emptyMap()
        return metadata.split(';')
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val key = unescapeMeta(pair.substring(0, separator))
                val value = unescapeMeta(pair.substring(separator + 1))
                key to value
            }
            .toMap()
    }

    private fun escapeMeta(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("=", "\\=")
            .replace(";", "\\;")
    }

    private fun unescapeMeta(value: String): String {
        val builder = StringBuilder(value.length)
        var escaped = false
        value.forEach { ch ->
            if (escaped) {
                builder.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else {
                builder.append(ch)
            }
        }
        if (escaped) builder.append('\\')
        return builder.toString()
    }

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray,
    ): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            val av = a[i].toDouble()
            val bv = b[i].toDouble()
            dot += av * bv
            normA += av * av
            normB += bv * bv
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0f else (dot / denominator).toFloat()
    }
}

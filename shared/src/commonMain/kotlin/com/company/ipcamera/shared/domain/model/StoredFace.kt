package com.company.ipcamera.shared.domain.model

/**
 * Сохранённое лицо в галерее для распознавания (блок 8.3).
 * label — имя/идентификатор человека; embedding — вектор из детектора лиц.
 * (FloatArray не сериализуется в JSON по умолчанию; для API использовать DTO с List<Float>.)
 */
data class StoredFace(
    val id: String,
    val label: String,
    val embedding: FloatArray,
    val cameraId: String? = null,
    val createdAt: Long,
    val metadata: Map<String, String> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoredFace) return false
        if (id != other.id) return false
        return embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

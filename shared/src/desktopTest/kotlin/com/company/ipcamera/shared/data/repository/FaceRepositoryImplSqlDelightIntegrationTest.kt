package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.domain.model.StoredFace
import com.company.ipcamera.shared.test.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Интеграционные тесты FaceRepositoryImplSqlDelight на in-memory SQLite.
 */
class FaceRepositoryImplSqlDelightIntegrationTest {
    @Test
    fun `insert and getById should persist face with metadata`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()
            val repository = FaceRepositoryImplSqlDelight(database, Unit)

            val face =
                StoredFace(
                    id = "face-1",
                    label = "john",
                    embedding = floatArrayOf(0.1f, 0.2f, 0.3f),
                    cameraId = "cam-1",
                    createdAt = 1_700_000_000_000,
                    metadata = mapOf("source" to "test", "tag" to "employee"),
                )

            val insertResult = repository.insert(face)
            assertTrue(insertResult.isSuccess)

            val stored = repository.getById("face-1")
            assertNotNull(stored)
            assertEquals("john", stored.label)
            assertEquals("cam-1", stored.cameraId)
            assertEquals("test", stored.metadata["source"])
            assertEquals(3, stored.embedding.size)
            assertTrue((stored.embedding[0] - 0.1f) < 0.0001f)
        }

    @Test
    fun `getByLabel and listLabels should return grouped labels`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()
            val repository = FaceRepositoryImplSqlDelight(database, Unit)

            repository.insert(
                StoredFace(
                    id = "face-a",
                    label = "alice",
                    embedding = floatArrayOf(1f, 0f),
                    createdAt = 1,
                ),
            )
            repository.insert(
                StoredFace(
                    id = "face-b",
                    label = "alice",
                    embedding = floatArrayOf(0.9f, 0.1f),
                    createdAt = 2,
                ),
            )
            repository.insert(
                StoredFace(
                    id = "face-c",
                    label = "bob",
                    embedding = floatArrayOf(0f, 1f),
                    createdAt = 3,
                ),
            )

            val aliceFaces = repository.getByLabel("alice", limit = 10)
            val labels = repository.listLabels()

            assertEquals(2, aliceFaces.size)
            assertEquals(listOf("alice", "bob"), labels)
        }

    @Test
    fun `findNearest should return top matches sorted by similarity`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()
            val repository = FaceRepositoryImplSqlDelight(database, Unit)

            repository.insert(
                StoredFace(
                    id = "face-near",
                    label = "near",
                    embedding = floatArrayOf(1f, 0f),
                    createdAt = 1,
                ),
            )
            repository.insert(
                StoredFace(
                    id = "face-mid",
                    label = "mid",
                    embedding = floatArrayOf(0.5f, 0.5f),
                    createdAt = 2,
                ),
            )
            repository.insert(
                StoredFace(
                    id = "face-far",
                    label = "far",
                    embedding = floatArrayOf(0f, 1f),
                    createdAt = 3,
                ),
            )

            val nearest =
                repository.findNearest(
                    embedding = floatArrayOf(0.9f, 0.1f),
                    topK = 2,
                    minSimilarity = 0.3f,
                )

            assertEquals(2, nearest.size)
            assertEquals("face-near", nearest[0].first.id)
            assertTrue(nearest[0].second >= nearest[1].second)
        }

    @Test
    fun `delete and deleteByLabel should remove expected records`() =
        runTest {
            val database = TestDatabaseFactory.createTestDatabase()
            val repository = FaceRepositoryImplSqlDelight(database, Unit)

            repository.insert(
                StoredFace(id = "face-1", label = "tmp", embedding = floatArrayOf(1f), createdAt = 1),
            )
            repository.insert(
                StoredFace(id = "face-2", label = "tmp", embedding = floatArrayOf(0.9f), createdAt = 2),
            )
            repository.insert(
                StoredFace(id = "face-3", label = "keep", embedding = floatArrayOf(0.1f), createdAt = 3),
            )

            val deleteOneResult = repository.delete("face-1")
            assertTrue(deleteOneResult.isSuccess)
            assertNull(repository.getById("face-1"))

            val deleteByLabelResult = repository.deleteByLabel("tmp")
            assertTrue(deleteByLabelResult.isSuccess)
            assertEquals(1, deleteByLabelResult.getOrThrow())
            assertEquals(1, repository.getByLabel("keep", 10).size)
        }
}

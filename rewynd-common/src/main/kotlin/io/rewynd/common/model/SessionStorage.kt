package io.rewynd.common.model

interface SessionStorage {
    suspend fun invalidate(id: String)

    suspend fun write(
        id: String,
        value: String,
    )

    suspend fun read(id: String): String
}

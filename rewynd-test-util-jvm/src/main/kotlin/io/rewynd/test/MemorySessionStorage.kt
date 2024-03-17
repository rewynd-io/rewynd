package io.rewynd.test

import io.rewynd.common.model.SessionStorage
import java.util.concurrent.ConcurrentHashMap

class MemorySessionStorage : SessionStorage {
    private val store = ConcurrentHashMap<String, String>()

    override suspend fun invalidate(id: String) {
        store.remove(id)
    }

    override suspend fun write(
        id: String,
        value: String,
    ) {
        store[id] = value
    }

    override suspend fun read(id: String): String = store[id] ?: throw NoSuchElementException("Session $id not found")
}

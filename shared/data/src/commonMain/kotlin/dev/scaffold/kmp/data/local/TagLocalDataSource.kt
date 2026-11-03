package dev.scaffold.kmp.data.local

import dev.scaffold.kmp.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TagLocalDataSource {
    fun observeAll(): Flow<List<Tag>>
    suspend fun getAll(): List<Tag>
    suspend fun replaceAll(items: List<Tag>)
    suspend fun upsert(item: Tag)
}

class InMemoryTagLocalDataSource(
    initialItems: List<Tag> = emptyList(),
) : TagLocalDataSource {
    private val mutex = Mutex()
    private val items = initialItems.associateByTo(linkedMapOf()) { it.id }
    private val state = MutableStateFlow(initialItems.toList())

    override fun observeAll(): Flow<List<Tag>> = state.asStateFlow()

    override suspend fun getAll(): List<Tag> =
        mutex.withLock { items.values.toList() }

    override suspend fun replaceAll(items: List<Tag>) {
        mutex.withLock {
            this.items.clear()
            items.forEach { this.items[it.id] = it }
            state.value = this.items.values.toList()
        }
    }

    override suspend fun upsert(item: Tag) {
        mutex.withLock {
            items[item.id] = item
            state.value = items.values.toList()
        }
    }
}

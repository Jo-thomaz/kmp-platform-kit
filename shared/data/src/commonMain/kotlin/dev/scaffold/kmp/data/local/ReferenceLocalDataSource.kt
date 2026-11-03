package dev.scaffold.kmp.data.local

import dev.scaffold.kmp.domain.model.ReferenceItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ReferenceLocalDataSource {
    fun observeAll(): Flow<List<ReferenceItem>>
    suspend fun getAll(): List<ReferenceItem>
    suspend fun replaceAll(items: List<ReferenceItem>)
    suspend fun upsert(item: ReferenceItem)
}

class InMemoryReferenceLocalDataSource(
    initialItems: List<ReferenceItem> = emptyList(),
) : ReferenceLocalDataSource {
    private val mutex = Mutex()
    private val items = initialItems.associateByTo(linkedMapOf()) { it.id }
    private val state = MutableStateFlow(initialItems.toList())

    override fun observeAll(): Flow<List<ReferenceItem>> = state.asStateFlow()

    override suspend fun getAll(): List<ReferenceItem> =
        mutex.withLock { items.values.toList() }

    override suspend fun replaceAll(items: List<ReferenceItem>) {
        mutex.withLock {
            this.items.clear()
            items.forEach { this.items[it.id] = it }
            state.value = this.items.values.toList()
        }
    }

    override suspend fun upsert(item: ReferenceItem) {
        mutex.withLock {
            items[item.id] = item
            state.value = items.values.toList()
        }
    }
}

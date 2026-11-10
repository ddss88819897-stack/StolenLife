package kr.paranos.stolenlife.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.stolenLifeStore: DataStore<Preferences> by preferencesDataStore(name = "stolenlife")

/**
 * 진행 상황 저장소. 웹판 localStorage('stolenlife_v21') 와 같은 역할이다.
 * GameState 전체를 JSON 한 줄로 넣고 뺀다. 읽기에 실패하면(구버전 · 손상) 저장이 없는 것으로 본다.
 */
class SaveStore(context: Context) {

    private val store = context.applicationContext.stolenLifeStore
    private val key = stringPreferencesKey(SAVE_KEY)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): GameState? = runCatching {
        store.data.first()[key]?.let { json.decodeFromString(GameState.serializer(), it) }
    }.getOrNull()

    suspend fun save(state: GameState) {
        val text = json.encodeToString(GameState.serializer(), state)
        store.edit { it[key] = text }
    }

    suspend fun clear() {
        store.edit { it.remove(key) }
    }

    private companion object {
        const val SAVE_KEY = "stolenlife_v21"
    }
}

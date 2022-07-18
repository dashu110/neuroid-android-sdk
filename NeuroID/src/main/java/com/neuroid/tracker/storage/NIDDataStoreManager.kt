package com.neuroid.tracker.storage

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.security.crypto.MasterKey
import com.neuroid.tracker.events.USER_INACTIVE
import com.neuroid.tracker.events.WINDOW_BLUR
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.utils.NIDTimerActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Semaphore

interface NIDDataStoreManager {
    fun saveEvent(event: NIDEventModel)
    suspend fun getAllEvents(): Set<String>
    fun addViewIdExclude(id: String)
    suspend fun clearEvents()
}

fun initDataStoreCtx(context: Context) {
    NIDDataStoreManagerImp.init(context)
}

fun getDataStoreInstance(): NIDDataStoreManager {
    return NIDDataStoreManagerImp
}

private object NIDDataStoreManagerImp : NIDDataStoreManager {
    private const val NID_SHARED_PREF_FILE = "NID_SHARED_PREF_FILE"
    private const val NID_STRING_EVENTS = "NID_STRING_EVENTS"
    private var sharedPref: SharedPreferences? = null
    private val sharedLock = Semaphore(1)
    private val listNonActiveEvents = listOf(
        USER_INACTIVE,
        WINDOW_BLUR //Block screen
    )
    private val listIdsExcluded = arrayListOf<String>()

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            sharedPref = context.getSharedPreferences(NID_SHARED_PREF_FILE, MODE_PRIVATE)
        }
    }

    private fun getKeyAlias(context: Context) = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    @Synchronized
    override fun saveEvent(event: NIDEventModel) {
        CoroutineScope(Dispatchers.IO).launch {
            if (listIdsExcluded.none { it == event.tgs || it == event.tg?.get("tgs") }) {
                val strEvent = event.getOwnJson()

                if (NIDJobServiceManager.userActive.not()) {
                    NIDJobServiceManager.userActive = true
                    NIDJobServiceManager.restart()
                }

                sharedLock.acquire()
                if (!listNonActiveEvents.any { strEvent.contains(it) }) {
                    NIDTimerActive.restartTimerActive()
                }
                val lastEvents = getStringSet(NID_STRING_EVENTS)
                val newEvents = LinkedHashSet<String>()
                newEvents.addAll(lastEvents)
                newEvents.add(strEvent)
                putStringSet(NID_STRING_EVENTS, newEvents)

                sharedLock.release()
            }
        }
    }

    override suspend fun getAllEvents(): Set<String> {
        sharedLock.acquire()
        val lastEvents = getStringSet(NID_STRING_EVENTS, emptySet())
        clearEvents()
        sharedLock.release()
        return lastEvents
    }

    override fun addViewIdExclude(id: String) {
        if (listIdsExcluded.none { it == id }) {
            listIdsExcluded.add(id)
        }
    }

    override suspend fun clearEvents() {
        putStringSet(NID_STRING_EVENTS, emptySet())
    }

    private suspend fun putStringSet(key: String, stringSet: Set<String>) {
        sharedPref?.let {
            with(it.edit()) {
                putStringSet(key, stringSet)
                apply()
            }
        }
    }

    private suspend fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> {
        return sharedPref?.getStringSet(key, default) ?: default
    }

}
package com.wwwescape.pixelebookreader.data.browse

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browseSourcesDataStore by preferencesDataStore(name = "browse_sources")

// Unit separator (U+001F): won't appear in a content:// uri or a real folder name.
private val RECORD_SEPARATOR = 0x1F.toChar()

/** Folder trees the user has granted access to via SAF's document-tree picker — Browse
 * navigates within these. There's no way to browse the device more broadly while staying
 * SAF-only, so each source doubles as a "pinned folder" shortcut — there's no separate, deeper
 * per-subfolder pinning mechanism. */
object BrowseSourceRepository {

    private val SOURCES_KEY = stringSetPreferencesKey("sources")

    fun sourcesFlow(context: Context): Flow<List<BrowseSource>> =
        context.browseSourcesDataStore.data.map { prefs ->
            (prefs[SOURCES_KEY] ?: emptySet()).mapNotNull { it.toSourceOrNull() }
        }

    suspend fun addSource(context: Context, treeUri: Uri) {
        FileSystemRepository.persistPermission(context, treeUri)
        val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: treeUri.lastPathSegment ?: treeUri.toString()
        context.browseSourcesDataStore.edit { prefs ->
            val current = prefs[SOURCES_KEY] ?: emptySet()
            prefs[SOURCES_KEY] = current + "$treeUri$RECORD_SEPARATOR$name"
        }
    }

    suspend fun removeSource(context: Context, source: BrowseSource) {
        context.browseSourcesDataStore.edit { prefs ->
            val current = prefs[SOURCES_KEY] ?: emptySet()
            prefs[SOURCES_KEY] = current.filterNot { it.toSourceOrNull()?.uri == source.uri }.toSet()
        }
        FileSystemRepository.releasePermission(context, source.uri)
    }

    private fun String.toSourceOrNull(): BrowseSource? {
        val parts = split(RECORD_SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        return BrowseSource(parts[0].toUri(), parts[1])
    }
}

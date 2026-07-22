package com.wwwescape.pixelebookreader.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.book.BookRepository
import com.wwwescape.pixelebookreader.data.book.CoverStorage
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.data.category.CategoryRepository
import com.wwwescape.pixelebookreader.data.category.LibrarySortOrder
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPreset
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPresetRepository
import com.wwwescape.pixelebookreader.data.filesystem.FileSystemRepository
import com.wwwescape.pixelebookreader.data.history.HistoryEntry
import com.wwwescape.pixelebookreader.data.history.HistoryRepository
import com.wwwescape.pixelebookreader.data.settings.AppSettings
import com.wwwescape.pixelebookreader.data.settings.SettingsRepository
import com.wwwescape.pixelebookreader.data.settings.ThemeContrast
import com.wwwescape.pixelebookreader.data.settings.ThemeMode
import com.wwwescape.pixelebookreader.data.stats.ReadingSession
import com.wwwescape.pixelebookreader.data.stats.ReadingSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val SCHEMA_VERSION = 1

enum class ImportMode {
    /** Wipes the current library/settings and replaces them with the backup's — a full device-
     * migration restore. */
    REPLACE,

    /** Adds the backup's books/categories/color presets/history/reading sessions as new rows
     * alongside whatever's already here (ids remapped as needed for cross-references) and
     * leaves the current device's app-wide settings untouched. No attempt at deduplicating
     * books that already exist by title/author — a "smart merge" is a meaningfully bigger
     * feature than this phase's scope, and re-importing the same backup twice will visibly
     * double up the library, by design (the alternative, silent fuzzy-matching, risks silently
     * dropping books the user actually wanted). */
    MERGE,
}

data class ImportSummary(val booksImported: Int, val categoriesImported: Int)

/** Local, account-free backup. Exports/imports a
 * single JSON file via SAF. Deliberately **does not** back up [com.wwwescape.pixelebookreader.data.reader.ReaderSettings]
 * (the in-reader typography/gestures/focus-aids panel, ~46 DataStore fields) — only the main
 * Settings screen's app-wide [AppSettings] is included, since that's what "settings" means in
 * this phase's own checklist wording, and hand-serializing every reader-customization field
 * would be a lot of surface area for a comparatively low-stakes settings group (it's explicitly
 * "tuned live while reading" per the "Settings, two-tier" architecture decision, not something a
 * device-migration restore needs to get exactly right).
 *
 * Book *files* aren't copied into the backup, only their SAF uri strings — a restored uri is
 * only valid if the same file is still reachable at that uri (same device, or the same folder
 * re-granted on a new one); this app has no way to know in advance whether that'll be true, so
 * it's surfaced as an in-app note rather than silently failing later. Cover images, by contrast,
 * *are* embedded (base64-encoded JPEG bytes) — unlike a book file, a cover is small,
 * internally-managed, and the user has no separate way to "re-point" it, so leaving it out would
 * make every restored book look broken. */
object BackupRepository {

    suspend fun export(context: Context, destination: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("schemaVersion", SCHEMA_VERSION)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("appSettings", SettingsRepository.settingsFlow(context).first().toJson())
            root.put("categories", JSONArray(CategoryRepository.getAllCategories(context).first().map { it.toJson() }))
            root.put("colorPresets", JSONArray(ColorPresetRepository.getAllPresets(context).first().map { it.toJson() }))
            root.put("books", JSONArray(BookRepository.getAllBooks(context).first().map { it.toJson() }))
            root.put("history", JSONArray(HistoryRepository.getAllHistory(context).first().map { it.toJson() }))
            root.put("readingSessions", JSONArray(ReadingSessionRepository.getAllSessions(context).first().map { it.toJson() }))

            context.contentResolver.openOutputStream(destination)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Couldn't open the destination file for writing")
        }
    }

    suspend fun import(context: Context, source: Uri, mode: ImportMode): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val root = FileSystemRepository.readBytes(context, source)
                ?.let { JSONObject(String(it, Charsets.UTF_8)) }
                ?: error("Couldn't read the backup file")

            if (mode == ImportMode.REPLACE) {
                BookRepository.deleteAllBooks(context)
                CategoryRepository.deleteAllCategories(context)
                ColorPresetRepository.deleteAllPresets(context)
                HistoryRepository.deleteAllHistory(context)
                ReadingSessionRepository.deleteAllSessions(context)
                root.optJSONObject("appSettings")?.let { applyAppSettings(context, it) }
            }

            val categoryIdMap = mutableMapOf<Int, Int>()
            root.optJSONArray("categories")?.let { array ->
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    val originalId = json.getInt("id")
                    val category = categoryFromJson(json, preserveId = mode == ImportMode.REPLACE)
                    val newId = CategoryRepository.addCategory(context, category).toInt()
                    categoryIdMap[originalId] = if (mode == ImportMode.REPLACE) originalId else newId
                }
            }

            val bookIdMap = mutableMapOf<Int, Int>()
            var booksImported = 0
            root.optJSONArray("books")?.let { array ->
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    val originalId = json.getInt("id")
                    val remappedCategories = json.optJSONArray("categories")?.let { cats ->
                        (0 until cats.length()).mapNotNull { categoryIdMap[cats.getInt(it)] }
                    }.orEmpty()
                    val coverPath = json.optString("coverBase64", "").ifEmpty { null }
                        ?.let { CoverStorage.save(context, Base64.decode(it, Base64.DEFAULT)) }
                    val book = bookFromJson(json, remappedCategories, coverPath, preserveId = mode == ImportMode.REPLACE)
                    val newId = BookRepository.addBook(context, book).toInt()
                    bookIdMap[originalId] = if (mode == ImportMode.REPLACE) originalId else newId
                    booksImported++
                }
            }

            root.optJSONArray("colorPresets")?.let { array ->
                for (i in 0 until array.length()) {
                    ColorPresetRepository.addPreset(context, colorPresetFromJson(array.getJSONObject(i), preserveId = mode == ImportMode.REPLACE))
                }
            }

            root.optJSONArray("history")?.let { array ->
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    val bookId = bookIdMap[json.getInt("bookId")] ?: continue
                    HistoryRepository.recordOpen(context, bookId, json.getLong("openedAt"))
                }
            }

            root.optJSONArray("readingSessions")?.let { array ->
                for (i in 0 until array.length()) {
                    val json = array.getJSONObject(i)
                    val bookId = bookIdMap[json.getInt("bookId")] ?: continue
                    ReadingSessionRepository.recordSession(context, bookId, json.getLong("startedAt"), json.getLong("durationMs"))
                }
            }

            ImportSummary(booksImported = booksImported, categoriesImported = categoryIdMap.size)
        }
    }

    private fun AppSettings.toJson() = JSONObject().apply {
        put("themeMode", themeMode.name)
        put("useDynamicColor", useDynamicColor)
        put("colorThemeId", colorThemeId)
        put("themeContrast", themeContrast.name)
        put("pureDark", pureDark)
        put("absoluteDark", absoluteDark)
        put("showStartScreen", showStartScreen)
        put("doublePressBackToExit", doublePressBackToExit)
    }

    private suspend fun applyAppSettings(context: Context, json: JSONObject) {
        SettingsRepository.setThemeMode(context, runCatching { ThemeMode.valueOf(json.getString("themeMode")) }.getOrDefault(ThemeMode.SYSTEM))
        SettingsRepository.setDynamicColor(context, json.optBoolean("useDynamicColor", true))
        SettingsRepository.setColorThemeId(context, json.optString("colorThemeId", "").ifEmpty { null })
        SettingsRepository.setThemeContrast(context, runCatching { ThemeContrast.valueOf(json.getString("themeContrast")) }.getOrDefault(ThemeContrast.STANDARD))
        SettingsRepository.setPureDark(context, json.optBoolean("pureDark", false))
        SettingsRepository.setAbsoluteDark(context, json.optBoolean("absoluteDark", false))
        SettingsRepository.setShowStartScreen(context, json.optBoolean("showStartScreen", true))
        SettingsRepository.setDoublePressBackToExit(context, json.optBoolean("doublePressBackToExit", false))
    }

    private fun Category.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("order", order)
        put("sortOrder", sortOrder.name)
        put("sortDescending", sortDescending)
    }

    private fun categoryFromJson(json: JSONObject, preserveId: Boolean) = Category(
        id = if (preserveId) json.getInt("id") else 0,
        title = json.getString("title"),
        order = json.optInt("order", 0),
        sortOrder = runCatching { LibrarySortOrder.valueOf(json.getString("sortOrder")) }.getOrDefault(LibrarySortOrder.LAST_READ),
        sortDescending = json.optBoolean("sortDescending", true),
    )

    private fun ColorPreset.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("backgroundColor", backgroundColor)
        put("fontColor", fontColor)
        put("order", order)
    }

    private fun colorPresetFromJson(json: JSONObject, preserveId: Boolean) = ColorPreset(
        id = if (preserveId) json.getInt("id") else 0,
        name = json.getString("name"),
        backgroundColor = json.getLong("backgroundColor"),
        fontColor = json.getLong("fontColor"),
        order = json.optInt("order", 0),
    )

    private fun Book.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("author", author)
        put("description", description)
        put("filePath", filePath)
        put("fileName", fileName)
        coverImagePath?.let { path ->
            runCatching { File(path).takeIf { it.exists() }?.readBytes() }.getOrNull()?.let {
                put("coverBase64", Base64.encodeToString(it, Base64.NO_WRAP))
            }
        }
        put("scrollIndex", scrollIndex)
        put("scrollOffset", scrollOffset)
        put("progress", progress.toDouble())
        put("lastOpened", lastOpened)
        put("categories", JSONArray(categories))
        put("series", series)
        put("seriesNumber", seriesNumber?.toDouble())
        put("tags", JSONArray(tags))
        put("rating", rating?.toDouble())
        put("isbn", isbn)
        put("publishDate", publishDate)
        put("language", language)
        put("chaptersRead", chaptersRead)
        put("finishedAt", finishedAt)
    }

    private fun bookFromJson(json: JSONObject, categories: List<Int>, coverPath: String?, preserveId: Boolean) = Book(
        id = if (preserveId) json.getInt("id") else 0,
        title = json.getString("title"),
        author = json.optStringOrNull("author"),
        description = json.optStringOrNull("description"),
        filePath = json.getString("filePath"),
        fileName = json.getString("fileName"),
        coverImagePath = coverPath,
        scrollIndex = json.optInt("scrollIndex", 0),
        scrollOffset = json.optInt("scrollOffset", 0),
        progress = json.optDouble("progress", 0.0).toFloat(),
        lastOpened = json.optLongOrNull("lastOpened"),
        categories = categories,
        series = json.optStringOrNull("series"),
        seriesNumber = json.optDoubleOrNull("seriesNumber")?.toFloat(),
        tags = json.optJSONArray("tags")?.let { array -> (0 until array.length()).map { array.getString(it) } }.orEmpty(),
        rating = json.optDoubleOrNull("rating")?.toFloat(),
        isbn = json.optStringOrNull("isbn"),
        publishDate = json.optStringOrNull("publishDate"),
        language = json.optStringOrNull("language"),
        chaptersRead = json.optInt("chaptersRead", 0),
        finishedAt = json.optLongOrNull("finishedAt"),
    )

    private fun HistoryEntry.toJson() = JSONObject().apply {
        put("bookId", bookId)
        put("openedAt", openedAt)
    }

    private fun ReadingSession.toJson() = JSONObject().apply {
        put("bookId", bookId)
        put("startedAt", startedAt)
        put("durationMs", durationMs)
    }

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name, "").ifEmpty { null }

    private fun JSONObject.optLongOrNull(name: String): Long? = if (has(name) && !isNull(name)) getLong(name) else null

    private fun JSONObject.optDoubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) getDouble(name) else null
}

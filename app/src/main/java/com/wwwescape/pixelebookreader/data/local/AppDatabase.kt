package com.wwwescape.pixelebookreader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wwwescape.pixelebookreader.data.book.Book
import com.wwwescape.pixelebookreader.data.book.BookDao
import com.wwwescape.pixelebookreader.data.bookmark.Bookmark
import com.wwwescape.pixelebookreader.data.bookmark.BookmarkDao
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.data.category.CategoryDao
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPreset
import com.wwwescape.pixelebookreader.data.colorpreset.ColorPresetDao
import com.wwwescape.pixelebookreader.data.highlight.Highlight
import com.wwwescape.pixelebookreader.data.highlight.HighlightDao
import com.wwwescape.pixelebookreader.data.history.HistoryDao
import com.wwwescape.pixelebookreader.data.history.HistoryEntry
import com.wwwescape.pixelebookreader.data.stats.ReadingSession
import com.wwwescape.pixelebookreader.data.stats.ReadingSessionDao

/** Adds [Category.sortOrder]/[Category.sortDescending] (a per-category sort
 * override), defaulting existing rows to `LAST_READ` descending — the same default the entity
 * itself already used pre-migration. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder TEXT NOT NULL DEFAULT 'LAST_READ'")
        db.execSQL("ALTER TABLE categories ADD COLUMN sortDescending INTEGER NOT NULL DEFAULT 1")
    }
}

/** Adds [Book.fileName] — the original SAF display name, needed to dispatch the right format
 * parser (a raw content:// uri's `lastPathSegment` isn't reliably a real file name). No rows
 * exist yet in practice (this app hasn't shipped), so the empty-string default is a formality. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN fileName TEXT NOT NULL DEFAULT ''")
    }
}

/** Adds the `color_presets` table (reading color presets) — structured
 * list data with manual ordering, the same shape as `categories`, so it lives in Room rather
 * than DataStore for the same reason categories do. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `color_presets` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`name` TEXT NOT NULL, " +
                "`backgroundColor` INTEGER NOT NULL, " +
                "`fontColor` INTEGER NOT NULL, " +
                "`order` INTEGER NOT NULL)",
        )
    }
}

/** Adds richer `Book` metadata: series/seriesNumber, tags, rating, isbn,
 * publishDate, language. All nullable/empty-default, so existing rows need no backfill. */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN series TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN seriesNumber REAL")
        db.execSQL("ALTER TABLE books ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE books ADD COLUMN rating REAL")
        db.execSQL("ALTER TABLE books ADD COLUMN isbn TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN publishDate TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN language TEXT")
    }
}

/** Adds reading-statistics tracking: the `reading_sessions` table (debounced
 * per-session reading time, see `ReaderViewModel`) and `Book.chaptersRead`/`Book.finishedAt`
 * (monotonic progress markers for the "chapters finished"/"books finished" counters). */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN chaptersRead INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE books ADD COLUMN finishedAt INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_sessions` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`bookId` INTEGER NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, " +
                "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_bookId` ON `reading_sessions` (`bookId`)")
    }
}

/** Adds the `bookmarks` and `highlights` tables (Bookmarks & Highlights, a differentiator) —
 * saved reading positions and highlighted passages, both keyed on `bookId` with the same
 * `CASCADE` shape as `reading_sessions`. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmarks` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`bookId` INTEGER NOT NULL, " +
                "`contentIndex` INTEGER NOT NULL, " +
                "`snippet` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_bookId` ON `bookmarks` (`bookId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `highlights` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`bookId` INTEGER NOT NULL, " +
                "`contentIndex` INTEGER NOT NULL, " +
                "`startOffset` INTEGER NOT NULL, " +
                "`endOffset` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`color` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_bookId` ON `highlights` (`bookId`)")
    }
}

@Database(
    entities = [Book::class, Category::class, HistoryEntry::class, ColorPreset::class, ReadingSession::class, Bookmark::class, Highlight::class],
    version = 7,
    exportSchema = true,
)
@TypeConverters(IntListConverter::class, StringListConverter::class, LibrarySortOrderConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun historyDao(): HistoryDao
    abstract fun colorPresetDao(): ColorPresetDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixel_e_book_reader.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build().also { instance = it }
            }
    }
}

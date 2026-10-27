package rocks.urzahil.confnav

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rocks.urzahil.confnav.data.local.AppDatabase
import java.io.File

/**
 * Builds a database exactly as an older app version left it (from the committed schema file), then
 * opens it the way the app does. Room runs the migrations on open and fails if the result does not
 * match the current schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `1 to current drops isBookmarked and keeps every session`() {
        createDatabaseAtVersion(1)
        SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL(
                """INSERT INTO conference_sessions (title, speaker, speakerRole, date, startTime, endTime,
                   locationName, address, latitude, longitude, colorHex, track, description, isBookmarked)
                   VALUES ('Opening Keynote', 'Ada Lovelace', 'Principal Engineer', '2026-10-20', '09:00', '10:00',
                   'Main Hall', '1 Conference Way', 40.7128, -74.006, '#0D9488', 'Keynote', 'Welcome', 1)"""
            )
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        try {
            val session = runBlocking { db.conferenceDao().getAllSessions().first() }.single()
            assertEquals("Opening Keynote", session.title)
            assertEquals("Ada Lovelace", session.speaker)
            assertEquals("Principal Engineer", session.speakerRole)
            assertEquals("2026-10-20", session.date)
            assertEquals("09:00", session.startTime)
            assertEquals("10:00", session.endTime)
            assertEquals("Main Hall", session.locationName)
            assertEquals("1 Conference Way", session.address)
            assertEquals(40.7128, session.latitude, 0.0)
            assertEquals(-74.006, session.longitude, 0.0)
            assertEquals("#0D9488", session.colorHex)
            assertEquals("Keynote", session.track)
            assertEquals("Welcome", session.description)

            val columns = db.openHelper.readableDatabase.query("PRAGMA table_info(conference_sessions)").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
            assertFalse("isBookmarked" in columns)
            assertEquals(3, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun `2 to 3 adds starred and leaves existing sessions unstarred`() {
        createDatabaseAtVersion(2)
        SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL(
                """INSERT INTO conference_sessions (title, speaker, speakerRole, date, startTime, endTime,
                   locationName, address, latitude, longitude, colorHex, track, description)
                   VALUES ('Opening Keynote', 'Ada Lovelace', '', '2026-10-20', '09:00', '10:00',
                   'Main Hall', '', 40.7128, -74.006, '#0D9488', '', '')"""
            )
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        try {
            val session = runBlocking { db.conferenceDao().getAllSessions().first() }.single()
            assertEquals("Opening Keynote", session.title)
            assertFalse(session.starred)
            assertEquals(3, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }

    /** Recreates schema [version] from app/schemas, the same way Room's MigrationTestHelper does. */
    private fun createDatabaseAtVersion(version: Int) {
        val file = File("schemas/${AppDatabase::class.java.name}/$version.json")
        val schema = Moshi.Builder().build().adapter(Map::class.java).fromJson(file.readText())!!
        val database = schema["database"] as Map<*, *>

        val statements = mutableListOf<String>()
        for (entity in database["entities"] as List<*>) {
            entity as Map<*, *>
            val table = entity["tableName"] as String
            statements += (entity["createSql"] as String).replace("\${TABLE_NAME}", table)
            for (index in entity["indices"] as List<*>? ?: emptyList<Any>()) {
                statements += ((index as Map<*, *>)["createSql"] as String).replace("\${TABLE_NAME}", table)
            }
        }
        statements += (database["setupQueries"] as List<*>).map { it as String }

        context.deleteDatabase(dbName)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null).use { db ->
            statements.forEach { db.execSQL(it) }
            db.version = version
        }
    }
}

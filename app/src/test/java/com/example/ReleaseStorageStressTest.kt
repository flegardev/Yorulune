package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReleaseStorageStressTest {
    @Test fun fiveThousandTasksRetainIdsAndDescriptionsAfterReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "release-stress-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, OrbitLocalDatabase::class.java, name).build()
        var db = open()
        try {
            val rows = (0 until 5000).map { LocalTaskEntity(id = "fixture-$it",
                title = "Fictional task $it", description = "Fixture notes $it") }
            db.orbitDao().upsertTasks(rows)
            db.close(); db = open()
            val before = db.orbitDao().getAllTasksOnce()
            assertEquals(rows.associateBy { it.id }, before.associateBy { it.id })
            try {
                db.withTransaction {
                    db.orbitDao().deleteTaskById("fixture-0")
                    throw IllegalStateException("deliberate fixture rollback")
                }
                fail("Transaction must propagate the fixture failure")
            } catch (expected: IllegalStateException) {
                assertEquals("deliberate fixture rollback", expected.message)
            }
            assertEquals(before.associateBy { it.id }, db.orbitDao().getAllTasksOnce().associateBy { it.id })
        } finally { db.close(); context.deleteDatabase(name) }
    }
}

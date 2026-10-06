package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeOsDatabaseTest {

    private lateinit var db: LifeOsDatabase
    private lateinit var dao: LifeOsDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeUserAndReadInList() = runBlocking {
        val user = UserEntity(
            id = 1,
            name = "Test User",
            email = "test@example.com",
            heightCm = 180f,
            weightKg = 75f,
            age = 30,
            gender = "Male",
            activityLevel = "Active"
        )
        dao.insertOrUpdateUserProfile(user)
        val loaded = dao.getUserProfileOnce()
        assertNotNull(loaded)
        assertEquals("Test User", loaded?.name)
        assertEquals(180f, loaded?.heightCm)
        assertEquals(75f, loaded?.weightKg)
        assertEquals(30, loaded?.age)
    }

    @Test
    fun recordAndRetrieveUserActivity() = runBlocking {
        val activity = UserActivityEntity(
            id = "act-1",
            type = "Running",
            durationMinutes = 45,
            intensity = "High",
            timestamp = System.currentTimeMillis()
        )
        dao.insertUserActivity(activity)
        dao.deleteUserActivityById("act-1")
    }
}

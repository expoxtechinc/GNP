package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.NewsRepository
import com.example.ui.NewsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NewsRepository
    private lateinit var viewModel: NewsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NewsRepository(database.newsDao())
        viewModel = NewsViewModel(context as Application, repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDbAndViewModelInitSuccess() = runBlocking {
        // Since init block in ViewModel launches repository.seedDatabaseIfEmpty(),
        // let's verify that items got seeded correctly.
        val articles = viewModel.allArticles.first()
        val prefs = viewModel.allPreferences.first()
        
        // Assert that the database seed filled up articles and preferences
        assertNotNull(articles)
        assertNotNull(prefs)
        assertEquals(5, articles.size)
        assertEquals(6, prefs.size)
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Global News Pro", appName)
    }
}


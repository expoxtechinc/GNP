package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    // --- Articles DAO ---
    @Query("SELECT * FROM news_articles ORDER BY publishTime DESC")
    fun getAllArticles(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY publishTime DESC")
    fun getArticlesByCategory(category: String): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE isSaved = 1 ORDER BY publishTime DESC")
    fun getSavedArticles(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE id = :id")
    suspend fun getArticleById(id: Long): NewsArticle?

    @Query("SELECT * FROM news_articles WHERE id = :id")
    fun getArticleByIdFlow(id: Long): Flow<NewsArticle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: NewsArticle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Update
    suspend fun updateArticle(article: NewsArticle)

    @Delete
    suspend fun deleteArticle(article: NewsArticle)

    @Query("DELETE FROM news_articles WHERE id = :id")
    suspend fun deleteArticleById(id: Long)

    @Query("UPDATE news_articles SET views = views + 1 WHERE id = :id")
    suspend fun incrementViews(id: Long)

    // --- User Preferences DAO ---
    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<UserPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: List<UserPreference>)

    @Query("UPDATE user_preferences SET isSelected = :isSelected WHERE category = :category")
    suspend fun updatePreferenceSelection(category: String, isSelected: Boolean)

    @Query("UPDATE user_preferences SET notificationsEnabled = :enabled WHERE category = :category")
    suspend fun updatePreferenceNotification(category: String, enabled: Boolean)

    // --- Activity Logs / Real-time Analytics DAO ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}

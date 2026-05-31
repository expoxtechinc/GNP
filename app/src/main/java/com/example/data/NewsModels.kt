package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val embedCode: String? = null,
    val author: String,
    val publishTime: Long = System.currentTimeMillis(),
    val views: Int = 0,
    val readingTimeMinutes: Int = 3,
    val isSaved: Boolean = false,
    val isBreaking: Boolean = false
) : Serializable

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val category: String,
    val isSelected: Boolean = false,
    val notificationsEnabled: Boolean = true
) : Serializable

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleTitle: String,
    val category: String,
    val actionType: String, // "VIEW", "SAVE", "UNSAVE", "ALERT_TRIGGERED"
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

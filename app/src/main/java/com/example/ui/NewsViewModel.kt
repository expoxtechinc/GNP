package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NewsViewModel(
    application: Application,
    private val repository: NewsRepository
) : AndroidViewModel(application) {

    // Filter categories backing flows
    private val _selectedCategory = MutableStateFlow("All")
    var selectedCategory: String
        get() = _selectedCategory.value
        set(value) { _selectedCategory.value = value }

    // Selected article for reader detail screen
    var currentArticleState = mutableStateOf<NewsArticle?>(null)
        private set

    // Current in-app real-time personalized alert state
    var activeAlertStateByInterest = mutableStateOf<NewsArticle?>(null)
        private set

    // Admin login and authentication state
    var isAdminLoggedIn by mutableStateOf(false)
        private set
    var loginError by mutableStateOf("")

    // Admin publishing form states
    var pubTitle by mutableStateOf("")
    var pubCategory by mutableStateOf("World")
    var pubContent by mutableStateOf("")
    var pubImageUrl by mutableStateOf("")
    var pubVideoUrl by mutableStateOf("")
    var pubEmbedCode by mutableStateOf("")
    var pubIsBreaking by mutableStateOf(false)
    var isPublishingSuccess by mutableStateOf(false)

    // Flow states
    val allArticles: StateFlow<List<NewsArticle>> = repository.allArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedArticles: StateFlow<List<NewsArticle>> = repository.savedArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPreferences: StateFlow<List<UserPreference>> = repository.allPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> = repository.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered feed combined state
    val filteredArticles: StateFlow<List<NewsArticle>> = combine(allArticles, _selectedCategory) { articles, category ->
        if (category == "All") {
            articles
        } else {
            articles.filter { it.category == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics computation flows
    val totalViews: StateFlow<Int> = allArticles.map { articles ->
        articles.sumOf { it.views }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageReadingTime: StateFlow<Double> = allArticles.map { articles ->
        if (articles.isEmpty()) 0.0 else articles.map { it.readingTimeMinutes }.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryMetrics: StateFlow<Map<String, Int>> = allArticles.map { articles ->
        articles.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.views } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        // Seed initial items if database is blank
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    fun selectCategory(category: String) {
        selectedCategory = category
    }

    fun setArticle(article: NewsArticle?) {
        currentArticleState.value = article
        if (article != null) {
            viewModelScope.launch {
                repository.incrementViews(article.id)
            }
        }
    }

    fun toggleSaveArticle(article: NewsArticle) {
        viewModelScope.launch {
            repository.toggleSaved(article.id)
            // Sync current viewed article
            if (currentArticleState.value?.id == article.id) {
                val current = currentArticleState.value
                if (current != null) {
                    currentArticleState.value = current.copy(isSaved = !current.isSaved)
                }
            }
        }
    }

    fun updatePreference(category: String, isSelected: Boolean) {
        viewModelScope.launch {
            repository.updatePreferenceSelection(category, isSelected)
        }
    }

    fun updatePreferenceNotification(category: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferenceNotification(category, enabled)
        }
    }

    fun clearAlert() {
        activeAlertStateByInterest.value = null
    }

    // Gated admin login verification
    fun adminLogin(email: String, word: String): Boolean {
        if (email.trim().lowercase() == "aki.sokpah.link@gmail.com" && word == "Admin@2026") {
            isAdminLoggedIn = true
            loginError = ""
            viewModelScope.launch {
                repository.insertLog(ActivityLog(articleTitle = "Security Console", category = "Admin", actionType = "SECURE_LOGIN"))
            }
            return true
        } else {
            loginError = "Access Denied: Invalid credentials"
            return false
        }
    }

    fun adminLogout() {
        isAdminLoggedIn = false
    }

    // Admin News Publishing Manager
    fun publishCustomArticle() {
        if (pubTitle.isBlank() || pubContent.isBlank()) {
            return
        }

        viewModelScope.launch {
            val freshArticle = NewsArticle(
                title = pubTitle,
                content = pubContent,
                category = pubCategory,
                imageUrl = if (pubImageUrl.isNotBlank()) pubImageUrl.trim() else null,
                videoUrl = if (pubVideoUrl.isNotBlank()) pubVideoUrl.trim() else null,
                embedCode = if (pubEmbedCode.isNotBlank()) pubEmbedCode.trim() else null,
                author = "Chief Editor (Admin)",
                views = 1,
                readingTimeMinutes = (pubContent.length / 450).coerceAtLeast(1),
                isBreaking = pubIsBreaking
            )
            repository.insertArticle(freshArticle)
            repository.insertLog(ActivityLog(articleTitle = freshArticle.title, category = freshArticle.category, actionType = "PUBLISH"))

            // Trigger real-time personalized alert if this category is liked by user!
            val preferences = repository.allPreferences.first()
            val categoryPref = preferences.find { it.category == freshArticle.category }
            if (categoryPref != null && categoryPref.isSelected && categoryPref.notificationsEnabled) {
                activeAlertStateByInterest.value = freshArticle
                repository.insertLog(ActivityLog(articleTitle = freshArticle.title, category = freshArticle.category, actionType = "ALERT_TRIGGERED"))
            }

            // Clear publishing form inputs
            pubTitle = ""
            pubContent = ""
            pubImageUrl = ""
            pubVideoUrl = ""
            pubEmbedCode = ""
            pubIsBreaking = false
            isPublishingSuccess = true
        }
    }

    fun deleteArticle(id: Long) {
        viewModelScope.launch {
            val article = repository.getArticleById(id)
            if (article != null) {
                repository.insertLog(ActivityLog(articleTitle = article.title, category = article.category, actionType = "DELETE"))
                repository.deleteArticleById(id)
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}

// Custom viewmodel provider factory to initialize database singletons safely
class NewsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = AppDatabase.getDatabase(application)
        val repository = NewsRepository(database.newsDao())
        return NewsViewModel(application, repository) as T
    }
}

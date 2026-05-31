package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.Serializable

class NewsRepository(private val newsDao: NewsDao) {

    val allArticles: Flow<List<NewsArticle>> = newsDao.getAllArticles()
    val savedArticles: Flow<List<NewsArticle>> = newsDao.getSavedArticles()
    val allPreferences: Flow<List<UserPreference>> = newsDao.getAllPreferences()
    val activityLogs: Flow<List<ActivityLog>> = newsDao.getAllLogs()

    fun getArticlesByCategory(category: String): Flow<List<NewsArticle>> {
        return newsDao.getArticlesByCategory(category)
    }

    fun getArticleByIdFlow(id: Long): Flow<NewsArticle?> {
        return newsDao.getArticleByIdFlow(id)
    }

    suspend fun getArticleById(id: Long): NewsArticle? {
        return newsDao.getArticleById(id)
    }

    suspend fun insertArticle(article: NewsArticle): Long {
        return newsDao.insertArticle(article)
    }

    suspend fun deleteArticleById(id: Long) {
        newsDao.deleteArticleById(id)
    }

    suspend fun toggleSaved(id: Long) {
        val article = newsDao.getArticleById(id)
        if (article != null) {
            val updated = article.copy(isSaved = !article.isSaved)
            newsDao.updateArticle(updated)
            
            // Log this action for real-time analytics
            val action = if (updated.isSaved) "SAVE" else "UNSAVE"
            insertLog(ActivityLog(articleTitle = updated.title, category = updated.category, actionType = action))
        }
    }

    suspend fun incrementViews(id: Long) {
        val article = newsDao.getArticleById(id)
        if (article != null) {
            newsDao.incrementViews(id)
            insertLog(ActivityLog(articleTitle = article.title, category = article.category, actionType = "VIEW"))
        }
    }

    suspend fun updatePreferenceSelection(category: String, isSelected: Boolean) {
        newsDao.updatePreferenceSelection(category, isSelected)
    }

    suspend fun updatePreferenceNotification(category: String, enabled: Boolean) {
        newsDao.updatePreferenceNotification(category, enabled)
    }

    suspend fun insertLog(log: ActivityLog) {
        newsDao.insertLog(log)
    }

    suspend fun clearLogs() {
        newsDao.clearLogs()
    }

    suspend fun seedDatabaseIfEmpty() {
        // Checking if we already have preferences
        val existingPreferences = allPreferences.first()
        if (existingPreferences.isEmpty()) {
            val defaultCategories = listOf(
                UserPreference("World", isSelected = true, notificationsEnabled = true),
                UserPreference("Technology", isSelected = true, notificationsEnabled = true),
                UserPreference("Science", isSelected = false, notificationsEnabled = true),
                UserPreference("Business", isSelected = true, notificationsEnabled = false),
                UserPreference("Sports", isSelected = false, notificationsEnabled = false),
                UserPreference("Entertainment", isSelected = false, notificationsEnabled = false)
            )
            newsDao.insertPreferences(defaultCategories)
        }

        // Checking if we have articles
        val existingArticles = allArticles.first()
        if (existingArticles.isEmpty()) {
            val seedArticles = listOf(
                NewsArticle(
                    title = "Global Climate Accord Reached: Historic 190-Nation Carbon Peak Scheduled for 2030",
                    content = """GENEVA — In an unprecedented displays of diplomatic unity, leaders representing 190 nations have finalized the "Geneva Climate Treaty of 2026", legally committing signatory nations to reach peak greenhouse gas emissions no later than December 2030. 
                        
The treaty, which builds upon previous framework revisions, imposes strictly audited caps on heavy industrial production hubs while providing trillions of dollars in clean infrastructure credits for transition-state economies.

U.N. Environment Director Sofia Vance characterized the summit's final consensus as a "complete paradigm shift" in ecological diplomacy. "For the first time in history, compliance is backed by carbon-offset trade credits rather than voluntary guidelines," Vance emphasized.

Under the framework:
• Advanced nations will implement complete coal-phaseouts by 2030.
• Developing nations are granted financial backing for localized solar and wind initiatives.
• Heavy manufacturers face a uniform global carbon levy starting in 2027.

While industrial coalitions have raised short-term inflation alarms, economists predict that the influx of green capital will generate over 45 million high-tech engineering jobs globally before the end of the decade.""",
                    category = "World",
                    imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=600",
                    videoUrl = "https://www.youtube.com/watch?v=9X68C-UnV6U",
                    embedCode = "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/9X68C-UnV6U\" frameborder=\"0\" allowfullscreen></iframe>",
                    author = "Elena Rostova",
                    views = 1240,
                    readingTimeMinutes = 5,
                    isBreaking = true
                ),
                NewsArticle(
                    title = "Cognitive Frontier: Neural-Interface Synapse Processor Cleared for Clinical Trials",
                    content = """SILICON VALLEY — NeuralEngine Inc. has received formal FDA approval to initiate its first human clinical trial for the "Synapse-X" processor, an ultra-low latency, bio-compatible neural implant designed to bypass broken neurological pathways.

Unlike previous attempts at neural bridge devices, the Synapse-X leverages flexible organic micro-transistors that mold to neural nodes, utilizing a generative pre-trained biological interpreter (GPI) to translate sensory intentions into real-time digital commands.

"We aren't just reading signals; we are completing the loops," said Dr. Arthur Pendelton, lead research scientist. "This marks a massive milestone toward restoring motor sovereignty to individuals suffering from persistent spinal trauma or neurological degenerative state."

The initial trial phase will register nine candidates across three research clinics in the United States and is scheduled to begin surgical implants next month.""",
                    category = "Technology",
                    imageUrl = "https://images.unsplash.com/photo-1677442136019-21780efad99a?q=80&w=600",
                    videoUrl = null,
                    author = "Marcus Vance",
                    views = 3105,
                    readingTimeMinutes = 4,
                    isBreaking = false
                ),
                NewsArticle(
                    title = "James Webb Explores 'Echo Epoch': Earliest Cosmic Structures Identified Near Time Inception",
                    content = """BALTIMORE — Astrophysics teams analyzing deep-field infrared data returned by the James Webb Space Telescope have mapped what they refer to as the "Echo Epoch"—a collection of supermassive protogalaxies existing less than 210 million years after the Big Bang.

These colossal gaseous clusters exhibit structures far heavier and denser than previous models predicted, challenging classical theories regarding standard primordial helium-hydrogen compression cycles.

"The presence of highly ordered, dense stellar systems so early in cosmic history suggests that cosmic dust condensation was either highly accelerated or initiated by non-baryonic matter gradients," stated Dr. Alistair Chen of the Space Telescope Science Institute.

Researchers plan to focus James Webb's localized primary mirrors on the core cluster designated J12-Echo for 120 continuous hours of exposure next autumn to refine composition graphs.""",
                    category = "Science",
                    imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=600",
                    videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    author = "Jean-Pierre Dupont",
                    views = 982,
                    readingTimeMinutes = 3,
                    isBreaking = false
                ),
                NewsArticle(
                    title = "Sovereign Debt Pivot: Central Banks Coordinate Multi-National Rate Adjustments Amid Inflation Retreat",
                    content = """LONDON — A coordinated action plan involving five major central banks resulted in synchronous 50-basis-point interest reductions this morning, confirming that global monetary policy has finally pivoted to a stimulation trajectory.

The Federal Reserve, European Central Bank, and Bank of England issued a joint statement citing consistent structural cooling of core energy markets and stabilized agricultural chains.

"Inflation metrics have nested safely within target projections of 2% for three consecutive quarters," declared central bank representative Charles Woodhouse. "Our primary focus now pivots toward ensuring capital liquidity for structural housing and green manufacturing."

Global indexes rallied immediately on the news, with the FTSE 100 and S&P 500 recording historical intraday highs within minutes of opening bells.""",
                    category = "Business",
                    imageUrl = "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=600",
                    videoUrl = null,
                    author = "Amara Kante",
                    views = 1530,
                    readingTimeMinutes = 4,
                    isBreaking = false
                ),
                NewsArticle(
                    title = "Gold Medal Tally: Emerging Champions Crowned at the International Athletics Gala",
                    content = """PARIS — The World Athletics League concluded its season finals with dramatic upsets on the track. Underdog sprinter Kenji Tanaka secured a stunning gold in the men's 100m, clocking a lifetime personal best of 9.76 seconds.

Meanwhile, in the women's high jump, Olympic veteran Clara Dubois cleared 2.08m on her final attempt to secure gold, establishing a brand-new stadium record.

"I trusted my cadence, remained patient in transition, and powered through the final 30 meters," an emotional Tanaka told press pools afterward.

The events drew a sold-out crowd of over 85,000 sports enthusiasts, validating the league's global reach and growing resonance with digital audiences.""",
                    category = "Sports",
                    imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=600",
                    videoUrl = null,
                    author = "Robert Sterling",
                    views = 762,
                    readingTimeMinutes = 2,
                    isBreaking = false
                )
            )
            newsDao.insertArticles(seedArticles)
        }
    }
}

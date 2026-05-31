package com.example.ui

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ActivityLog
import com.example.data.NewsArticle
import com.example.data.UserPreference
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AlertPulseRed
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NewsAppMainScreen(viewModel: NewsViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("Feed") }
    var showOnboarding by remember { mutableStateOf(false) }

    // Observe state flows
    val preferences by viewModel.allPreferences.collectAsState()
    val activeAlert by viewModel.activeAlertStateByInterest

    // Set onboarding visibility based on whether any category is chosen
    LaunchedEffect(preferences) {
        if (preferences.isNotEmpty() && !preferences.any { it.isSelected }) {
            showOnboarding = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showOnboarding) {
            OnboardingScreen(
                viewModel = viewModel,
                preferences = preferences,
                onDismiss = { showOnboarding = false }
            )
        } else {
            val currentArticle by viewModel.currentArticleState

            // If an article is selected, view DetailScreen; otherwise show standard tabs
            if (currentArticle != null) {
                DetailScreen(
                    article = currentArticle!!,
                    onBack = { viewModel.setArticle(null) },
                    onSave = { viewModel.toggleSaveArticle(it) }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            containerColor = Color(0xFF0F0F0F), // Sleek Sophisticated Dark footer (#0F0F0F)
                            tonalElevation = 8.dp
                        ) {
                            val navColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary, // Brand Red
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = Color.Transparent // Clean, flush look that mimics HTML active state
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Feed") },
                                label = { Text("Feed", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                                selected = currentTab == "Feed",
                                onClick = { currentTab = "Feed" },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_feed")
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Notifications, contentDescription = "Alerts") },
                                label = { Text("Alerts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                                selected = currentTab == "Alerts",
                                onClick = { currentTab = "Alerts" },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_alerts")
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Bookmarks") },
                                label = { Text("Saved", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                                selected = currentTab == "Saved",
                                onClick = { currentTab = "Saved" },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_saved")
                            )
                            NavigationBarItem(
                                icon = { 
                                    Box {
                                        Icon(Icons.Filled.Person, contentDescription = "Admin")
                                        // A small elegant red dot over admin just like in the HTML design dashboard mockup
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .align(Alignment.TopEnd)
                                                .offset(x = 1.dp, y = (-1).dp)
                                        )
                                    }
                                },
                                label = { Text("Admin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                                selected = currentTab == "Admin",
                                onClick = { currentTab = "Admin" },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_admin")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (currentTab) {
                            "Feed" -> FeedScreen(viewModel = viewModel)
                            "Alerts" -> AlertsConfigurationScreen(viewModel = viewModel, preferences = preferences)
                            "Saved" -> SavedArticlesScreen(viewModel = viewModel)
                            "Admin" -> AdminPortalScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // --- Simulated In-App Personalized Alert Sliding Banner ---
        AnimatedVisibility(
            visible = activeAlert != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            activeAlert?.let { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            viewModel.setArticle(alert)
                            viewModel.clearAlert()
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AlertPulseRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "REAL-TIME ALERT",
                                    color = AlertPulseRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearAlert() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New Article matching your [${alert.category}] interest:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to read full ad-free article →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. ONBOARDING / PERSONALIZATION SELECTION
// ==========================================
@Composable
fun OnboardingScreen(
    viewModel: NewsViewModel,
    preferences: List<UserPreference>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            // Iconic Red BBC-like block logo
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "G L O B A L",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Personalize Your Feed",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select your target news categories. We will customize your feed to remain completely ad-free and provide optional real-time interest alerts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Interest grid list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(preferences) { pref ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (pref.isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updatePreference(pref.category, !pref.isSelected)
                            },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (pref.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (pref.category) {
                                        "World" -> Icons.Filled.Home
                                        "Technology" -> Icons.Filled.Settings
                                        "Science" -> Icons.Filled.Star
                                        "Business" -> Icons.Filled.Info
                                        "Sports" -> Icons.Filled.Star
                                        else -> Icons.Filled.PlayArrow
                                    },
                                    contentDescription = pref.category,
                                    tint = if (pref.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = pref.category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pref.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Checkbox(
                                checked = pref.isSelected,
                                onCheckedChange = { viewModel.updatePreference(pref.category, it) }
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("onboarding_continue"),
            shape = RoundedCornerShape(12.dp),
            enabled = preferences.any { it.isSelected },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                "Build My News Desk",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

// ==========================================
// 2. MAIN NEWS FEED (with category sliding tabs)
// ==========================================
@Composable
fun FeedScreen(viewModel: NewsViewModel) {
    val filteredArticles by viewModel.filteredArticles.collectAsState()
    val preferences by viewModel.allPreferences.collectAsState()
    val categories = listOf("All") + preferences.map { it.category }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar & Brand header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background) // Seamless, transparent-to-bg blend
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Editorial brand block
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD32F2F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "GNP Emblem",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Global News Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }

                Badge(
                    containerColor = Color(0xFFD32F2F), // Use matching Sophisticated Brand Red
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        "PRO LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Sliding category row - borderless, floating directly on deep dark background
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 12.dp, top = 2.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = viewModel.selectedCategory == category
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.1f))
                        .clickable { viewModel.selectCategory(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Articles Feed
        if (filteredArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Empty",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No articles in this section",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Admin publishers can load new stories directly from the Admin console tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Layout: Breaking News Hero banner first if present
                val breakingNews = filteredArticles.firstOrNull { it.isBreaking }
                if (breakingNews != null) {
                    item {
                        BreakingNewsHeroCard(article = breakingNews, onClick = { viewModel.setArticle(it) })
                    }
                }

                // Regular Feed items
                val regularFeed = if (breakingNews != null) filteredArticles.filter { it.id != breakingNews.id } else filteredArticles
                items(regularFeed) { article ->
                    FeedNewsItemRow(article = article, onClick = { viewModel.setArticle(it) })
                }
            }
        }
    }
}

@Composable
fun BreakingNewsHeroCard(article: NewsArticle, onClick: (NewsArticle) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick(article) }
            .testTag("breaking_hero"),
        shape = RoundedCornerShape(24.dp), // Styled after tailwind's rounded-3xl
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f) // Exact 16/10 aspect ratio from HTML mockup
        ) {
            // Background Image or fallback gradient
            if (article.imageUrl != null) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE2B93C), Color(0xFFD32F2F))
                            )
                        )
                )
            }

            // Dark premium gradient scrim to ensure extreme text readability on images
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // LIVE UPDATE Badge (Top-Left)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color(0xFFD32F2F), RoundedCornerShape(50.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Live Update icon",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "LIVE UPDATE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            // Category tag (Top-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    article.category.uppercase(),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
            }

            // Editorial headlines & publisher metadata (Bottom Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "BREAKING REPORT",
                    color = Color(0xFFD32F2F),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "BY ${article.author.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                    Text(
                        text = "${article.readingTimeMinutes} MINS READ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FeedNewsItemRow(article: NewsArticle, onClick: (NewsArticle) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)) // rounded-2xl
            .background(Color(0x0DFFFFFF)) // bg-white/5 Translucent Glass
            .border(width = 1.dp, color = Color(0x0DFFFFFF), shape = RoundedCornerShape(16.dp)) // border-white/5
            .clickable { onClick(article) }
            .padding(14.dp)
            .testTag("news_item_row_${article.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.category.uppercase(),
                        color = Color(0xFFD32F2F), // Brand Red
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.75.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${article.readingTimeMinutes} MIN READ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyLarge, // Clean humanist sans title
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BY ${article.author.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Read count",
                            modifier = Modifier.size(11.dp),
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${article.views}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            
            if (article.imageUrl != null) {
                Spacer(modifier = Modifier.width(16.dp))
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
                // Beautiful placeholder matching the play circle / article icon layout in the HTML spec
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Playback Icon Placeholder",
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. REVOLUTIONARY AD-FREE IMMERSIVE READER
// ==========================================
@Composable
fun DetailScreen(
    article: NewsArticle,
    onBack: () -> Unit,
    onSave: (NewsArticle) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Set up Text-to-Speech Engine
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        try {
            tts = TextToSpeech(context) { status ->
                if (status != TextToSpeech.ERROR) {
                    try {
                        tts?.language = Locale.ENGLISH
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tts = null
        }
        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // TTS Speak trigger
                        IconButton(
                            onClick = {
                                try {
                                    if (isSpeaking) {
                                        tts?.stop()
                                        isSpeaking = false
                                    } else {
                                        val textToRead = "${article.title}. Published by ${article.author}. Content: ${article.content}"
                                        tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "news_speech")
                                        isSpeaking = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isSpeaking = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.Close else Icons.Filled.PlayArrow,
                                contentDescription = "Listen to News Article",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { onSave(article) },
                            modifier = Modifier.testTag("detail_save")
                        ) {
                            Icon(
                                imageVector = if (article.isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Bookmark story",
                                tint = if (article.isSaved) AlertPulseRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Media Visual Header
            if (article.imageUrl != null) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Category Tag Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            article.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Badge(
                        containerColor = SuccessGreen.copy(alpha = 0.15f),
                        contentColor = SuccessGreen
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Verified Clean Reading", modifier = Modifier.size(12.dp), tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AD-FREE GUARANTEE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bylines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Reported by ${article.author}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Published " + SimpleDateFormat("MMM d, yyyy · H:mm", Locale.getDefault()).format(Date(article.publishTime)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Create,
                            contentDescription = "Journalist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Audio player HUD if speaker active
                if (isSpeaking) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Audio Playback", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Narrating Editorial Article...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Engine uses advanced text-to-speech rendering",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    tts?.stop()
                                    isSpeaking = false
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("MUTE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Core content body (Serif typography for elegant newspaper read)
                Text(
                    text = article.content,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 28.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // --- MULTIMEDIA RICH MODULES (Video Links / Embeds) ---
                if (article.videoUrl != null || article.embedCode != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Related Media Coverage",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enjoy interactive media linked to this report safely and without any trackers or advertisements.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Embedded Video Card Container (Provides real-time interactive click out)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Interactive Media Stream",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Watch Article Video Stream",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A professional video companion channel is active for this story.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (article.embedCode != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            "Embedded HTML Component Code:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = article.embedCode,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val targetUrl = article.videoUrl ?: "https://www.youtube.com/watch?v=9X68C-UnV6U"
                                    uriHandler.openUri(targetUrl)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play Stream")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LAUNCH FULL MULTIMEDIA STREAM", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// ==========================================
// 4. PERSONALIZE ALERTS CONFIGURATION
// ==========================================
@Composable
fun AlertsConfigurationScreen(viewModel: NewsViewModel, preferences: List<UserPreference>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Alerts",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Personalized News Alerts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
        Text(
            text = "Configure interest channels that will spawn real-time alerts on your device. We prioritize completely silent background updates to prevent battery drainage.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, start = 4.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(preferences) { pref ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pref.isSelected)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (pref.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (pref.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (pref.category) {
                                            "World" -> Icons.Filled.Home
                                            "Technology" -> Icons.Filled.Settings
                                            "Science" -> Icons.Filled.Star
                                            "Business" -> Icons.Filled.Info
                                            "Sports" -> Icons.Filled.Star
                                            else -> Icons.Filled.PlayArrow
                                        },
                                        contentDescription = pref.category,
                                        tint = if (pref.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = pref.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (pref.isSelected) "Subscribed to Desk Updates" else "Not Subscribed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (pref.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = pref.isSelected,
                                onCheckedChange = { viewModel.updatePreference(pref.category, it) }
                            )
                        }

                        if (pref.isSelected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Notifications,
                                        contentDescription = "Notifications active",
                                        tint = if (pref.notificationsEnabled) AlertPulseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "In-App Notification Banner",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Switch(
                                    checked = pref.notificationsEnabled,
                                    onCheckedChange = { viewModel.updatePreferenceNotification(pref.category, it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. OFFLINE SAVED BOOKMARKS HUB
// ==========================================
@Composable
fun SavedArticlesScreen(viewModel: NewsViewModel) {
    val savedArticles by viewModel.savedArticles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Saved Library",
                tint = AlertPulseRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Saved Desk",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
        Text(
            text = "Your bookmarks are serialized instantly into the local Room database to grant flawless real-time accessibility even during absolute offline network states.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, start = 4.dp)
        )

        if (savedArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            contentDescription = "Empty",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your library is blank",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bookmark premium news columns. They will appear here immediately and can be read completely ad-free and tracking-free.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedArticles) { article ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setArticle(article) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (article.imageUrl != null) {
                                AsyncImage(
                                    model = article.imageUrl,
                                    contentDescription = article.title,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = article.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.toggleSaveArticle(article) }) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Remove", tint = AlertPulseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 6. GATED SECURITY PORTAL & REAL-TIME ANALYTICS DASHBOARD
// =========================================================
@Composable
fun AdminPortalScreen(viewModel: NewsViewModel) {
    if (viewModel.isAdminLoggedIn) {
        AdminDashboardScreen(viewModel = viewModel)
    } else {
        AdminLoginScreen(viewModel = viewModel)
    }
}

@Composable
fun AdminLoginScreen(viewModel: NewsViewModel) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .testTag("admin_login_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "🔒 Gated console",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Security Console",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gaining entry requires official admin credentials. System actions are strictly monitored in logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (viewModel.loginError.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = viewModel.loginError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Admin Email Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_input"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Console Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboardOptions,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input"),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.adminLogin(emailInput, passwordInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_submit_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("AUTHORIZE ENTRY", fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(viewModel: NewsViewModel) {
    val totalViews by viewModel.totalViews.collectAsState()
    val avgReadingTime by viewModel.averageReadingTime.collectAsState()
    val articles by viewModel.allArticles.collectAsState()
    val categoryMetrics by viewModel.categoryMetrics.collectAsState()
    val logs by viewModel.activityLogs.collectAsState()

    var dashboardTab by remember { mutableStateOf("Analytics") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Admin Navigation / Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ADMIN ACTIVE SESSION",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Global Console",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }

                Button(
                    onClick = { viewModel.adminLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("EXIT SESSION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        TabRow(
            selectedTabIndex = if (dashboardTab == "Analytics") 0 else if (dashboardTab == "Publish") 1 else 2,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = dashboardTab == "Analytics",
                onClick = { dashboardTab = "Analytics" },
                text = { Text("Reader Analytics") },
                modifier = Modifier.testTag("admin_tab_analytics")
            )
            Tab(
                selected = dashboardTab == "Publish",
                onClick = { dashboardTab = "Publish" },
                text = { Text("Publish Story") },
                modifier = Modifier.testTag("admin_tab_publish")
            )
            Tab(
                selected = dashboardTab == "Manage",
                onClick = { dashboardTab = "Manage" },
                text = { Text("Database") },
                modifier = Modifier.testTag("admin_tab_manage")
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (dashboardTab) {
                "Analytics" -> AdminAnalyticsTab(totalViews, avgReadingTime, categoryMetrics, logs, viewModel)
                "Publish" -> AdminPublishTab(viewModel)
                "Manage" -> AdminManageTab(articles, viewModel)
            }
        }
    }
}

@Composable
fun AdminAnalyticsTab(
    totalViews: Int,
    avgReadingTime: Double,
    categoryMetrics: Map<String, Int>,
    logs: List<ActivityLog>,
    viewModel: NewsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Core Statistics Deck
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = "Views", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Total Views", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = String.format("%,d", totalViews),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text("Live active logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Read Time", tint = AccentGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reading Pace", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = String.format("%.1f min", avgReadingTime),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text("Average read time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas Analytics Graphic (views by category)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Views Distribution by Interest Channel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (categoryMetrics.isEmpty()) {
                    Text(
                        "No statistics available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val maxVal = categoryMetrics.values.maxOrNull()?.toFloat() ?: 1f
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categoryMetrics.forEach { (category, value) ->
                            val percent = (value.toFloat() / maxVal).coerceIn(0.05f, 1f)
                            
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "$value views", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Custom Canvas painted bar chart
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                ) {
                                    val barWidth = size.width * percent
                                    // Base rail
                                    drawRoundRect(
                                        color = Color.LightGray.copy(alpha = 0.2f),
                                        size = size,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                    )
                                    // Highlight rail represent views count
                                    drawRoundRect(
                                        color = if (category == "World") Color(0xFFC8102E) else if (category == "Technology") Color(0xFF1E293B) else Color(0xFFE2B93C),
                                        size = Size(barWidth, size.height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chronological Monitoring Logs list
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time Activity Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    TextButton(onClick = { viewModel.clearAllLogs() }) {
                        Text("CLEAR LOGS", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waiting for reader actions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (log.actionType == "PUBLISH") SuccessGreen
                                            else if (log.actionType == "SECURE_LOGIN") Color.Blue
                                            else if (log.actionType == "ALERT_TRIGGERED") AlertPulseRed
                                            else MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${log.actionType}: ${log.articleTitle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = SimpleDateFormat("H:mm:ss", Locale.getDefault()).format(Date(log.timestamp)) + " · ${log.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPublishTab(viewModel: NewsViewModel) {
    val categories = listOf("World", "Technology", "Science", "Business", "Sports", "Entertainment")
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Publish Premium Editorial Column",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        if (viewModel.isPublishingSuccess) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Article Successfully Saved & Broadcasted!", fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Text(
                            "Subscribers with active alert settings have triggered real-time notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Auto close success message state after prompt
            LaunchedEffect(viewModel.isPublishingSuccess) {
                delay(4000)
                viewModel.isPublishingSuccess = false
            }
        }

        OutlinedTextField(
            value = viewModel.pubTitle,
            onValueChange = { viewModel.pubTitle = it },
            label = { Text("Article Title") },
            modifier = Modifier.fillMaxWidth().testTag("pub_title_input"),
            shape = RoundedCornerShape(8.dp)
        )

        // Dropdown Selection visual simulating category selection
        Column {
            Text("Interest Channel Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(3).forEach { cat ->
                    FilterChip(
                        selected = viewModel.pubCategory == cat,
                        onClick = { viewModel.pubCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.drop(3).forEach { cat ->
                    FilterChip(
                        selected = viewModel.pubCategory == cat,
                        onClick = { viewModel.pubCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.pubContent,
            onValueChange = { viewModel.pubContent = it },
            label = { Text("Core Article Written Content (Plaintext/Narrative)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("pub_content_input"),
            shape = RoundedCornerShape(8.dp),
            maxLines = 15
        )

        OutlinedTextField(
            value = viewModel.pubImageUrl,
            onValueChange = { viewModel.pubImageUrl = it },
            label = { Text("Image URL link (Unsplash or hosted media)") },
            placeholder = { Text("https://images.unsplash.com/photo-...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.pubVideoUrl,
                onValueChange = { viewModel.pubVideoUrl = it },
                label = { Text("Video URL Link") },
                placeholder = { Text("e.g. YouTube stream link") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = viewModel.pubEmbedCode,
                onValueChange = { viewModel.pubEmbedCode = it },
                label = { Text("Embed Code / iframe") },
                placeholder = { Text("<iframe... allow></iframe>") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(AlertPulseRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Highlight as Breaking News", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Spawns a massive visual hero banner at the very top of Feed", style = MaterialTheme.typography.labelSmall)
                }
            }
            Switch(
                checked = viewModel.pubIsBreaking,
                onCheckedChange = { viewModel.pubIsBreaking = it }
            )
        }

        Button(
            onClick = {
                if (viewModel.pubTitle.isBlank() || viewModel.pubContent.isBlank()) {
                    Toast.makeText(context, "Title and content cannot be blank", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.publishCustomArticle()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("admin_publish_submit"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Publish", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("COMMIT & DEPLOY TO READERS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun AdminManageTab(articles: List<NewsArticle>, viewModel: NewsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Aggregated Database Records",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Delete or purge outdated mock news reports instantly from the local SQLite database container.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(articles) { article ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = article.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (article.isBreaking) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(AlertPulseRed, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("BREAKING", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Reads: ${article.views} · Author: ${article.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteArticle(article.id) },
                            modifier = Modifier.testTag("admin_delete_article_${article.id}")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

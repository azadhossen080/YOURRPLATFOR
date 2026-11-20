package com.example.model

data class AppSettings(
    val isDarkMode: Boolean = false,
    val isDimmingEnabled: Boolean = true,
    val dimmingLevel: Float = 30f,
    val isLocationAllowed: Boolean = true,
    val selectedLanguage: String = "বাংলা (Bangladesh)",
    val selectedLanguageCode: String = "bn_BD"
)

data class NotificationModel(
    val id: String,
    val title: String,
    val timeAgo: String,
    val iconType: String = "like", // "like", "comment", "welcome"
    val isRead: Boolean = false
)

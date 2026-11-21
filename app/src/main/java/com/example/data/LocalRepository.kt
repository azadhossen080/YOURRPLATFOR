package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppSettings
import com.example.model.FriendStatus
import com.example.model.FriendUser
import com.example.model.NotificationModel
import com.example.model.PostModel
import com.example.model.ReviewModel
import com.example.model.ReviewStatus
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_POSTS = "posts_json"
        private const val KEY_SETTINGS = "settings_json"
        private const val KEY_PROFILE = "profile_json"
        private const val KEY_NOTIFICATIONS = "notifications_json"
        private const val KEY_FRIENDS = "friends_json"
        private const val KEY_REVIEWS = "reviews_json"
        private const val KEY_LAST_EXPORT_PATH = "last_export_path"
        private const val KEY_LAST_EXPORT_TIME = "last_export_time"
        const val COMMUNITY_TELEGRAM_URL = "https://t.me/sazadofficialbd"
    }

    fun loadPosts(): List<PostModel> {
        val raw = prefs.getString(KEY_POSTS, null)
        if (raw.isNullOrBlank()) {
            val defaults = getDefaultPosts()
            savePosts(defaults)
            return defaults
        }
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<PostModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val commentsArray = obj.optJSONArray("comments") ?: JSONArray()
                val commentsList = mutableListOf<String>()
                for (c in 0 until commentsArray.length()) {
                    commentsList.add(commentsArray.getString(c))
                }
                val loadedUserName = obj.getString("userName")
                // Migrate legacy default post author away from hardcoded personal name
                if (loadedUserName == "আজাদ হোসেন" && obj.optString("id") == "2") {
                    continue
                }
                val stickersArray = obj.optJSONArray("mojiStickers") ?: JSONArray()
                val stickersList = mutableListOf<String>()
                for (s in 0 until stickersArray.length()) {
                    stickersList.add(stickersArray.getString(s))
                }
                val tagsArray = obj.optJSONArray("tags") ?: JSONArray()
                val tagsList = mutableListOf<String>()
                for (t in 0 until tagsArray.length()) {
                    tagsList.add(tagsArray.getString(t))
                }
                val collabArray = obj.optJSONArray("collaborators") ?: JSONArray()
                val collabList = mutableListOf<String>()
                for (col in 0 until collabArray.length()) {
                    collabList.add(collabArray.getString(col))
                }
                list.add(
                    PostModel(
                        id = obj.getString("id"),
                        userName = loadedUserName,
                        timeAgo = obj.getString("timeAgo"),
                        postText = obj.getString("postText"),
                        mediaUri = if (obj.has("mediaUri") && !obj.isNull("mediaUri")) obj.getString("mediaUri") else null,
                        mediaType = obj.optString("mediaType", "text"),
                        thumbnailUri = if (obj.has("thumbnailUri") && !obj.isNull("thumbnailUri")) obj.getString("thumbnailUri") else null,
                        isVideo = obj.optBoolean("isVideo", false),
                        likeCount = obj.optInt("likeCount", 0),
                        isLiked = obj.optBoolean("isLiked", false),
                        comments = commentsList,
                        isUserPost = obj.optBoolean("isUserPost", false),
                        colorFilter = obj.optString("colorFilter", com.example.model.ColorFilterMode.NONE.id),
                        transitionEffect = obj.optString("transitionEffect", com.example.model.TransitionEffect.NONE.id),
                        mojiStickers = stickersList,
                        playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.5f, 2.0f),
                        isMuted = obj.optBoolean("isMuted", false),
                        trimStartFraction = obj.optDouble("trimStartFraction", 0.0).toFloat().coerceIn(0f, 0.95f),
                        trimEndFraction = obj.optDouble("trimEndFraction", 1.0).toFloat().coerceIn(0.05f, 1f),
                        isLiveStream = obj.optBoolean("isLiveStream", false),
                        postTitle = obj.optString("postTitle", ""),
                        soundName = obj.optString("soundName", "Original Audio"),
                        overlayText = obj.optString("overlayText", ""),
                        overlayColorHex = obj.optLong("overlayColorHex", 0xFFFFFFFF),
                        timerDurationSec = obj.optInt("timerDurationSec", 0),
                        locationName = obj.optString("locationName", ""),
                        playlistName = obj.optString("playlistName", ""),
                        tags = tagsList,
                        collaborators = collabList,
                        ageRestriction = obj.optString("ageRestriction", "All Ages"),
                        authorVerified = obj.optBoolean("authorVerified", true),
                        authorVip = obj.optBoolean("authorVip", false)
                    )
                )
            }
            if (list.isEmpty()) getDefaultPosts() else list
        } catch (e: Exception) {
            getDefaultPosts()
        }
    }

    fun savePosts(posts: List<PostModel>) {
        val array = JSONArray()
        for (p in posts) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("userName", p.userName)
            obj.put("timeAgo", p.timeAgo)
            obj.put("postText", p.postText)
            obj.put("mediaUri", p.mediaUri)
            obj.put("mediaType", p.mediaType)
            obj.put("thumbnailUri", p.thumbnailUri)
            obj.put("isVideo", p.isVideo)
            obj.put("likeCount", p.likeCount)
            obj.put("isLiked", p.isLiked)
            obj.put("isUserPost", p.isUserPost)
            obj.put("colorFilter", p.colorFilter)
            obj.put("transitionEffect", p.transitionEffect)
            obj.put("playbackSpeed", p.playbackSpeed.toDouble())
            obj.put("isMuted", p.isMuted)
            obj.put("trimStartFraction", p.trimStartFraction.toDouble())
            obj.put("trimEndFraction", p.trimEndFraction.toDouble())
            obj.put("isLiveStream", p.isLiveStream)
            obj.put("postTitle", p.postTitle)
            obj.put("soundName", p.soundName)
            obj.put("overlayText", p.overlayText)
            obj.put("overlayColorHex", p.overlayColorHex)
            obj.put("timerDurationSec", p.timerDurationSec)
            obj.put("locationName", p.locationName)
            obj.put("playlistName", p.playlistName)
            obj.put("ageRestriction", p.ageRestriction)
            obj.put("authorVerified", p.authorVerified)
            obj.put("authorVip", p.authorVip)
            val sArray = JSONArray()
            p.mojiStickers.forEach { sArray.put(it) }
            obj.put("mojiStickers", sArray)
            val tArray = JSONArray()
            p.tags.forEach { tArray.put(it) }
            obj.put("tags", tArray)
            val colArray = JSONArray()
            p.collaborators.forEach { colArray.put(it) }
            obj.put("collaborators", colArray)
            val cArray = JSONArray()
            p.comments.forEach { cArray.put(it) }
            obj.put("comments", cArray)
            array.put(obj)
        }
        prefs.edit().putString(KEY_POSTS, array.toString()).apply()
    }

    fun loadProfile(): UserProfile {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            val obj = JSONObject(raw)
            val rawName = obj.optString("name", "Guest User")
            val rawHandle = obj.optString("handle", "")
            // Migrate legacy hardcoded default profile to clean "0" followers & identity setup
            if (rawName == "আজাদ হোসেন" && rawHandle == "@azadhossen") {
                val migrated = UserProfile()
                saveProfile(migrated)
                return migrated
            }
            val rawUserId = obj.optString("userId", rawHandle.removePrefix("@"))
            val rawFollowers = obj.optString("followersCount", "0")
            val normalizedFollowers = if (rawFollowers == "১.২K") "0" else rawFollowers
            UserProfile(
                name = rawName,
                userId = rawUserId,
                handle = if (rawHandle.isNotBlank()) rawHandle else if (rawUserId.isNotBlank()) "@$rawUserId" else "",
                bio = obj.optString("bio", ""),
                email = obj.optString("email", ""),
                phoneNumber = obj.optString("phoneNumber", ""),
                authUid = obj.optString("authUid", ""),
                followersCount = normalizedFollowers,
                isProfileCreated = obj.optBoolean("isProfileCreated", rawUserId.isNotBlank() && rawName != "Guest User"),
                createdAtTimestamp = obj.optLong("createdAtTimestamp", 0L),
                coverColorHex = obj.optLong("coverColorHex", 0xFF3F51B5),
                coverImageUri = if (obj.has("coverImageUri") && !obj.isNull("coverImageUri")) obj.getString("coverImageUri") else null,
                avatarColorHex = obj.optLong("avatarColorHex", 0xFF5352ED),
                avatarImageUri = if (obj.has("avatarImageUri") && !obj.isNull("avatarImageUri")) obj.getString("avatarImageUri") else null,
                isVerified = obj.optBoolean("isVerified", true),
                isVip = obj.optBoolean("isVip", false)
            )
        } catch (e: Exception) {
            UserProfile()
        }
    }

    fun saveProfile(profile: UserProfile) {
        val obj = JSONObject()
        obj.put("name", profile.name)
        obj.put("userId", profile.userId)
        obj.put("handle", profile.handle)
        obj.put("bio", profile.bio)
        obj.put("email", profile.email)
        obj.put("phoneNumber", profile.phoneNumber)
        obj.put("authUid", profile.authUid)
        obj.put("followersCount", profile.followersCount)
        obj.put("isProfileCreated", profile.isProfileCreated)
        obj.put("createdAtTimestamp", profile.createdAtTimestamp)
        obj.put("coverColorHex", profile.coverColorHex)
        obj.put("coverImageUri", profile.coverImageUri)
        obj.put("avatarColorHex", profile.avatarColorHex)
        obj.put("avatarImageUri", profile.avatarImageUri)
        obj.put("isVerified", profile.isVerified)
        obj.put("isVip", profile.isVip)
        prefs.edit().putString(KEY_PROFILE, obj.toString()).apply()
    }

    fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return try {
            val obj = JSONObject(raw)
            AppSettings(
                isDarkMode = obj.optBoolean("isDarkMode", false),
                isDimmingEnabled = obj.optBoolean("isDimmingEnabled", true),
                dimmingLevel = obj.optDouble("dimmingLevel", 30.0).toFloat(),
                isLocationAllowed = obj.optBoolean("isLocationAllowed", true),
                selectedLanguage = obj.optString("selectedLanguage", "বাংলা (Bangladesh)"),
                selectedLanguageCode = obj.optString("selectedLanguageCode", "bn_BD")
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val obj = JSONObject()
        obj.put("isDarkMode", settings.isDarkMode)
        obj.put("isDimmingEnabled", settings.isDimmingEnabled)
        obj.put("dimmingLevel", settings.dimmingLevel.toDouble())
        obj.put("isLocationAllowed", settings.isLocationAllowed)
        obj.put("selectedLanguage", settings.selectedLanguage)
        obj.put("selectedLanguageCode", settings.selectedLanguageCode)
        prefs.edit().putString(KEY_SETTINGS, obj.toString()).apply()
    }

    fun loadNotifications(): List<NotificationModel> {
        val raw = prefs.getString(KEY_NOTIFICATIONS, null)
        if (raw.isNullOrBlank()) {
            val defaults = getDefaultNotifications()
            saveNotifications(defaults)
            return defaults
        }
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<NotificationModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NotificationModel(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        timeAgo = obj.getString("timeAgo"),
                        iconType = obj.optString("iconType", "like"),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            getDefaultNotifications()
        }
    }

    fun saveNotifications(notifications: List<NotificationModel>) {
        val array = JSONArray()
        for (n in notifications) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("timeAgo", n.timeAgo)
            obj.put("iconType", n.iconType)
            obj.put("isRead", n.isRead)
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
    }

    fun loadReviews(): List<ReviewModel> {
        val raw = prefs.getString(KEY_REVIEWS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<ReviewModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val statusName = obj.optString("status", ReviewStatus.APPROVED.name)
                val status = try {
                    ReviewStatus.valueOf(statusName)
                } catch (_: Exception) {
                    ReviewStatus.APPROVED
                }
                list.add(
                    ReviewModel(
                        id = obj.getString("id"),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", "User"),
                        rating = obj.optInt("rating", 5).coerceIn(1, 5),
                        comment = obj.optString("comment", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        timeAgo = obj.optString("timeAgo", ""),
                        status = status,
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReviews(reviews: List<ReviewModel>) {
        val array = JSONArray()
        for (r in reviews) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("userId", r.userId)
            obj.put("userName", r.userName)
            obj.put("rating", r.rating)
            obj.put("comment", r.comment)
            obj.put("timestamp", r.timestamp)
            obj.put("timeAgo", r.timeAgo)
            obj.put("status", r.status.name)
            obj.put("isPinned", r.isPinned)
            array.put(obj)
        }
        prefs.edit().putString(KEY_REVIEWS, array.toString()).apply()
    }

    fun buildCollectedUserDataJson(
        profile: UserProfile,
        posts: List<PostModel>,
        friends: List<FriendUser>,
        reviews: List<ReviewModel>,
        settings: AppSettings
    ): String {
        val now = System.currentTimeMillis()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val root = JSONObject()
        root.put("app", "YOURPLATFORM")
        root.put("storageMode", "Cloud & Local Sync")
        root.put("communityUrl", COMMUNITY_TELEGRAM_URL)
        root.put("collectedAtMillis", now)
        root.put("collectedAtIso", isoFormatter.format(Date(now)))

        val profileObj = JSONObject().apply {
            put("authUid", profile.authUid)
            put("userName", profile.name)
            put("userId", profile.displayUserId)
            put("handle", profile.handle)
            put("email", profile.email)
            put("phoneNumber", profile.phoneNumber)
            put("bio", profile.bio)
            put("followersCount", profile.followersCount)
            put("isProfileCreated", profile.isProfileCreated)
            put("createdAtTimestamp", profile.createdAtTimestamp)
        }
        root.put("userProfile", profileObj)

        val userPosts = posts.filter { it.isUserPost }
        val statsObj = JSONObject().apply {
            put("totalFeedPosts", posts.size)
            put("userPostsCount", userPosts.size)
            put("friendsCount", friends.count { it.status == FriendStatus.FRIEND })
            put("pendingRequestsCount", friends.count { it.status == FriendStatus.INCOMING_REQUEST })
            put("reviewsCount", reviews.size)
            put("approvedReviewsCount", reviews.count { it.status == ReviewStatus.APPROVED })
        }
        root.put("stats", statsObj)

        val reviewsArr = JSONArray()
        reviews.forEach { r ->
            reviewsArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("userId", r.userId)
                    put("userName", r.userName)
                    put("rating", r.rating)
                    put("comment", r.comment)
                    put("status", r.status.name)
                    put("isPinned", r.isPinned)
                    put("timestamp", r.timestamp)
                }
            )
        }
        root.put("reviews", reviewsArr)

        val postsArr = JSONArray()
        userPosts.forEach { p ->
            postsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("userName", p.userName)
                    put("postText", p.postText)
                    put("mediaType", p.mediaType)
                    put("isVideo", p.isVideo)
                    put("mediaUri", p.mediaUri)
                    put("likeCount", p.likeCount)
                    put("commentsCount", p.comments.size)
                }
            )
        }
        root.put("userPosts", postsArr)

        val settingsObj = JSONObject().apply {
            put("selectedLanguage", settings.selectedLanguage)
            put("selectedLanguageCode", settings.selectedLanguageCode)
            put("isDarkMode", settings.isDarkMode)
            put("isLocationAllowed", settings.isLocationAllowed)
        }
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    fun exportCollectedUserDataToFile(jsonContent: String, userId: String): File? {
        return try {
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val safeId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "user" }
            val outFile = File(exportDir, "yourplatform_data_${safeId}_${System.currentTimeMillis()}.json")
            outFile.writeText(jsonContent, Charsets.UTF_8)
            val now = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_LAST_EXPORT_PATH, outFile.absolutePath)
                .putLong(KEY_LAST_EXPORT_TIME, now)
                .apply()
            outFile
        } catch (_: Exception) {
            null
        }
    }

    fun getLastExportPath(): String? = prefs.getString(KEY_LAST_EXPORT_PATH, null)
    fun getLastExportTime(): Long? {
        val v = prefs.getLong(KEY_LAST_EXPORT_TIME, -1L)
        return if (v > 0L) v else null
    }

    private fun getDefaultPosts(): List<PostModel> {
        return listOf(
            PostModel(
                id = "1",
                userName = "YOURPLATFORM Official",
                timeAgo = "২ মিনিট আগে",
                postText = "YOURPLATFORM-এ আপনাকে স্বাগতম! আপনার User Name এবং User ID তৈরি করে পোস্ট, রিলস ও লাইভ শেয়ার শুরু করুন।",
                mediaType = "gradient",
                likeCount = 18,
                comments = listOf("দারুণ অভিজ্ঞতা!"),
                isLiked = false,
                isUserPost = false,
                authorVerified = true,
                authorVip = true
            )
        )
    }

    private fun getDefaultNotifications(): List<NotificationModel> {
        return listOf(
            NotificationModel(
                id = "1",
                title = "YOURPLATFORM Official আপনার পোস্টে লাইক দিয়েছেন।",
                timeAgo = "১০ মিনিট আগে",
                iconType = "like"
            ),
            NotificationModel(
                id = "2",
                title = "Tech World আপনার পোস্টে কমেন্ট করেছেন।",
                timeAgo = "১ ঘণ্টা আগে",
                iconType = "comment"
            ),
            NotificationModel(
                id = "3",
                title = "YOURPLATFORM এর নতুন Waves রিলস ফিচার ব্যবহার করে দেখুন!",
                timeAgo = "৩ ঘণ্টা আগে",
                iconType = "welcome"
            )
        )
    }

    fun loadFriends(): List<FriendUser> {
        val raw = prefs.getString(KEY_FRIENDS, null)
        if (raw.isNullOrBlank()) {
            val defaults = getDefaultFriends()
            saveFriends(defaults)
            return defaults
        }
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<FriendUser>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val statusName = obj.optString("status", FriendStatus.SUGGESTION.name)
                val status = try {
                    FriendStatus.valueOf(statusName)
                } catch (_: Exception) {
                    FriendStatus.SUGGESTION
                }
                list.add(
                    FriendUser(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        handle = obj.getString("handle"),
                        bio = obj.optString("bio", ""),
                        mutualFriendsCount = obj.optInt("mutualFriendsCount", 0),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF5352ED),
                        isVerified = obj.optBoolean("isVerified", false),
                        isOnline = obj.optBoolean("isOnline", false),
                        status = status,
                        requestTimeAgo = obj.optString("requestTimeAgo", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            getDefaultFriends()
        }
    }

    fun saveFriends(friends: List<FriendUser>) {
        val array = JSONArray()
        for (f in friends) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("handle", f.handle)
            obj.put("bio", f.bio)
            obj.put("mutualFriendsCount", f.mutualFriendsCount)
            obj.put("avatarColorHex", f.avatarColorHex)
            obj.put("isVerified", f.isVerified)
            obj.put("isOnline", f.isOnline)
            obj.put("status", f.status.name)
            obj.put("requestTimeAgo", f.requestTimeAgo)
            array.put(obj)
        }
        prefs.edit().putString(KEY_FRIENDS, array.toString()).apply()
    }

    private fun getDefaultFriends(): List<FriendUser> {
        return listOf(
            FriendUser(
                id = "u_req_1",
                name = "তানভীর আহমেদ",
                handle = "@tanvir_ahmed",
                bio = "ফটোগ্রাফি ও ট্রাভেল ভ্লগার 📸",
                mutualFriendsCount = 12,
                avatarColorHex = 0xFF009688,
                isVerified = true,
                isOnline = true,
                status = FriendStatus.INCOMING_REQUEST,
                requestTimeAgo = "১৫ মিনিট আগে"
            ),
            FriendUser(
                id = "u_req_2",
                name = "নুসরাত জাহান",
                handle = "@nusrat_jahan",
                bio = "ডিজিটাল আর্টিস্ট ও ডিজাইনার 🎨",
                mutualFriendsCount = 8,
                avatarColorHex = 0xFFE91E63,
                isOnline = true,
                status = FriendStatus.INCOMING_REQUEST,
                requestTimeAgo = "২ ঘণ্টা আগে"
            ),
            FriendUser(
                id = "u_req_3",
                name = "রাফি ইসলাম",
                handle = "@rafi_islam",
                bio = "টেক রিভিউয়ার ও গেমার 🎮",
                mutualFriendsCount = 5,
                avatarColorHex = 0xFF3F51B5,
                isOnline = false,
                status = FriendStatus.INCOMING_REQUEST,
                requestTimeAgo = "১ দিন আগে"
            ),
            FriendUser(
                id = "u_req_4",
                name = "সাদিয়া আক্তার",
                handle = "@sadia_akter",
                bio = "কবিতা ও গল্প ভালোবাসি ✍️",
                mutualFriendsCount = 3,
                avatarColorHex = 0xFF9C27B0,
                isOnline = false,
                status = FriendStatus.INCOMING_REQUEST,
                requestTimeAgo = "২ দিন আগে"
            ),
            FriendUser(
                id = "u_fr_1",
                name = "মাহমুদ হাসান",
                handle = "@mahmud_dev",
                bio = "সফটওয়্যার ইঞ্জিনিয়ার 💻",
                mutualFriendsCount = 24,
                avatarColorHex = 0xFF5352ED,
                isVerified = true,
                isOnline = true,
                status = FriendStatus.FRIEND
            ),
            FriendUser(
                id = "u_fr_2",
                name = "ফারহানা ইয়াসমিন",
                handle = "@farhana_y",
                bio = "ফ্যাশন ও লাইফস্টাইল ক্রিয়েটর ✨",
                mutualFriendsCount = 19,
                avatarColorHex = 0xFFFF5722,
                isVerified = true,
                isOnline = true,
                status = FriendStatus.FRIEND
            ),
            FriendUser(
                id = "u_fr_3",
                name = "আরিফুর রহমান",
                handle = "@arifur_bd",
                bio = "মিউজিক লাভার 🎵",
                mutualFriendsCount = 11,
                avatarColorHex = 0xFF2196F3,
                isOnline = false,
                status = FriendStatus.FRIEND
            ),
            FriendUser(
                id = "u_fr_4",
                name = "তাসনিম তাবাসসুম",
                handle = "@tasnim_t",
                bio = "বই ও কফি ☕📚",
                mutualFriendsCount = 7,
                avatarColorHex = 0xFF8E24AA,
                isOnline = true,
                status = FriendStatus.FRIEND
            ),
            FriendUser(
                id = "u_fr_5",
                name = "মেহেদী হাসান",
                handle = "@mehedi_hasan",
                bio = "Waves রিলস মেকার 🎬",
                mutualFriendsCount = 15,
                avatarColorHex = 0xFF00897B,
                isOnline = false,
                status = FriendStatus.FRIEND
            ),
            FriendUser(
                id = "u_sug_1",
                name = "জুবায়ের খান",
                handle = "@zubayer_k",
                bio = "ফিটনেস ও স্পোর্টস 🏋️",
                mutualFriendsCount = 6,
                avatarColorHex = 0xFFD84315,
                isOnline = true,
                status = FriendStatus.SUGGESTION
            ),
            FriendUser(
                id = "u_sug_2",
                name = "মিথিলা রহমান",
                handle = "@mithila_r",
                bio = "ফুড ব্লগার 🍲",
                mutualFriendsCount = 10,
                avatarColorHex = 0xFFC2185B,
                isVerified = true,
                isOnline = false,
                status = FriendStatus.SUGGESTION
            ),
            FriendUser(
                id = "u_sug_3",
                name = "নাফিজ আহমেদ",
                handle = "@nafiz_a",
                bio = "ইউটিউবার ও কনটেন্ট ক্রিয়েটর 🎥",
                mutualFriendsCount = 4,
                avatarColorHex = 0xFF1E88E5,
                isOnline = true,
                status = FriendStatus.SUGGESTION
            ),
            FriendUser(
                id = "u_sug_4",
                name = "সুমাইয়া ইসলাম",
                handle = "@sumaiya_i",
                bio = "ভ্রমণপিপাসু 🌍",
                mutualFriendsCount = 2,
                avatarColorHex = 0xFF43A047,
                isOnline = false,
                status = FriendStatus.SUGGESTION
            )
        )
    }
}

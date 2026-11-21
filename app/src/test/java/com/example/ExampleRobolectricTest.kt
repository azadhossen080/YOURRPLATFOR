package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.FriendStatus
import com.example.util.AppStringsRegistry
import com.example.viewmodel.PlatformViewModel
import com.example.viewmodel.PostUploadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("YOURPLATFORM", appName)
    }

    @Test
    fun `launch MainActivity successfully`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
    }

    @Test
    fun `friends list and friend request lifecycle works`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PlatformViewModel(app)

        val initialFriends = viewModel.friends.value
        assertTrue(initialFriends.any { it.status == FriendStatus.INCOMING_REQUEST })
        assertTrue(initialFriends.any { it.status == FriendStatus.FRIEND })
        assertTrue(initialFriends.any { it.status == FriendStatus.SUGGESTION })

        val incoming = initialFriends.first { it.status == FriendStatus.INCOMING_REQUEST }
        viewModel.acceptFriendRequest(incoming.id)
        val afterAccept = viewModel.friends.value.first { it.id == incoming.id }
        assertEquals(FriendStatus.FRIEND, afterAccept.status)

        val suggestion = viewModel.friends.value.first { it.status == FriendStatus.SUGGESTION }
        viewModel.sendFriendRequest(suggestion.id)
        assertEquals(FriendStatus.OUTGOING_REQUEST, viewModel.friends.value.first { it.id == suggestion.id }.status)

        viewModel.cancelFriendRequest(suggestion.id)
        assertEquals(FriendStatus.SUGGESTION, viewModel.friends.value.first { it.id == suggestion.id }.status)

        viewModel.unfriend(incoming.id)
        assertEquals(FriendStatus.SUGGESTION, viewModel.friends.value.first { it.id == incoming.id }.status)
    }

    @Test
    fun `language change updates settings and localized strings`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PlatformViewModel(app)

        val enStrings = AppStringsRegistry.forCode("en_US")
        assertEquals("Settings & Privacy", enStrings.settingsTitle)
        assertEquals("Publish", enStrings.publishButton)
        assertFalse(AppStringsRegistry.isRtl("en_US"))
        assertTrue(AppStringsRegistry.isRtl("ar_SA"))

        viewModel.updateSettings(
            viewModel.settings.value.copy(
                selectedLanguage = "English (United States)",
                selectedLanguageCode = "en_US"
            )
        )
        assertEquals("en_US", viewModel.settings.value.selectedLanguageCode)

        // Verify persistence across ViewModel re-creation
        val reloaded = PlatformViewModel(app)
        assertEquals("en_US", reloaded.settings.value.selectedLanguageCode)
        assertEquals("English (United States)", reloaded.settings.value.selectedLanguage)
    }

    @Test
    fun `post processing and upload workflow persists image post and shows upload states`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PlatformViewModel(app)

        val tempImg = File(app.cacheDir, "test_upload_${System.currentTimeMillis()}.jpg")
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempImg).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bmp.recycle()

        val initialCount = viewModel.posts.value.size
        var publishedCallbackInvoked = false
        viewModel.submitPost(
            text = "Processed photo test post",
            type = "gallery",
            mediaUri = Uri.fromFile(tempImg).toString(),
            onPublished = { publishedCallbackInvoked = true }
        )

        // Should enter Uploading state immediately
        assertTrue(viewModel.uploadState.value is PostUploadState.Uploading)

        // Advance Robolectric main looper & IO coroutine steps to complete
        val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        var attempts = 0
        while (viewModel.uploadState.value is PostUploadState.Uploading && attempts < 50) {
            mainLooper.idleFor(java.time.Duration.ofMillis(250))
            delay(50)
            attempts++
        }

        assertTrue(
            "Expected Success but was ${viewModel.uploadState.value}",
            viewModel.uploadState.value is PostUploadState.Success
        )
        assertTrue(publishedCallbackInvoked)
        assertEquals(initialCount + 1, viewModel.posts.value.size)

        val createdPost = viewModel.posts.value.first()
        assertEquals("Processed photo test post", createdPost.postText)
        assertNotNull(createdPost.mediaUri)
        assertTrue(createdPost.mediaUri!!.contains("/files/media/"))
        assertEquals(0, viewModel.selectedTab.value)
    }

    @Test
    fun `default followers is zero and create user name and user id persists`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val viewModel = PlatformViewModel(app)
        assertEquals("0", viewModel.userProfile.value.followersCount)
        assertFalse(viewModel.userProfile.value.isProfileCreated)

        viewModel.createOrUpdateIdentity("Nabil Hasan", "nabil_99")
        assertEquals("Nabil Hasan", viewModel.userProfile.value.name)
        assertEquals("nabil_99", viewModel.userProfile.value.userId)
        assertEquals("@nabil_99", viewModel.userProfile.value.handle)
        assertEquals("0", viewModel.userProfile.value.followersCount)
        assertTrue(viewModel.userProfile.value.isProfileCreated)

        val reloaded = PlatformViewModel(app)
        assertEquals("Nabil Hasan", reloaded.userProfile.value.name)
        assertEquals("nabil_99", reloaded.userProfile.value.userId)
        assertEquals("0", reloaded.userProfile.value.followersCount)
        assertTrue(reloaded.userProfile.value.isProfileCreated)
    }

    @Test
    fun `reviews control and collected user data export json work`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val viewModel = PlatformViewModel(app)
        viewModel.createOrUpdateIdentity("Sara Khan", "sara_id_01", email = "sara@example.com")
        assertFalse(viewModel.userProfile.value.isAdmin)

        // Regular user can submit star rating even with blank comment
        viewModel.submitReview(rating = 4, comment = "")
        assertEquals(1, viewModel.reviews.value.size)
        assertEquals(4, viewModel.reviews.value.first().rating)

        viewModel.submitReview(rating = 5, comment = "Clean interface and fast posting!")
        assertEquals(2, viewModel.reviews.value.size)
        val rev = viewModel.reviews.value.first()
        assertEquals("Sara Khan", rev.userName)
        assertEquals("sara_id_01", rev.userId)
        assertEquals(5, rev.rating)

        // Admin account automatically unlocks review moderation
        viewModel.createOrUpdateIdentity("Sazad Admin", "admin_01", email = "djsazad@gmail.com")
        assertTrue(viewModel.userProfile.value.isAdmin)

        viewModel.togglePinReview(rev.id)
        assertTrue(viewModel.reviews.value.first().isPinned)

        viewModel.updateReviewStatus(rev.id, com.example.model.ReviewStatus.HIDDEN)
        assertEquals(com.example.model.ReviewStatus.HIDDEN, viewModel.reviews.value.first().status)

        val en = AppStringsRegistry.forCode("en_US")
        val bn = AppStringsRegistry.forCode("bn_BD")
        assertEquals("https://t.me/sazadofficialbd", en.community.communityUrlLabel)
        assertEquals("Join It", bn.community.joinItBtn)
        assertFalse(en.auth.authSubtitle.contains("firebase", ignoreCase = true))
        assertFalse(en.auth.cloudSyncedLabel.contains("firestore", ignoreCase = true))
        assertFalse(bn.auth.authSubtitle.contains("firebase", ignoreCase = true))

        viewModel.deleteReview(rev.id)
        assertEquals(1, viewModel.reviews.value.size)
    }

    @Test
    fun `identity email phone registration and Firestore payload parity check`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val viewModel = PlatformViewModel(app)
        viewModel.createOrUpdateIdentity(
            userName = "Ayesha Rahman",
            userId = "ayesha_bd",
            email = "ayesha@example.com",
            phoneNumber = "+8801712345678"
        )
        viewModel.updateVerificationAndVip(isVerified = true, isVip = true)

        val profile = viewModel.userProfile.value
        assertEquals("Ayesha Rahman", profile.name)
        assertEquals("ayesha_bd", profile.userId)
        assertEquals("ayesha@example.com", profile.email)
        assertEquals("+8801712345678", profile.phoneNumber)
        assertEquals("0", profile.followersCount)
        assertTrue(profile.isProfileCreated)
        assertTrue(profile.isVerified)
        assertTrue(profile.isVip)

        val publicMap = com.example.data.FirestorePayloadMapper.toPublicProfileCreateMap("uid_123", profile)
        assertFalse(publicMap.containsKey("id"))
        assertFalse(publicMap.containsKey("email"))
        assertFalse(publicMap.containsKey("phoneNumber"))
        assertFalse(publicMap.containsKey("coverImageUri"))
        assertEquals("uid_123", publicMap["uid"])
        assertEquals("ayesha_bd", publicMap["customUserId"])
        assertEquals(true, publicMap["profileCreated"])

        val privateMap = com.example.data.FirestorePayloadMapper.toPrivateInfoCreateMap(
            uid = "uid_123",
            email = profile.email,
            phoneNumber = profile.phoneNumber
        )
        assertFalse(privateMap.containsKey("id"))
        assertEquals("ayesha@example.com", privateMap["email"])
        assertEquals("+8801712345678", privateMap["phoneNumber"])

        viewModel.setSelectedTab(4)
        viewModel.prepareForSignOut()
        assertEquals(0, viewModel.selectedTab.value)
        assertTrue(viewModel.cloudSyncState.value is com.example.viewmodel.FirestoreSyncState.Idle)
    }

    @Test
    fun `local account create login verification captcha and viewModel binding work`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences(com.example.data.LocalAccountStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val store = com.example.data.LocalAccountStore(app)
        val code = com.example.data.LocalAccountStore.generateVerificationCode()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })

        val captcha = com.example.data.LocalAccountStore.generateCaptchaCode()
        assertEquals(5, captcha.length)
        assertTrue(captcha.all { it.isLetterOrDigit() })

        assertTrue(com.example.data.LocalAccountStore.isValidContact("+880 1712-345678"))
        assertTrue(com.example.data.LocalAccountStore.isValidContact("user@example.com"))
        assertFalse(com.example.data.LocalAccountStore.isValidContact("not_valid"))

        val created = store.registerAccount(
            firstName = "Tasnim",
            lastName = "Karim",
            contact = "+8801712345678",
            rawPassword = "StrongPass123"
        )
        assertTrue(created is com.example.data.AccountAuthResult.Success)
        val account = (created as com.example.data.AccountAuthResult.Success).account
        assertEquals("Tasnim Karim", account.fullName)
        assertNotNull(store.getActiveAccount())

        // Verify password is not stored in plaintext
        val rawAccountsJson = app.getSharedPreferences(
            com.example.data.LocalAccountStore.PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString("accounts_json", "") ?: ""
        assertFalse(rawAccountsJson.contains("StrongPass123"))

        // Duplicate contact rejected
        val dup = store.registerAccount("Another", "User", "+880 1712-345678", "AnotherPass1")
        assertTrue(dup is com.example.data.AccountAuthResult.DuplicateAccount)

        // Logout clears active session
        store.logoutLocalAccount()
        assertEquals(null, store.getActiveAccount())

        // Wrong password rejected, right password (formatted phone) succeeds
        val wrong = store.authenticate("+8801712345678", "WrongPassword")
        assertTrue(wrong is com.example.data.AccountAuthResult.InvalidCredentials)

        val valid = store.authenticate("+880 1712-345678", "StrongPass123")
        assertTrue(valid is com.example.data.AccountAuthResult.Success)

        val vm = PlatformViewModel(app)
        vm.bindLocalAccount(account)
        assertEquals("Tasnim Karim", vm.userProfile.value.name)
        assertEquals("+8801712345678", vm.userProfile.value.phoneNumber)
        assertTrue(vm.userProfile.value.isProfileCreated)
    }

    @Test
    fun `profile picture and cover photo changes publish to profile feed`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val viewModel = PlatformViewModel(app)
        viewModel.createOrUpdateIdentity("Tariqul Islam", "tariq_2026")
        val initialPosts = viewModel.posts.value.size

        val avatarImg = File(app.cacheDir, "test_avatar_${System.currentTimeMillis()}.jpg")
        val coverImg = File(app.cacheDir, "test_cover_${System.currentTimeMillis()}.jpg")
        listOf(avatarImg, coverImg).forEach { file ->
            val bmp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
            FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            bmp.recycle()
        }

        viewModel.updateAvatarUri(Uri.fromFile(avatarImg).toString())
        val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        repeat(20) {
            if (viewModel.posts.value.size > initialPosts) return@repeat
            mainLooper.idleFor(java.time.Duration.ofMillis(100))
            delay(50)
        }

        assertEquals(initialPosts + 1, viewModel.posts.value.size)
        val avatarPost = viewModel.posts.value.first()
        assertTrue(avatarPost.isUserPost)
        assertNotNull(avatarPost.mediaUri)
        assertEquals(viewModel.userProfile.value.avatarImageUri, avatarPost.mediaUri)

        viewModel.updateCoverUri(Uri.fromFile(coverImg).toString())
        repeat(20) {
            if (viewModel.posts.value.size > initialPosts + 1) return@repeat
            mainLooper.idleFor(java.time.Duration.ofMillis(100))
            delay(50)
        }

        assertEquals(initialPosts + 2, viewModel.posts.value.size)
        val coverPost = viewModel.posts.value.first()
        assertTrue(coverPost.isUserPost)
        assertNotNull(coverPost.mediaUri)
        assertEquals(viewModel.userProfile.value.coverImageUri, coverPost.mediaUri)
    }

    @Test
    fun `posting reels with video studio details runs 1 min review routes to waves and persists`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("yourplatform_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val viewModel = PlatformViewModel(app)
        viewModel.updateVerificationAndVip(isVerified = true, isVip = true)

        val editDraft = com.example.model.MediaEditDraft(
            colorFilter = com.example.model.ColorFilterMode.CINEMA.id,
            transitionEffect = com.example.model.TransitionEffect.ZOOM_IN.id,
            mojiStickers = listOf("🔥", "🌊", "✨"),
            playbackSpeed = 1.5f,
            isMuted = true,
            trimStartFraction = 0.1f,
            trimEndFraction = 0.85f,
            isLiveStream = true,
            soundName = "Waves Beat 2026",
            overlayText = "Dhaka Night Ride",
            overlayColorHex = 0xFFFFD54F,
            timerDurationSec = 30,
            useFrontCamera = false,
            postTitle = "Night Ride Highlights",
            locationName = "Dhaka",
            playlistName = "Travel & Nature",
            tags = listOf("waves", "night"),
            collaborators = listOf("@farhan_x"),
            ageRestriction = "13+"
        )

        var sawReviewStage = false
        var publishedInvoked = false
        viewModel.submitPost(
            text = "My Edited Live Reel #waves",
            type = "reels",
            mediaUri = null,
            previewUri = null,
            editDraft = editDraft,
            onPublished = { publishedInvoked = true }
        )

        val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        var attempts = 0
        while (viewModel.uploadState.value is PostUploadState.Uploading && attempts < 80) {
            val state = viewModel.uploadState.value
            if (state is PostUploadState.Uploading && state.stage == com.example.viewmodel.UploadStage.REVIEW_1_MIN) {
                sawReviewStage = true
            }
            mainLooper.idleFor(java.time.Duration.ofMillis(120))
            delay(40)
            attempts++
        }

        assertTrue("Expected REVIEW_1_MIN upload stage to be observed", sawReviewStage)
        assertTrue(publishedInvoked)
        assertEquals(1, viewModel.selectedTab.value)

        val createdReel = viewModel.posts.value.first()
        assertEquals(createdReel.id, viewModel.activeReelPostId.value)
        assertTrue(createdReel.isVideo)
        assertEquals("cinema", createdReel.colorFilter)
        assertEquals("zoom_in", createdReel.transitionEffect)
        assertEquals(listOf("🔥", "🌊", "✨"), createdReel.mojiStickers)
        assertEquals(1.5f, createdReel.playbackSpeed)
        assertTrue(createdReel.isMuted)
        assertEquals(0.1f, createdReel.trimStartFraction)
        assertEquals(0.85f, createdReel.trimEndFraction)
        assertTrue(createdReel.isLiveStream)
        assertEquals("Night Ride Highlights", createdReel.postTitle)
        assertEquals("Waves Beat 2026", createdReel.soundName)
        assertEquals("Dhaka Night Ride", createdReel.overlayText)
        assertEquals(0xFFFFD54F, createdReel.overlayColorHex)
        assertEquals(30, createdReel.timerDurationSec)
        assertEquals("Dhaka", createdReel.locationName)
        assertEquals("Travel & Nature", createdReel.playlistName)
        assertEquals(listOf("waves", "night"), createdReel.tags)
        assertEquals(listOf("@farhan_x"), createdReel.collaborators)
        assertEquals("13+", createdReel.ageRestriction)
        assertTrue(createdReel.authorVerified)
        assertTrue(createdReel.authorVip)

        // Verify persistence across ViewModel reload
        val reloaded = PlatformViewModel(app)
        val persisted = reloaded.posts.value.first { it.id == createdReel.id }
        assertEquals("Night Ride Highlights", persisted.postTitle)
        assertEquals("Waves Beat 2026", persisted.soundName)
        assertEquals("Dhaka Night Ride", persisted.overlayText)
        assertEquals("Dhaka", persisted.locationName)
        assertEquals("Travel & Nature", persisted.playlistName)
        assertEquals("13+", persisted.ageRestriction)
        assertTrue(persisted.authorVerified)
        assertTrue(persisted.authorVip)
        assertTrue(reloaded.userProfile.value.isVerified)
        assertTrue(reloaded.userProfile.value.isVip)

        // Verify openReelInWaves switches tab to 1 and focuses requested reel
        reloaded.setSelectedTab(0)
        assertEquals(0, reloaded.selectedTab.value)
        reloaded.openReelInWaves(persisted.id)
        assertEquals(1, reloaded.selectedTab.value)
        assertEquals(persisted.id, reloaded.activeReelPostId.value)

        // Ensure Firestore payload mapper keeps rules-safe mediaType for live posts
        val firestoreMap = com.example.data.FirestorePayloadMapper.toPostCreateMap(
            uid = "uid_test",
            post = persisted.copy(mediaType = "live")
        )
        assertEquals("reels", firestoreMap["mediaType"])
    }
}

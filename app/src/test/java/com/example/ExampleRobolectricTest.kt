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
        viewModel.createOrUpdateIdentity("Sara Khan", "sara_id_01")
        viewModel.submitReview(rating = 5, comment = "Clean interface and fast posting!")

        assertEquals(1, viewModel.reviews.value.size)
        val rev = viewModel.reviews.value.first()
        assertEquals("Sara Khan", rev.userName)
        assertEquals("sara_id_01", rev.userId)
        assertEquals(5, rev.rating)

        viewModel.togglePinReview(rev.id)
        assertTrue(viewModel.reviews.value.first().isPinned)

        viewModel.updateReviewStatus(rev.id, com.example.model.ReviewStatus.HIDDEN)
        assertEquals(com.example.model.ReviewStatus.HIDDEN, viewModel.reviews.value.first().status)

        val exportedFile = viewModel.exportCollectedUserData()
        assertNotNull(exportedFile)
        assertTrue(exportedFile!!.exists())
        val content = exportedFile.readText()
        assertFalse(content.contains("terabox", ignoreCase = true))
        assertFalse(content.contains("firebase", ignoreCase = true))
        assertFalse(content.contains("firestore", ignoreCase = true))
        val parsed = org.json.JSONObject(content)
        assertEquals("Cloud & Local Sync", parsed.getString("storageMode"))
        assertEquals("https://t.me/sazadofficialbd", parsed.getString("communityUrl"))
        val en = AppStringsRegistry.forCode("en_US")
        val bn = AppStringsRegistry.forCode("bn_BD")
        assertEquals("https://t.me/sazadofficialbd", en.community.communityUrlLabel)
        assertEquals("Join It", bn.community.joinItBtn)
        assertFalse(en.auth.authSubtitle.contains("firebase", ignoreCase = true))
        assertFalse(en.auth.cloudSyncedLabel.contains("firestore", ignoreCase = true))
        assertFalse(bn.auth.authSubtitle.contains("firebase", ignoreCase = true))
        assertFalse(bn.reviewsStorage.storageNoticeText.contains("firestore", ignoreCase = true))
        val parsedProfile = parsed.getJSONObject("userProfile")
        assertEquals("sara_id_01", parsedProfile.getString("userId"))
        assertEquals("Sara Khan", parsedProfile.getString("userName"))
        assertEquals("0", parsedProfile.getString("followersCount"))
        assertTrue(content.contains("Clean interface and fast posting!"))

        viewModel.deleteReview(rev.id)
        assertTrue(viewModel.reviews.value.isEmpty())
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

        val profile = viewModel.userProfile.value
        assertEquals("Ayesha Rahman", profile.name)
        assertEquals("ayesha_bd", profile.userId)
        assertEquals("ayesha@example.com", profile.email)
        assertEquals("+8801712345678", profile.phoneNumber)
        assertEquals("0", profile.followersCount)
        assertTrue(profile.isProfileCreated)

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
}

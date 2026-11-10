package com.jhaiian.clint.browser.delegates

import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.blocker.blockedpage.WebsiteBlockedRequest
import com.jhaiian.clint.blocker.engine.WebsiteBlockerWebIntegration
import com.jhaiian.clint.browser.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal const val PREF_ASYNC_BLOCKER_LOAD = "async_blocker_load"

internal fun MainActivity.initializeWebsiteBlockerEngine() {
    // Optional background load (off by default): the blocker is a safety feature, so
    // the synchronous load stays the default and guarantees the first page is checked.
    // Loading is disk + JNI work; the engine itself is queried from WebView background
    // threads, so it is thread-safe either way.
    if (prefs.getBoolean(PREF_ASYNC_BLOCKER_LOAD, false)) {
        lifecycleScope.launch(Dispatchers.IO) {
            WebsiteBlockerWebIntegration.initialize(this@initializeWebsiteBlockerEngine)
        }
    } else {
        WebsiteBlockerWebIntegration.initialize(this)
    }
}

internal fun MainActivity.onWebsiteBlocked(blockedUrl: String, previousUrl: String, tabId: String) {
    if (tabManager.activeTab?.id != tabId) return
    uiState.websiteBlockedRequest = WebsiteBlockedRequest(
        blockedUrl = blockedUrl,
        previousUrl = previousUrl,
        tabId = tabId
    )
}

internal fun MainActivity.dismissWebsiteBlockedOverlay() {
    val request = uiState.websiteBlockedRequest ?: return
    uiState.websiteBlockedRequest = null
    val tab = tabManager.tabs.find { it.id == request.tabId } ?: return
    if (tab.webView.canGoBack()) {
        tab.webView.goBack()
    } else {
        tab.webView.loadUrl(getHomepageUrl(tab.isIncognito))
    }
}

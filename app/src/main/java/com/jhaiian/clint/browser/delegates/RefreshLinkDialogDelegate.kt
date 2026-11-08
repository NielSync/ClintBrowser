package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.RefreshLinkDialogRequest
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.ui.ClintToast

internal fun MainActivity.showRefreshLinkDownloadDialog(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    session: MainActivity.RefreshLinkSession,
    pageUrl: String = referer
) {
    uiState.refreshLinkDialogRequest = RefreshLinkDialogRequest(
        existingFilename = session.filename,
        onUpdateExisting = {
            ClintDownloadManager.updateDownloadUrl(session.downloadId, url, referer, cookies, pageUrl)
            ClintToast.show(this, getString(R.string.refresh_link_updated_toast, session.filename))
            refreshLinkSession = null
        },
        onAddNew = { showDownloadDialog(url, filename, userAgent, referer, cookies, pageUrl = pageUrl) }
    )
}

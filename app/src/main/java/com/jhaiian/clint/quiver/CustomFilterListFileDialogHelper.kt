package com.jhaiian.clint.quiver

import android.content.Intent
import android.net.Uri
import com.jhaiian.clint.R
import kotlinx.coroutines.launch
import com.jhaiian.clint.ui.ClintToast

internal fun QuiverGuardActivity.launchAddFilterListFromFile() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }
    filePickerLauncher.launch(intent)
}

internal fun QuiverGuardActivity.importFilterListFromFile(uri: Uri) {
    activityScope.launch {
        when (val result = LocalFilterListImporter.import(applicationContext, uri)) {
            is LocalFilterListImportResult.Success -> uiState.addFromFileImport = result
            is LocalFilterListImportResult.Error -> ClintToast.show(this@importFilterListFromFile, getString(result.messageResId))
        }
    }
}

internal fun QuiverGuardActivity.confirmAddFilterListFromFile(title: String) {
    val imported = uiState.addFromFileImport ?: return
    val id = database().addCustomFilterList(title, "")
    database().updateDownloadResult(id, imported.file.absolutePath, imported.sizeBytes, System.currentTimeMillis(), imported.ruleCount, null, null)
    onFilterListAdded(FilterList(id = id, name = title, downloadUrl = "", isEnabled = true, localPath = imported.file.absolutePath, fileSizeBytes = imported.sizeBytes, downloadedAt = System.currentTimeMillis(), ruleCount = imported.ruleCount, isCustom = true))
    uiState.addFromFileImport = null
    ClintToast.show(this, getString(R.string.quiver_guard_download_success_toast, title))
}

package com.jhaiian.clint.blocker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.ClintToast

internal fun WebsiteBlockerActivity.confirmCheckUpdateForItem(category: WebsiteBlockerCategory) {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    if (!category.isDownloaded) {
        ClintToast.show(this, getString(R.string.filter_list_item_not_downloaded, categoryTitle(category)))
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.filter_list_item_check_update_confirm_message, categoryTitle(category)),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { downloadCategories(listOf(category), force = false) }
    )
}

internal fun WebsiteBlockerActivity.confirmForceUpdateForItem(category: WebsiteBlockerCategory) {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    if (!category.isDownloaded) {
        ClintToast.show(this, getString(R.string.filter_list_item_not_downloaded, categoryTitle(category)))
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_force_update_selected_confirm_title),
        message = getString(R.string.filter_list_item_force_update_confirm_message, categoryTitle(category)),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { downloadCategories(listOf(category), force = true) }
    )
}

internal fun WebsiteBlockerActivity.confirmRemoveForItem(category: WebsiteBlockerCategory) {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.website_blocker_delete_confirm_title),
        message = getString(R.string.website_blocker_delete_confirm_message, 1),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.history_delete_selected),
        onPositive = { removeCategories(listOf(category.id)) }
    )
}

internal fun WebsiteBlockerActivity.copyCategoryName(category: WebsiteBlockerCategory) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.website_blocker_name_clip_label), categoryTitle(category)))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        ClintToast.show(this, getString(R.string.filter_list_item_name_copied))
    }
}

internal fun WebsiteBlockerActivity.copyCategoryLink(category: WebsiteBlockerCategory) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.website_blocker_link_clip_label), category.downloadUrl))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        ClintToast.show(this, getString(R.string.filter_list_item_link_copied))
    }
}

internal fun WebsiteBlockerActivity.shareCategoryLink(category: WebsiteBlockerCategory) {
    try {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, category.downloadUrl) },
                getString(R.string.website_blocker_share_chooser_title)
            )
        )
    } catch (_: Exception) {
    }
}

internal fun WebsiteBlockerActivity.confirmCheckUpdateForSelection() {
    val selection = selectedCategories()
    if (selection.isEmpty()) return
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    val downloaded = selection.filter { it.isDownloaded }
    if (downloaded.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.website_blocker_selection_not_downloaded),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.website_blocker_check_update_selected_message, downloaded.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { downloadCategories(downloaded, force = false) }
    )
}

internal fun WebsiteBlockerActivity.confirmForceUpdateForSelection() {
    val selection = selectedCategories()
    if (selection.isEmpty()) return
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    val downloaded = selection.filter { it.isDownloaded }
    if (downloaded.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_force_update_selected_confirm_title),
            message = getString(R.string.website_blocker_selection_not_downloaded),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_force_update_selected_confirm_title),
        message = getString(R.string.website_blocker_force_update_selected_confirm_message, downloaded.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { downloadCategories(downloaded, force = true) }
    )
}

internal fun WebsiteBlockerActivity.confirmRemoveForSelection() {
    val ids = uiState.selectedIds
    if (ids.isEmpty()) return
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.website_blocker_delete_confirm_title),
        message = getString(R.string.website_blocker_delete_confirm_message, ids.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.history_delete_selected),
        onPositive = { removeCategories(ids); uiState.exitSelectionMode() }
    )
}

internal fun WebsiteBlockerActivity.copySelectedCategoryNames() {
    val selection = selectedCategories()
    if (selection.isEmpty()) return
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.website_blocker_name_clip_label), selection.joinToString("\n") { categoryTitle(it) }))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        ClintToast.show(this, getString(R.string.filter_list_selection_names_copied))
    }
}

internal fun WebsiteBlockerActivity.copySelectedCategoryLinks() {
    val selection = selectedCategories()
    if (selection.isEmpty()) return
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.website_blocker_link_clip_label), selection.joinToString("\n") { it.downloadUrl }))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        ClintToast.show(this, getString(R.string.filter_list_selection_links_copied))
    }
}

internal fun WebsiteBlockerActivity.shareSelectedCategoryLinks() {
    val selection = selectedCategories()
    if (selection.isEmpty()) return
    try {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, selection.joinToString("\n") { it.downloadUrl })
                },
                getString(R.string.website_blocker_share_chooser_title)
            )
        )
    } catch (_: Exception) {
    }
}

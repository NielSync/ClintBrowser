package com.jhaiian.clint.blocker

import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.ClintToast

internal fun WebsiteBlockerActivity.confirmCheckUpdateAllCategories() {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    val downloaded = uiState.categories.filter { it.isDownloaded }
    if (downloaded.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.website_blocker_update_no_categories_message),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.website_blocker_update_check_message, downloaded.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { downloadCategories(downloaded, force = false) }
    )
}

internal fun WebsiteBlockerActivity.confirmForceUpdateAllCategories() {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    val downloaded = uiState.categories.filter { it.isDownloaded }
    if (downloaded.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.website_blocker_force_update_all_confirm_title),
            message = getString(R.string.website_blocker_force_update_no_categories_message),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.website_blocker_force_update_all_confirm_title),
        message = getString(R.string.website_blocker_force_update_all_confirm_message, downloaded.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { downloadCategories(downloaded, force = true) }
    )
}

internal fun WebsiteBlockerActivity.confirmCheckUpdateActive() {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    val active = uiState.categories.filter { it.isEnabled && it.isDownloaded }
    if (active.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.filter_list_update_check_title),
            message = getString(R.string.website_blocker_no_active_selected),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.filter_list_update_check_title),
        message = getString(R.string.website_blocker_check_update_active_message, active.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_update_check_action),
        onPositive = { downloadCategories(active, force = false) }
    )
}

internal fun WebsiteBlockerActivity.confirmForceUpdateActive() {
    if (uiState.isDownloadRunning || uiState.isCompileRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    val active = uiState.categories.filter { it.isEnabled && it.isDownloaded }
    if (active.isEmpty()) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.website_blocker_force_update_active_confirm_title),
            message = getString(R.string.website_blocker_no_active_selected),
            positiveLabel = getString(R.string.action_ok)
        )
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.website_blocker_force_update_active_confirm_title),
        message = getString(R.string.website_blocker_force_update_active_confirm_message, active.size),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.filter_list_force_update_action),
        onPositive = { downloadCategories(active, force = true) }
    )
}

internal fun WebsiteBlockerActivity.confirmRecompile() {
    if (uiState.isCompileRunning) return
    if (uiState.isDownloadRunning) {
        ClintToast.show(this, getString(R.string.filter_list_operation_in_progress))
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.website_blocker_recompile_confirm_title),
        message = getString(R.string.website_blocker_recompile_confirm_message),
        negativeLabel = getString(R.string.action_cancel),
        positiveLabel = getString(R.string.quiver_guard_back_dialog_compile),
        onPositive = { startCompile() }
    )
}

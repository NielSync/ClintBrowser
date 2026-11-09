package com.jhaiian.clint.browser.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.summarize.PageSummarizer
import com.jhaiian.clint.summarize.extractPageForSummarize
import com.jhaiian.clint.ui.theme.LocalClintColors

/**
 * Bottom sheet that summarizes the current page through the user's configured
 * AI provider. Opened from the toolbar flash button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SummarizeSheet(activity: MainActivity, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val clipboard = LocalClipboardManager.current

    var pageTitle by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf<Stage>(Stage.Working) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        if (activity.uiState.isIncognito) {
            stage = Stage.Failed(FailReason.INCOGNITO)
            return@LaunchedEffect
        }
        if (prefs.getString(PageSummarizer.PREF_API_KEY, "")?.isBlank() != false) {
            stage = Stage.Failed(FailReason.NO_KEY)
            return@LaunchedEffect
        }
        stage = Stage.Working
        activity.extractPageForSummarize { title, text ->
            pageTitle = title
            if (text.isBlank()) {
                stage = Stage.Failed(FailReason.NO_CONTENT)
                return@extractPageForSummarize
            }
            PageSummarizer.summarize(prefs, title, text) { result ->
                stage = result.fold(
                    onSuccess = { Stage.Done(it) },
                    onFailure = { Stage.Failed(FailReason.ERROR(it.message ?: "")) }
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = stringResource(R.string.summarize_sheet_title),
                        color = colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (pageTitle.isNotBlank()) {
                        Text(
                            text = pageTitle,
                            color = colors.secondaryText,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (val current = stage) {
                is Stage.Working -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.summarize_generating),
                        color = colors.secondaryText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                is Stage.Done -> Column {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = current.text,
                            color = colors.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_close), color = colors.primary)
                        }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(current.text))
                            com.jhaiian.clint.ui.ClintToast.show(activity, activity.getString(R.string.summarize_copied))
                        }) {
                            Text(stringResource(R.string.summarize_copy), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                is Stage.Failed -> Column {
                    Text(
                        text = when (val reason = current.reason) {
                            FailReason.INCOGNITO -> stringResource(R.string.summarize_incognito)
                            FailReason.NO_KEY -> stringResource(R.string.summarize_no_key)
                            FailReason.NO_CONTENT -> stringResource(R.string.summarize_no_content)
                            is FailReason.ERROR -> stringResource(R.string.summarize_failed, reason.detail ?: "")
                        },
                        color = colors.secondaryText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                        if (current.reason == FailReason.NO_KEY) {
                            TextButton(onClick = {
                                onDismiss()
                                activity.startActivity(
                                    android.content.Intent(activity, com.jhaiian.clint.settings.SettingsActivity::class.java)
                                        .putExtra(com.jhaiian.clint.settings.SettingsActivity.EXTRA_OPEN_FRAGMENT, "summarize")
                                )
                            }) {
                                Text(stringResource(R.string.summarize_open_settings), color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (current.reason != FailReason.INCOGNITO) {
                            TextButton(onClick = { attempt += 1 }) {
                                Text(stringResource(R.string.summarize_retry), color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.summarize_privacy_note),
                color = colors.secondaryText,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private sealed interface Stage {
    data object Working : Stage
    data class Done(val text: String) : Stage
    data class Failed(val reason: FailReason) : Stage
}

private sealed interface FailReason {
    data object INCOGNITO : FailReason
    data object NO_KEY : FailReason
    data object NO_CONTENT : FailReason
    data class ERROR(val detail: String?) : FailReason
}

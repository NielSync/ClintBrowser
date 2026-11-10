package com.jhaiian.clint.settings.summarize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.summarize.PageSummarizer
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

/**
 * Settings pane for the webpage summarize feature: provider, API key, model,
 * and the toolbar flash-button toggle.
 */
@Composable
fun SummarizePane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }

    var providerId by remember { mutableStateOf(prefs.getString(PageSummarizer.PREF_PROVIDER, PageSummarizer.PROVIDER_OPENAI) ?: PageSummarizer.PROVIDER_OPENAI) }
    var apiKey by remember { mutableStateOf(prefs.getString(PageSummarizer.PREF_API_KEY, "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString(PageSummarizer.PREF_MODEL, "") ?: "") }
    var baseUrl by remember { mutableStateOf(prefs.getString(PageSummarizer.PREF_BASE_URL, "") ?: "") }
    var toolbarButton by remember { mutableStateOf(prefs.getBoolean(PageSummarizer.PREF_TOOLBAR_BUTTON, false)) }
    var shakeToSummarize by remember { mutableStateOf(prefs.getBoolean(PageSummarizer.PREF_SHAKE, true)) }
    var providerDialog by remember { mutableStateOf(false) }

    val provider = PageSummarizer.PROVIDERS.firstOrNull { it.id == providerId } ?: PageSummarizer.PROVIDERS.first()
    val providerLabel = stringResource(
        when (provider.id) {
            PageSummarizer.PROVIDER_OPENAI -> R.string.summarize_provider_openai
            PageSummarizer.PROVIDER_GROQ -> R.string.summarize_provider_groq
            PageSummarizer.PROVIDER_OPENROUTER -> R.string.summarize_provider_openrouter
            else -> R.string.summarize_provider_custom
        }
    )

    SettingsScreenScaffold(
        overlay = {
            if (providerDialog) {
                ProviderDialog(
                    current = providerId,
                    onSelect = { id ->
                        providerId = id
                        prefs.edit().putString(PageSummarizer.PREF_PROVIDER, id).apply()
                        providerDialog = false
                    },
                    onDismiss = { providerDialog = false },
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation
                )
            }
        }
    ) {
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Bolt,
                title = stringResource(R.string.summarize_provider),
                summary = providerLabel,
                colors = colors,
                onClick = { providerDialog = true },
                trailing = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = colors.secondaryText
                    )
                }
            )
            RowDivider(colors.divider)
            SettingsField(
                label = stringResource(R.string.summarize_api_key),
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    prefs.edit().putString(PageSummarizer.PREF_API_KEY, it).apply()
                },
                masked = true
            )
            RowDivider(colors.divider)
            SettingsField(
                label = stringResource(R.string.summarize_model),
                value = model,
                onValueChange = {
                    model = it
                    prefs.edit().putString(PageSummarizer.PREF_MODEL, it).apply()
                },
                hint = provider.defaultModel
            )
            if (provider.id == PageSummarizer.PROVIDER_CUSTOM) {
                RowDivider(colors.divider)
                SettingsField(
                    label = stringResource(R.string.summarize_base_url),
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        prefs.edit().putString(PageSummarizer.PREF_BASE_URL, it).apply()
                    },
                    hint = "https://example.com/v1"
                )
            }
        }

        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Bolt,
                title = stringResource(R.string.summarize_show_toolbar_button),
                summary = stringResource(R.string.summarize_show_toolbar_button_desc),
                colors = colors,
                onClick = {
                    toolbarButton = !toolbarButton
                    prefs.edit().putBoolean(PageSummarizer.PREF_TOOLBAR_BUTTON, toolbarButton).apply()
                },
                trailing = {
                    ClintSwitch(checked = toolbarButton)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Vibration,
                title = stringResource(R.string.summarize_shake),
                summary = stringResource(R.string.summarize_shake_desc),
                colors = colors,
                onClick = {
                    shakeToSummarize = !shakeToSummarize
                    prefs.edit().putBoolean(PageSummarizer.PREF_SHAKE, shakeToSummarize).apply()
                },
                trailing = {
                    ClintSwitch(checked = shakeToSummarize)
                }
            )
        }
    }
}

@Composable
private fun SettingsField(label: String, value: String, onValueChange: (String) -> Unit, masked: Boolean = false, hint: String = "") {
    val colors = LocalClintColors.current
    val labelContent: @Composable () -> Unit = { Text(label) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        if (masked) {
            var passwordVisible by remember { mutableStateOf(false) }
            com.jhaiian.clint.ui.ClintOutlinedPasswordField(
                value = value,
                onValueChange = onValueChange,
                passwordVisible = passwordVisible,
                label = labelContent,
                trailingIcon = {
                    val desc = stringResource(if (passwordVisible) R.string.summarize_hide_api_key else R.string.summarize_show_api_key)
                    androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) androidx.compose.material.icons.Icons.Filled.VisibilityOff else androidx.compose.material.icons.Icons.Filled.Visibility,
                            contentDescription = desc,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        } else {
            ClintOutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = labelContent,
                singleLine = true,
                supportingText = if (hint.isNotEmpty()) {
                    { Text(hint) }
                } else null
            )
        }
    }
}

@Composable
private fun ProviderDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean
) {
    val colors = LocalClintColors.current
    com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBackground,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface,
        title = { Text(stringResource(R.string.summarize_provider)) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                PageSummarizer.PROVIDERS.forEach { provider ->
                    val label = stringResource(
                        when (provider.id) {
                            PageSummarizer.PROVIDER_OPENAI -> R.string.summarize_provider_openai
                            PageSummarizer.PROVIDER_GROQ -> R.string.summarize_provider_groq
                            PageSummarizer.PROVIDER_OPENROUTER -> R.string.summarize_provider_openrouter
                            else -> R.string.summarize_provider_custom
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(provider.id) }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = provider.id == current, onClick = { onSelect(provider.id) })
                        Text(label, color = colors.onSurface, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.primary)
            }
        }
    )
}

package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsBrowser(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        UserSettings()
        AdminSettings()
    }
}

@Composable
fun UserSettings(modifier: Modifier = Modifier,) {
    Column(modifier = modifier) {
        Text("User Settings")
    }
}

@Composable
fun AdminSettings(modifier: Modifier = Modifier,) {
    Column(modifier = modifier) {
        Text("Admin Settings")
    }
}

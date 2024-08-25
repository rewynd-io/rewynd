package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.items
import io.rewynd.model.User

@Composable
fun SettingsBrowser(
    browserViewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) = with(browserViewModel) {
    val nullableUser by user
    loadUser()

    nullableUser?.let {
        Column(modifier = modifier) {
            DeviceSettings()
            UserSettings(viewModel = browserViewModel, it)
            if (it.permissions.isAdmin == true) {
                AdminSettings(browserViewModel)
            }
        }
    }
}

@Composable
fun UserSettings(viewModel: BrowserViewModel, user: User, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("User Settings")
        Row{
            Text("Enable subtitles by default")
            Checkbox(user.preferences.enableSubtitlesByDefault, {
                viewModel.uploadUserPrefs(user.preferences.copy(enableSubtitlesByDefault = it))
            })
        }
    }
}

@Composable
fun DeviceSettings(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Device Settings")
    }
}

@Composable
fun AdminSettings(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val users = remember { viewModel.listUsers() }.collectAsLazyPagingItems()
    Column(modifier = modifier) {
        Text("Admin Settings")
        Text("Users")
        Row {
            Text("Username")
            Text("Admin")
            Text("Enable subtitles by default")
        }
        LazyVerticalGrid(columns = GridCells.Fixed(1)) {
            items(users) {
                Row {
                    Text(it.username)
                    Text(it.permissions.isAdmin.toString())
                    Text(it.preferences.enableSubtitlesByDefault.toString())
                }
            }
        }
    }
}

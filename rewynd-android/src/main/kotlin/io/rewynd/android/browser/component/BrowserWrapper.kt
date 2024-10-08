package io.rewynd.android.browser.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.rewynd.android.browser.BrowserNavigationActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // TODO use padding
@Composable
fun BrowserWrapper(
    actions: BrowserNavigationActions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar({
            }, navigationIcon = {
                IconButton(
                    onClick = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Drawer", // TODO localize
                    )
                }
            }, actions = {
                IconButton(onClick = { actions.search() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search", // TODO Localize
                    )
                }
            })
        },
    ) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Column {
                        NavigationDrawerItem(
                            label = {
                                Row {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home", // TODO Localize
                                    )
                                    Text(
                                        text = "Home", // TODO Localize
                                    )
                                }
                            },
                            selected = false,
                            onClick = { actions.home() },
                        )
                        NavigationDrawerItem(
                            label = {
                                Row {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings", // TODO Localize
                                    )
                                    Text(
                                        text = "Settings", // TODO Localize
                                    )
                                }
                            },
                            selected = false,
                            onClick = { actions.settings() },
                        )
                    }
                }
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(it),
            content = content,
            drawerState = drawerState
        )
    }
}

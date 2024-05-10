package io.rewynd.android.component

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintLayout
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.login.MainViewModel

@Composable
fun LoginInput(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) = ConstraintLayout {
    val (usernameRef, passwordRef, buttonRef, serverRef) = createRefs()
    val serverUrlState by mainViewModel.serverUrl.collectAsState()
    val username by mainViewModel.username.collectAsState()
    val password by mainViewModel.password.collectAsState()
    val server = serverUrlState.value
    TextField(
        label = { Text(text = "Server URL") },
        value = server,
        onValueChange = { mainViewModel.serverUrl.value = ServerUrl(it) },
        modifier =
            modifier.constrainAs(serverRef) {
                top.linkTo(parent.top)
                bottom.linkTo(usernameRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
    )
    TextField(
        label = { Text(text = "username") },
        value = username,
        onValueChange = { mainViewModel.username.value = it },
        modifier =
            modifier.constrainAs(usernameRef) {
                top.linkTo(serverRef.bottom)
                bottom.linkTo(passwordRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
    )
    TextField(
        label = { Text(text = "password") },
        value = password,
        onValueChange = { mainViewModel.password.value = it },
        modifier =
            modifier.constrainAs(passwordRef) {
                top.linkTo(usernameRef.bottom)
                bottom.linkTo(buttonRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
    )
    Button(
        {
            mainViewModel.login()
        },
        content = {
            Text("Connect")
        },
        modifier =
            modifier.constrainAs(buttonRef) {
                top.linkTo(passwordRef.bottom)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
    )
}

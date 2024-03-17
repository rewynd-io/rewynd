package io.rewynd.android.component

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintLayout
import io.rewynd.android.login.MainViewModel
import io.rewynd.model.LoginRequest

@Composable
fun LoginInput(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) = ConstraintLayout {
    val (usernameRef, passwordRef, buttonRef) = createRefs()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    TextField(
        label = { Text(text = "username") },
        value = username,
        onValueChange = { username = it },
        modifier =
            modifier.constrainAs(usernameRef) {
                top.linkTo(parent.top)
                bottom.linkTo(passwordRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
    )
    TextField(
        label = { Text(text = "password") },
        value = password,
        onValueChange = { password = it },
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
            mainViewModel.login(LoginRequest(username = username, password = password))
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

package io.rewynd.android.login

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.rewynd.android.browser.BrowserActivity
import io.rewynd.android.component.LoginInput

class MainActivity : AppCompatActivity() {
    private val viewModel by lazy {
        MainViewModel(this.application)
    }

    override fun onResume() {
        super.onResume()

        setContent {
            val loginState by viewModel.loginState
            when (loginState) {
                is LoginState.LoggedIn -> {
                    startActivity(Intent(this, BrowserActivity::class.java))
                    finish()
                }
                is LoginState.LoggedOut -> {
                    viewModel.verify() // Check if we are already logged in
                }
                is LoginState.LoggedOutVerificationFailed -> {
                    LoginInput(viewModel)
                }
                is LoginState.PendingLogin,
                is LoginState.PendingVerification,
                ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
            }
        }
    }

    companion object
}

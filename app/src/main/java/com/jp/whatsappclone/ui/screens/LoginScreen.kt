package com.jp.whatsappclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DestructiveRed
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.SecondaryText

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    loading: Boolean = false,
    errorMessage: String? = null,
    firebaseConfigured: Boolean = true,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val emailValid = email.trim().let { it.contains('@') && it.substringAfter('@').contains('.') }
    val passwordValid = password.isNotBlank()
    val submit = {
        submitted = true
        if (emailValid && passwordValid) {
            focusManager.clearFocus()
            onLogin(email.trim(), password)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(54.dp))
        Box(Modifier.size(82.dp).clip(CircleShape).background(BrandGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Chat, null, tint = AppBackground, modifier = Modifier.size(45.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Welcome back", color = PrimaryText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Sign in to INNOVFIX Internal Messenger", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(42.dp))

        LoginField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = Icons.Outlined.Email,
            isError = submitted && !emailValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        )
        if (submitted && !emailValid) {
            LoginError("Enter a valid email address")
        } else {
            Spacer(Modifier.height(18.dp))
        }

        LoginField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = Icons.Outlined.Lock,
            isError = submitted && !passwordValid,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        if (passwordVisible) "Hide password" else "Show password",
                        tint = SecondaryText,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        if (submitted && !passwordValid) {
            LoginError("Enter your password")
        } else {
            Spacer(Modifier.height(24.dp))
        }

        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                .background(if (loading || !firebaseConfigured) BrandGreen.copy(alpha = .45f) else BrandGreen)
                .clickable(enabled = !loading && firebaseConfigured, onClick = submit)
                .semantics { contentDescription = "Log in" },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (loading) "Signing in…" else "Log in", color = AppBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        errorMessage?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = DestructiveRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(54.dp))
        Text(
            if (firebaseConfigured) "Administrator-invited employees only"
            else "Firebase is not configured in this build. Add app/google-services.json.",
            color = SecondaryText,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = label },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
        cursorBrush = SolidColor(BrandGreen),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(ComponentSurface)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(leadingIcon, null, tint = if (isError) DestructiveRed else SecondaryText, modifier = Modifier.size(23.dp))
                Spacer(Modifier.size(13.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(label, color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

@Composable
private fun LoginError(text: String) {
    Text(
        text,
        color = DestructiveRed,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 5.dp, bottom = 4.dp),
    )
}

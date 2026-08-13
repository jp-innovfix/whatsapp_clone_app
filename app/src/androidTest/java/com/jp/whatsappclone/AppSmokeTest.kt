package com.jp.whatsappclone

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNeverDisplaysAnEmailPasswordGate() {
        composeRule.onAllNodesWithText("Welcome back").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Email").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Password").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Log in").assertCountEquals(0)
    }
}

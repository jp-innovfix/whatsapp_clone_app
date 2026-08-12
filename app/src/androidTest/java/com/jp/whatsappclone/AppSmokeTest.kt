package com.jp.whatsappclone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun logIn() {
        composeRule.onNodeWithContentDescription("Email").performTextInput("demo@innovfix.test")
        composeRule.onNodeWithContentDescription("Password").performTextInput("demo")
        composeRule.onNodeWithContentDescription("Log in").performClick()
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
    }

    @Test
    fun loginValidatesEmailAndPassword() {
        composeRule.onNodeWithContentDescription("Log in").performClick()
        composeRule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
        composeRule.onNodeWithText("Enter your password").assertIsDisplayed()
    }

    @Test
    fun mainTabsAreInteractive() {
        logIn()
        composeRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeRule.onNodeWithText("Calls").performClick()
        composeRule.onNodeWithText("Calls").assertIsDisplayed()
        composeRule.onNodeWithText("Updates").performClick()
        composeRule.onNodeWithText("Status").assertIsDisplayed()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.onNodeWithText("Last 7 days performance").assertIsDisplayed()
    }

    @Test
    fun newChatFabOpensContactPicker() {
        logIn()
        composeRule.onNodeWithContentDescription("New chat").performClick()
        composeRule.onNodeWithText("Select contact").assertIsDisplayed()
        composeRule.onNodeWithText("New group").assertIsDisplayed()
    }

    @Test
    fun longPressingChatShowsSelectionToolbar() {
        logIn()
        composeRule.onNodeWithText("Mia Kapoor").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Archive").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pin").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mute").assertIsDisplayed()
    }

    @Test
    fun groupHeaderOpensScrollableGroupInfo() {
        logIn()
        composeRule.onNodeWithText("INNOVFIX").performClick()
        composeRule.onNodeWithContentDescription("Open group info").performClick()
        composeRule.onNodeWithText("Add group description").assertIsDisplayed()
        composeRule.onNodeWithText("Add to lists").assertIsDisplayed()
    }

    @Test
    fun atSymbolShowsGroupMentionSuggestions() {
        logIn()
        composeRule.onNodeWithText("INNOVFIX").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("@")
        composeRule.onNodeWithText("Mention all members in this chat").assertIsDisplayed()
    }
}

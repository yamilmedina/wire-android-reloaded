/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.messagecomposer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.EditMessageBundle
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentOptions
import com.wire.android.ui.home.messagecomposer.attachment._AttachmentOptionsComponent
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import okio.Path

@Composable
fun MessageComposer(
    messageComposerState: MessageComposerInnerState,
    messagesList: @Composable () -> Unit,
    onSendTextMessage: (String, List<UiMention>, messageId: String?) -> Unit,
    onSendEditTextMessage: (EditMessageBundle) -> Unit,
    onSendAttachment: (AssetBundle?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean,
    interactionAvailability: InteractionAvailability,
    tempCachePath: Path,
    securityClassificationType: SecurityClassificationType,
    membersToMention: List<Contact>,
    onPingClicked: () -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?
) {
    val messageComposerStateHolder = _rememberMessageComposerStateHolder()

    MessageComposerTest(
        messageComposerStateHolder,
        messagesList
    )
}

@Composable
fun MessageComposerTest(
    messageComposerStateHolder: _MessageComposerStateHolder,
    messagesList: @Composable () -> Unit,
) {
    val movableMessageList = remember { movableContentOf(messagesList) }

    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        when (val active = messageComposerStateHolder.mwessageComposerState) {
            is _MessageComposerState._Active -> _ActiveMessageComposer(movableMessageList, active)
            is _MessageComposerState._InActive -> _InActiveMessageComposer(messagesList)
        }


//    BackHandler(messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
//        messageComposerState.hideAttachmentOptions()
//        messageComposerState.toInactive()
//    }
    }
}

@Composable
fun _InActiveMessageComposer(messagesList: @Composable () -> Unit, onTransistionToActive: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
            AdditionalOptionButton(
                isSelected = false,
                isEnabled = true,
                onClick = { onTransistionToActive(true) }
            )
        }

        Text(
            "Test",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { onTransistionToActive(false) }
        )
    }
}

@Composable
fun _ActiveMessageComposer(messageContent: @Composable () -> Unit, activeMessageComposerState: _MessageComposerState._Active) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

        Column(
            Modifier
                .fillMaxWidth()
                .height(currentScreenHeight)
        ) {
            // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
            var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }

            if (KeyboardHelper.isKeyboardVisible()) {
                val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
                val notKnownAndCalculated = keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                val knownAndDifferent = keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                if (notKnownAndCalculated || knownAndDifferent) {
                    keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                }
            }
            val attachmentOptionsVisible = activeMessageComposerState.test == _GeneralOptionItems.AttachFile
                    && !KeyboardHelper.isKeyboardVisible()

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
//                                                messageComposerState.focusManager.clearFocus()
//                                                messageComposerState.toInactive()
                                },
                                onDoubleTap = { /* Called on Double Tap */ },
                                onLongPress = { /* Called on Long Press */ },
                                onTap = { /* Called on Tap */ }
                            )
                        }
                        .background(color = colorsScheme().backgroundVariant)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    messageContent()
                }

                ComposingInput(MessageCompositionInputSize.COLLAPSED, {}, FocusRequester())
                AttachmentAndAdditionalOptionsMenuItems(
                    isMentionActive = false,
                    isFileSharingEnabled = false,
                    startMention = {},
                    modifier = Modifier
                )
            }

            // Box wrapping for additional options content
            // we want to offset the AttachmentOptionsComponent equal to where
            // the device keyboard is displayed, so that when the keyboard is closed,
            // we get the effect of overlapping it
            if (attachmentOptionsVisible) {
                _AttachmentOptionsComponent(
                    modifier = Modifier
                        .height(keyboardHeight.height)
                        .fillMaxWidth()
                        .background(colorsScheme().messageComposerBackgroundColor)
                )
            }
            // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
            // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
            // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.
            else if (activeMessageComposerState.test != _GeneralOptionItems.AttachFile && !KeyboardHelper.isKeyboardVisible()) {
                Box(
                    modifier = Modifier
                        .height(keyboardHeight.height)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AttachmentAndAdditionalOptionsMenuItems(
    isMentionActive: Boolean,
    isFileSharingEnabled: Boolean,
    startMention: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        Box(Modifier.wrapContentSize()) {
            MessageComposeActions(
                true,
                isMentionActive,
                false,
                isFileSharingEnabled,
                startMention,
                {},
                {}
            )
        }
    }
}

@Composable
fun ComposingInput(inputSize: MessageCompositionInputSize, onFocused: () -> Unit, focusRequester: FocusRequester) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.Bottom
    ) {
        _MessageComposerInput(
            messageText = TextFieldValue("Test"),
            onMessageTextChanged = { },
            singleLine = false,
            onFocusChanged = { isFocused -> if (isFocused) onFocused() },
            focusRequester = focusRequester,
            modifier = Modifier
                .weight(1f)
                .then(
                    when (inputSize) {
                        MessageCompositionInputSize.COLLAPSED -> Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)
                        MessageCompositionInputSize.EXPANDED -> Modifier.fillMaxHeight()
                    }
                )
        )
        MessageSendActions(
            onSendButtonClicked = {

            },
            sendButtonEnabled = true
        )
    }
}

@Composable
fun _MessageComposerInput(
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    singleLine: Boolean,
    onFocusChanged: (Boolean) -> Unit = {},
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { }
) {
    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = wireTextFieldColors(
            backgroundColor = Color.Transparent,
            borderColor = Color.Transparent,
            focusColor = Color.Transparent
        ),
        singleLine = singleLine,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
                .focusRequester(focusRequester)
        ),
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}

@Suppress("ComplexMethod", "ComplexCondition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageComposer(
    messagesContent: @Composable () -> Unit,
    messageComposerState: MessageComposerInnerState,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path,
    interactionAvailability: InteractionAvailability,
    membersToMention: List<Contact>,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    onSendAttachmentClicked: (AssetBundle?) -> Unit,
    securityClassificationType: SecurityClassificationType,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onSendButtonClicked: () -> Unit,
    onEditSaveButtonClicked: () -> Unit,
    onMentionPicked: (Contact) -> Unit,
    onPingClicked: () -> Unit
) {
    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        val transition = updateTransition(
            targetState = messageComposerState.messageComposeInputState,
            label = stringResource(R.string.animation_label_message_compose_input_state_transition)
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

            Column(
                Modifier
                    .fillMaxWidth()
                    .height(currentScreenHeight)
            ) {

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }
                val isKeyboardVisible = KeyboardHelper.isKeyboardVisible()
                if (isKeyboardVisible) {
                    val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
                    val notKnownAndCalculated = keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent = keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent) {
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                    }
                }
                val attachmentOptionsVisible =
                    messageComposerState.messageComposeInputState.attachmentOptionsDisplayed && !isKeyboardVisible


                LaunchedEffect(isKeyboardVisible) {
                    if (!isKeyboardVisible && !messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
                        if (!messageComposerState.messageComposeInputState.isEditMessage) {
                            messageComposerState.toInactive()
                        }
                        messageComposerState.focusManager.clearFocus()
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        messageComposerState.focusManager.clearFocus()
                                        messageComposerState.toInactive()
                                    },
                                    onDoubleTap = { /* Called on Double Tap */ },
                                    onLongPress = { /* Called on Long Press */ },
                                    onTap = { /* Called on Tap */ }
                                )
                            }
                            .background(color = colorsScheme().backgroundVariant)
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        messagesContent()
                        if (membersToMention.isNotEmpty()) {
                            MembersMentionList(
                                membersToMention = membersToMention,
                                onMentionPicked = onMentionPicked
                            )
                        }
                    }

                    MessageComposerInput(
                        transition = transition,
                        interactionAvailability = interactionAvailability,
                        isFileSharingEnabled = isFileSharingEnabled,
                        securityClassificationType = securityClassificationType,
                        messageComposeInputState = messageComposerState.messageComposeInputState,
                        quotedMessageData = messageComposerState.quotedMessageData,
                        membersToMention = membersToMention,
                        inputFocusRequester = messageComposerState.inputFocusRequester,
                        actions = remember(messageComposerState) {
                            MessageComposerInputActions(
                                onMessageTextChanged = messageComposerState::setMessageTextValue,
                                onMentionPicked = onMentionPicked,
                                onSendButtonClicked = onSendButtonClicked,
                                onToggleFullScreen = messageComposerState::toggleFullScreen,
                                onCancelReply = messageComposerState::cancelReply,
                                startMention = messageComposerState::startMention,
                                onPingClicked = onPingClicked,
                                onInputFocusChanged = { isFocused ->
                                    messageComposerState.messageComposeInputFocusChange(isFocused)
                                    if (isFocused) {
                                        messageComposerState.toActive()
                                        messageComposerState.hideAttachmentOptions()
                                    }
                                },
                                onAdditionalOptionButtonClicked = {
                                    messageComposerState.focusManager.clearFocus()
                                    messageComposerState.toActive()
                                    messageComposerState.showAttachmentOptions()
                                },
                                onEditSaveButtonClicked = onEditSaveButtonClicked,
                                onEditCancelButtonClicked = messageComposerState::closeEditToInactive
                            )
                        }
                    )
                }

                // Box wrapping for additional options content
                // we want to offset the AttachmentOptionsComponent equal to where
                // the device keyboard is displayed, so that when the keyboard is closed,
                // we get the effect of overlapping it
                if (attachmentOptionsVisible) {
                    AttachmentOptions(
                        attachmentInnerState = messageComposerState.attachmentInnerState,
                        onSendAttachment = onSendAttachmentClicked,
                        onMessageComposerError = onMessageComposerError,
                        isFileSharingEnabled = isFileSharingEnabled,
                        tempWritableImageUri = tempWritableImageUri,
                        tempWritableVideoUri = tempWritableVideoUri,
                        tempCachePath = tempCachePath,
                        modifier = Modifier
                            .height(keyboardHeight.height)
                            .fillMaxWidth()
                            .background(colorsScheme().messageComposerBackgroundColor)
                    )
                }
                // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
                // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
                // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.
                else if (!messageComposerState.messageComposeInputState.attachmentOptionsDisplayed && !isKeyboardVisible &&
                    keyboardHeight is KeyboardHeight.Known && messageComposerState.messageComposeInputState.inputFocused &&
                    interactionAvailability == InteractionAvailability.ENABLED
                ) {
                    Box(
                        modifier = Modifier
                            .height(keyboardHeight.height)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    BackHandler(messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
        messageComposerState.hideAttachmentOptions()
        messageComposerState.toInactive()
    }
}

@Composable
private fun MembersMentionList(
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (membersToMention.isNotEmpty()) Divider()
        LazyColumn(
            modifier = Modifier.background(colorsScheme().background),
            reverseLayout = true
        ) {
            membersToMention.forEach {
                if (it.membership != Membership.Service) {
                    item {
                        MemberItemToMention(
                            avatarData = it.avatarData,
                            name = it.name,
                            label = it.label,
                            membership = it.membership,
                            clickable = Clickable(enabled = true) { onMentionPicked(it) },
                            modifier = Modifier
                        )
                        Divider(
                            color = MaterialTheme.wireColorScheme.divider,
                            thickness = Dp.Hairline
                        )
                    }
                }
            }
        }
    }
}

sealed class KeyboardHeight(open val height: Dp) {
    object NotKnown : KeyboardHeight(DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET)
    data class Known(override val height: Dp) : KeyboardHeight(height)

    companion object {
        val DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET = 250.dp
    }
}

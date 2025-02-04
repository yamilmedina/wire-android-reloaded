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
 */
package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.logic.util.isPositiveNotNull

class MessageCompositionInputStateHolder(
    private val messageComposition: MutableState<MessageComposition>,
    selfDeletionTimer: State<SelfDeletionTimer>
) {
    var inputFocused: Boolean by mutableStateOf(false)
        private set

    private val messageType = derivedStateOf {
        if (selfDeletionTimer.value.duration.isPositiveNotNull()) {
            MessageType.SelfDeleting(selfDeletionTimer.value)
        } else {
            MessageType.Normal
        }
    }

    var inputType: MessageCompositionType by mutableStateOf(
        MessageCompositionType.Composing(
            messageCompositionState = messageComposition,
            messageType = messageType
        )
    )

    var inputVisibility by mutableStateOf(true)
        private set

    var inputState: MessageCompositionInputState by mutableStateOf(
        MessageCompositionInputState.INACTIVE
    )

    var inputSize by mutableStateOf(
        MessageCompositionInputSize.COLLAPSED
    )
        private set

    fun toInActive() {
        inputVisibility = true
        inputSize = MessageCompositionInputSize.COLLAPSED
        inputState = MessageCompositionInputState.INACTIVE
        clearFocus()
    }

    fun toActive(isFocused: Boolean) {
        inputVisibility = true
        inputSize = MessageCompositionInputSize.COLLAPSED
        inputState = MessageCompositionInputState.ACTIVE
        if (isFocused) requestFocus() else clearFocus()
    }

    fun toEdit() {
        inputType = MessageCompositionType.Editing(
            messageCompositionState = messageComposition,
            messageCompositionSnapShot = messageComposition.value
        )
        toActive(true)
    }

    fun toComposing() {
        inputType = MessageCompositionType.Composing(
            messageCompositionState = messageComposition,
            messageType = messageType
        )
        toActive(true)
    }

    fun toggleInputSize() {
        inputSize = if (inputSize == MessageCompositionInputSize.COLLAPSED) {
            MessageCompositionInputSize.EXPANDED
        } else {
            MessageCompositionInputSize.COLLAPSED
        }
    }

    fun clearFocus() {
        inputFocused = false
    }

    fun requestFocus() {
        inputFocused = true
    }

    fun show() {
        inputVisibility = true
    }

    fun hide() {
        inputVisibility = false
    }

    companion object {
        @Suppress("MagicNumber")
        fun saver(
            messageComposition: MutableState<MessageComposition>,
            selfDeletionTimer: State<SelfDeletionTimer>
        ): Saver<MessageCompositionInputStateHolder, *> = Saver(
            save = {
                listOf(
                    it.inputFocused,
                    it.inputVisibility,
                    it.inputState,
                )
            },
            restore = {
                MessageCompositionInputStateHolder(
                    messageComposition = messageComposition,
                    selfDeletionTimer = selfDeletionTimer
                ).apply {
                    inputFocused = it[0] as Boolean
                    inputVisibility = it[1] as Boolean
                    inputState = it[2] as MessageCompositionInputState
                }
            }
        )
    }
}

sealed class MessageCompositionType {
    @Composable
    open fun inputTextColor(): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = colorsScheme().secondaryText
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    @Composable
    open fun labelText(): String = stringResource(R.string.label_type_a_message)

    class Composing(messageCompositionState: MutableState<MessageComposition>, val messageType: State<MessageType>) :
        MessageCompositionType() {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

        @Composable
        override fun inputTextColor(): WireTextFieldColors = if (messageType.value is MessageType.SelfDeleting) {
            wireTextFieldColors(
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent,
                focusColor = Color.Transparent,
                placeholderColor = colorsScheme().primary
            )
        } else {
            super.inputTextColor()
        }

        @Composable
        override fun labelText(): String = if (messageType.value is MessageType.SelfDeleting) {
            stringResource(id = R.string.self_deleting_message_label)
        } else {
            super.labelText()
        }
    }

    class Editing(
        messageCompositionState: MutableState<MessageComposition>,
        val messageCompositionSnapShot: MessageComposition
    ) : MessageCompositionType() {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor

        val isEditButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText != messageCompositionSnapShot.messageText
        }
    }
}

enum class MessageCompositionInputSize {
    COLLAPSED,
    EXPANDED;
}

enum class MessageCompositionInputState {
    ACTIVE,
    INACTIVE
}

sealed class MessageType {
    object Normal : MessageType()
    data class SelfDeleting(val selfDeletionTimer: SelfDeletionTimer) : MessageType()
}

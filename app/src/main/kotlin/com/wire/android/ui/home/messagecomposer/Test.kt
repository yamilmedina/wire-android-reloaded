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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue


@Composable
fun _rememberMessageComposerStateHolder(): _MessageComposerStateHolder {
    val focusManager = LocalFocusManager.current
    val inputFocusRequester = remember { FocusRequester() }

    val _messageComposerStateHolder = remember { _MessageComposerStateHolder(focusManager, inputFocusRequester) }

    return _messageComposerStateHolder
}

class _MessageComposerStateHolder(
    val focusManager: FocusManager,
    val inputFocusRequester: FocusRequester,
) {

    val _messageComposition: _MessageComposition by mutableStateOf(_MessageComposition(TextFieldValue("")))
    var _messageComposerState: _MessageComposerState by mutableStateOf(
        _MessageComposerState._InActive(_messageComposition)
    )
        private set

    fun toActive(showAttachmentOption: Boolean) {
        _messageComposerState = _MessageComposerState._Active(
            _messageComposition = _messageComposition,
            _generalOptionItem = if (showAttachmentOption) _AdditionalOptionSubMenuState.AttachFile else _AdditionalOptionSubMenuState.None,
            _messageCompositionInputType = _MessageCompositionInputType.Composing,
            _messageCompositionInputSize = MessageCompositionInputSize.COLLAPSED,
            _additionalOptionsState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu()
        )

//        inputFocusRequester.requestFocus()
    }

    fun toInActive() {
        _messageComposerState = _MessageComposerState._InActive(_messageComposition)
    }

}

enum class MessageCompositionInputSize {
    COLLAPSED, // wrap content	    COLLAPSED, // wrap content
    EXPANDED; // fullscreen	    EXPANDED; // fullscreen
}

data class _MessageComposition(val text: TextFieldValue)

sealed class _MessageComposerState {

    data class _Active(
        val _messageComposition: _MessageComposition,
        private val _generalOptionItem: _AdditionalOptionSubMenuState,
        private val _messageCompositionInputType: _MessageCompositionInputType.Composing,
        private val _messageCompositionInputSize: MessageCompositionInputSize,
        private val _additionalOptionsState: AdditionalOptionMenuState,
    ) : _MessageComposerState() {

        var messageComposition: _MessageComposition by mutableStateOf(_messageComposition)

        var inputType: _MessageCompositionInputType by mutableStateOf(_messageCompositionInputType)

        var inputSize: MessageCompositionInputSize by mutableStateOf(_messageCompositionInputSize)

        val additionalOptionsState: AdditionalOptionMenuState by mutableStateOf(_additionalOptionsState)

        fun toEphemeralInputType() {
            inputType = _MessageCompositionInputType.Ephemeral
        }

        fun messageTextChanged(it: TextFieldValue) {
            messageComposition = messageComposition.copy(it)
        }
    }

    data class _InActive(val _messageComposition: _MessageComposition) : _MessageComposerState()

}

sealed class AdditionalOptionMenuState {
    abstract var dupaJasia: _AdditionalOptionSubMenuState

    class AttachmentAndAdditionalOptionsMenu : AdditionalOptionMenuState() {

        override var dupaJasia: _AdditionalOptionSubMenuState by mutableStateOf(_AdditionalOptionSubMenuState.None)

        fun toggleAttachmentMenu() {
            dupaJasia = if (dupaJasia == _AdditionalOptionSubMenuState.AttachFile) {
                _AdditionalOptionSubMenuState.None
            } else {
                _AdditionalOptionSubMenuState.AttachFile
            }
        }

        fun toggleGifMenu() {
            dupaJasia = if (dupaJasia == _AdditionalOptionSubMenuState.Gif) {
                _AdditionalOptionSubMenuState.None
            } else {
                _AdditionalOptionSubMenuState.Gif
            }
        }

    }

    class RichTextEditing : AdditionalOptionMenuState() {
        override var dupaJasia: _AdditionalOptionSubMenuState by mutableStateOf(_AdditionalOptionSubMenuState.None)
    }

}

data class _MessageCompositionInputState(
    private val _messageCompositionInputType: _MessageCompositionInputType,
    private val _messageCompositionInputSize: MessageCompositionInputSize
) {

    var inputType: _MessageCompositionInputType by mutableStateOf(_messageCompositionInputType)

    var inputSize: MessageCompositionInputSize by mutableStateOf(_messageCompositionInputSize)

    fun toggleInputSize() {
        inputSize = if (inputSize == MessageCompositionInputSize.COLLAPSED) {
            MessageCompositionInputSize.EXPANDED
        } else {
            MessageCompositionInputSize.COLLAPSED
        }
    }
}

sealed class _MessageCompositionInputType {
    object Composing : _MessageCompositionInputType()
    object Editing : _MessageCompositionInputType()
    object Ephemeral : _MessageCompositionInputType()
}

enum class _AdditionalOptionSubMenuState {
    None,
    AttachFile,
    RecordAudio,
    AttachImage,
    Emoji,
    Gif;
}


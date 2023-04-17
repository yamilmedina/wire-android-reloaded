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

    /*    var mwessageComposerState: _MessageComposerState by mutableStateOf(_MessageComposerState._Active(
            messageComposition = _MessageComposition._Empty,
            _generalOptionItem = _GeneralOptionItems.AttachFile ,
            _messageCompositionInputType = _MessageCompositionInputType.Composing,
            _messageCompositionInputSize = MessageCompositionInputSize.COLLAPSED
        ))*/

    var _messageComposerState: _MessageComposerState by mutableStateOf(
        _MessageComposerState._InActive(_MessageComposition._Empty)
    )
        private set

    fun toActive(showAttachmentOption: Boolean) {
        _messageComposerState = _MessageComposerState._Active(
            messageComposition = _MessageComposition._Empty,
            _generalOptionItem = if (showAttachmentOption) _AttachmentAndAdditionalOptionsSubMenuItems.AttachFile else _AttachmentAndAdditionalOptionsSubMenuItems.None,
            _messageCompositionInputType = _MessageCompositionInputType.Composing,
            _messageCompositionInputSize = MessageCompositionInputSize.COLLAPSED,
            _additionalOptionsState = AdditionalOptionState.AttachmentAndAdditionalOptions()
        )

//        inputFocusRequester.requestFocus()
    }

    fun toInActive() {
        _messageComposerState = _MessageComposerState._InActive.DEFAULT
    }

}

enum class MessageCompositionInputSize {
    COLLAPSED, // wrap content	    COLLAPSED, // wrap content
    EXPANDED; // fullscreen	    EXPANDED; // fullscreen
}

sealed class _MessageComposerState {

    abstract val messageComposition: _MessageComposition

    data class _Active(
        override val messageComposition: _MessageComposition,
        private val _generalOptionItem: _AttachmentAndAdditionalOptionsSubMenuItems,
        private val _messageCompositionInputType: _MessageCompositionInputType.Composing,
        private val _messageCompositionInputSize: MessageCompositionInputSize,
        private val _additionalOptionsState: AdditionalOptionState,
    ) : _MessageComposerState() {

        var inputType: _MessageCompositionInputType by mutableStateOf(_messageCompositionInputType)

        var inputSize: MessageCompositionInputSize by mutableStateOf(_messageCompositionInputSize)

        val additionalOptionsState by mutableStateOf(_additionalOptionsState)
    }

    data class _InActive(override val messageComposition: _MessageComposition) : _MessageComposerState() {
        companion object {
            val DEFAULT = _InActive(_MessageComposition._Empty)
        }
    }

}

sealed class AdditionalOptionState {
    abstract var dupaJasia: _AttachmentAndAdditionalOptionsSubMenuItems

    class AttachmentAndAdditionalOptions : AdditionalOptionState() {

        override var dupaJasia: _AttachmentAndAdditionalOptionsSubMenuItems by mutableStateOf(_AttachmentAndAdditionalOptionsSubMenuItems.None)

        fun toggleAttachmentMenu() {
            dupaJasia = if (dupaJasia == _AttachmentAndAdditionalOptionsSubMenuItems.AttachFile) {
                _AttachmentAndAdditionalOptionsSubMenuItems.None
            } else {
                _AttachmentAndAdditionalOptionsSubMenuItems.AttachFile
            }
        }

        fun toggleGifMenu() {
            dupaJasia = if (dupaJasia == _AttachmentAndAdditionalOptionsSubMenuItems.Gif) {
                _AttachmentAndAdditionalOptionsSubMenuItems.None
            } else {
                _AttachmentAndAdditionalOptionsSubMenuItems.Gif
            }
        }


    }

    class RichTextEditing : AdditionalOptionState() {
        override var dupaJasia: _AttachmentAndAdditionalOptionsSubMenuItems by mutableStateOf(_AttachmentAndAdditionalOptionsSubMenuItems.None)
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
    object SelfDeleting : _MessageCompositionInputType()
}

enum class _AttachmentAndAdditionalOptionsSubMenuItems {
    None,
    AttachFile,
    Emoji,
    Gif;
}

sealed class _MessageComposition() {
    companion object {
        val DEFAULT = _Empty
    }

    data class _TextComposition(val messageText: TextFieldValue) : _MessageComposition()
    object _AssetComposition : _MessageComposition()
    object _Empty : _MessageComposition()

}

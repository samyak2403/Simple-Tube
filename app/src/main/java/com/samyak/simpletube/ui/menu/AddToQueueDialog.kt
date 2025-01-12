package com.samyak.simpletube.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.ListThumbnailSize
import com.samyak.simpletube.models.MultiQueueObject
import com.samyak.simpletube.playback.PlayerConnection.Companion.queueBoard
import com.samyak.simpletube.ui.component.ListDialog
import com.samyak.simpletube.ui.component.ListItem
import com.samyak.simpletube.ui.component.QueueListItem
import com.samyak.simpletube.ui.component.TextFieldDialog


@Composable
fun AddToQueueDialog(
    isVisible: Boolean,
    initialTextFieldValue: String? = null,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var queues by remember {
        mutableStateOf(emptyList<MultiQueueObject>())
    }
    var showCreateQueueDialog by rememberSaveable {
        mutableStateOf(false)
    }


    LaunchedEffect(Unit) {
        queues = queueBoard.getAllQueues().reversed()
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.create_queue),
                    thumbnailContent = {
                        Image(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable {
                        showCreateQueueDialog = true
                    }
                )
            }

            var index = queues.size
            // add queue
            items(queues) { queue ->
                QueueListItem(
                    queue = queue,
                    number = index--,
                    modifier = Modifier.clickable {
                        onAdd(queue.title)
                        onDismiss()
                    }
                )
            }
        }
    }

    if (showCreateQueueDialog) {
        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
            title = { Text(text = stringResource(R.string.create_queue)) },
            initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
            onDismiss = { showCreateQueueDialog = false },
            onDone = { queuetName ->
                onAdd(queuetName)
                onDismiss()
            },
        )
    }
}
package com.emm.hello.features.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.emm.hello.R

@Composable
fun ColumnScope.MainToolbar(
    navigateToBackup: () -> Unit,
    switchVisualization: () -> Unit,
    wordSearch: String,
    onWordSearchUpdate: (String) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager
) {
    val showSearchBar = remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "Learning",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = switchVisualization) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = navigateToBackup) {
            Icon(
                painter = painterResource(R.drawable.outline_save_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        IconButton(onClick = {
            showSearchBar.value = !showSearchBar.value
        }) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }

    AnimatedVisibility(showSearchBar.value) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = wordSearch,
            singleLine = true,
            onValueChange = {
                onWordSearchUpdate(it)
            },
            label = {
                Text("Search Word")
            },
            trailingIcon = {
                LeadingIcon {
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Unspecified,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )
    }
}

@Composable
private fun LeadingIcon(onSave: () -> Unit) {
    IconButton(onClick = { onSave() }) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
        )
    }
}
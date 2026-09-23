package com.example.mavlinkapplication.ui.params

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mavlinkapplication.domain.VehicleParam

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamScreen(
    viewModel: ParamViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val filteredParams by viewModel.filteredParams.collectAsStateWithLifecycle()
    val filter         by viewModel.filter.collectAsStateWithLifecycle()
    val isLoading      by viewModel.isLoading.collectAsStateWithLifecycle()

    var editingParam by remember { mutableStateOf<VehicleParam?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parameters") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("← Back") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = filter,
                onValueChange = viewModel::setFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search parameters…") },
                singleLine = true,
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredParams, key = { it.id }) { param ->
                        ParamRow(param = param, onClick = { editingParam = param })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editingParam?.let { param ->
        EditParamDialog(
            param = param,
            onConfirm = { newValue ->
                viewModel.setParam(param.id, newValue)
                editingParam = null
            },
            onDismiss = { editingParam = null },
        )
    }
}

@Composable
private fun ParamRow(param: VehicleParam, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(param.id, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            Text(
                text = "%.4f".format(param.value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun EditParamDialog(
    param: VehicleParam,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var valueText by remember { mutableStateOf(param.value.toString()) }
    val parsed = valueText.toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(param.id) },
        text = {
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Value") },
                isError = parsed == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text("SET")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

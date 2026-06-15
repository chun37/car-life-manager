package com.chun.carlife.ui.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chun.carlife.data.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclePicker(
    vehicles: List<Vehicle>,
    selected: Vehicle?,
    onSelect: (Vehicle) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "車両",
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selected?.name ?: "選択してください",
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vehicles.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.name) },
                    onClick = {
                        onSelect(v)
                        expanded = false
                    },
                )
            }
        }
    }
}

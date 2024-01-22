package me.timeto.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.timeto.app.c
import me.timeto.app.rememberVM
import me.timeto.shared.db.ChecklistDb
import me.timeto.shared.vm.ChecklistsPickerSheetVM

@Composable
fun ChecklistsPickerSheet(
    layer: WrapperView.Layer,
    selectedChecklists: List<ChecklistDb>,
    onPick: (List<ChecklistDb>) -> Unit,
) {

    val (vm, state) = rememberVM { ChecklistsPickerSheetVM(selectedChecklists) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(c.sheetBg)
    ) {

        val scrollState = rememberScrollState()

        Sheet.HeaderViewOld(
            onCancel = { layer.close() },
            title = state.headerTitle,
            doneText = state.doneTitle,
            isDoneEnabled = true,
            scrollState = scrollState,
        ) {
            onPick(vm.getSelectedChecklists())
            layer.close()
        }

        Column(
            modifier = Modifier
                .verticalScroll(state = scrollState)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {

            Row(Modifier.height(20.dp)) { }

            val checklistsUI = state.checklistsUI
            checklistsUI.forEach { checklistUI ->
                val isFirst = state.checklistsUI.first() == checklistUI
                MyListView__ItemView(
                    isFirst = isFirst,
                    isLast = state.checklistsUI.last() == checklistUI,
                    withTopDivider = !isFirst,
                ) {
                    MyListView__ItemView__CheckboxView(
                        text = checklistUI.text,
                        isChecked = checklistUI.isSelected,
                    ) {
                        vm.toggleChecklist(checklistUI)
                    }
                }
            }
        }
    }
}

package com.codingroadmap.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ReaderDialogs(
    showDisplay: Boolean,
    showNote: Boolean,
    textScale: Float,
    note: String,
    onNoteChange: (String) -> Unit,
    onCloseDisplay: () -> Unit,
    onCloseNote: () -> Unit,
    onTextScale: (Float) -> Unit,
    onSaveNote: () -> Unit
) {
    if (showDisplay) {
        AlertDialog(
            onDismissRequest = onCloseDisplay,
            title = { Text("읽기 설정") },
            text = {
                Column {
                    Text("글자 크기")
                    Slider(value = textScale, onValueChange = onTextScale, valueRange = .9f..1.35f)
                    Text("본문 리더는 항상 다크모드로 열립니다.")
                }
            },
            confirmButton = { TextButton(onClick = onCloseDisplay) { Text("완료") } }
        )
    }
    if (showNote) {
        AlertDialog(
            onDismissRequest = onCloseNote,
            title = { Text("내 메모") },
            text = { OutlinedTextField(value = note, onValueChange = onNoteChange, minLines = 4, label = { Text("이 챕터에 메모") }) },
            confirmButton = { TextButton(onClick = onSaveNote) { Text("저장") } },
            dismissButton = { TextButton(onClick = onCloseNote) { Text("취소") } }
        )
    }
}

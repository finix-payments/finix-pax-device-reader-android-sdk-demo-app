package com.finix.paxdevicereaderapplication.ui.signature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet that lets the customer draw a signature. Emits the captured signature as a
 * Base64-encoded PNG via [onConfirm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureBottomSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val points = remember { emptyList<Offset>().toMutableStateList() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            SignatureCanvas(
                points = points,
                onSizeChanged = { canvasSize = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )

            Button(
                onClick = {
                    onConfirm(renderSignatureToBase64(points, canvasSize.width, canvasSize.height))
                },
                enabled = points.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Submit")
            }

            OutlinedButton(
                onClick = { points.clear() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun SignatureCanvas(
    points: SnapshotStateList<Offset>,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .background(Color.LightGray)
            .pointerInput(Unit) {
                onSizeChanged(size)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    points.add(down.position.clampTo(size.width, size.height))
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()
                        if (change.pressed) {
                            points.add(change.position.clampTo(size.width, size.height))
                        } else {
                            points.add(PEN_UP)
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        drawPath(
            path = signatureGuideline(size.width, size.height),
            color = Color(0xFF7D90A5),
            style = Stroke(width = 3f),
        )
        drawPath(
            path = points.toSignaturePath(),
            color = Color.Black,
            style = Stroke(width = 5f),
        )
    }
}

private fun Offset.clampTo(width: Int, height: Int): Offset =
    Offset(x.coerceIn(0f, width.toFloat()), y.coerceIn(0f, height.toFloat()))

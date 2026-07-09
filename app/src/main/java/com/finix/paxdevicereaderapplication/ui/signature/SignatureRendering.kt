package com.finix.paxdevicereaderapplication.ui.signature

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

/** Sentinel offset used to mark a break in the stroke (pen lifted). */
val PEN_UP: Offset = Offset(-1f, -1f)

private const val SIGNATURE_STROKE_WIDTH = 5f
private const val GUIDELINE_STROKE_WIDTH = 3f

/**
 * Builds a smooth [Path] from a list of captured points, treating [PEN_UP] entries as breaks that
 * start a new sub-path. Uses quadratic segments for smoothing.
 */
fun List<Offset>.toSignaturePath(): Path {
    val path = Path()
    forEachIndexed { index, point ->
        if (point == PEN_UP) return@forEachIndexed
        val previous = getOrNull(index - 1)
        when {
            index == 0 || previous == null || previous == PEN_UP -> path.moveTo(point.x, point.y)
            else -> path.quadraticTo(
                previous.x,
                previous.y,
                (point.x + previous.x) / 2f,
                (point.y + previous.y) / 2f,
            )
        }
    }
    return path
}

/** The horizontal guideline three-quarters of the way down the signing area. */
fun signatureGuideline(width: Float, height: Float): Path {
    val y = height * 0.75f
    val inset = 10f
    return Path().apply {
        moveTo(inset, y)
        lineTo(width - inset, y)
    }
}

/**
 * Renders the captured [points] (and guideline) into a PNG and returns it Base64-encoded, or an
 * empty string if there is nothing to render.
 */
fun renderSignatureToBase64(points: List<Offset>, width: Int, height: Int): String {
    if (points.isEmpty() || width <= 0 || height <= 0) return ""
    val bitmap = renderSignatureBitmap(points, width, height)
    return bitmap.toBase64Png()
}

private fun renderSignatureBitmap(points: List<Offset>, width: Int, height: Int): Bitmap {
    val imageBitmap = ImageBitmap(width, height)
    val canvas = Canvas(imageBitmap)

    val guidelinePaint = Paint().apply {
        isAntiAlias = true
        color = Color.LightGray
        strokeWidth = GUIDELINE_STROKE_WIDTH
        style = PaintingStyle.Stroke
    }
    val signaturePaint = Paint().apply {
        isAntiAlias = true
        color = Color.Black
        strokeWidth = SIGNATURE_STROKE_WIDTH
        style = PaintingStyle.Stroke
    }

    canvas.drawPath(signatureGuideline(width.toFloat(), height.toFloat()), guidelinePaint)
    canvas.drawPath(points.toSignaturePath(), signaturePaint)

    return imageBitmap.asAndroidBitmap()
}

private fun Bitmap.toBase64Png(): String {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

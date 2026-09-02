package com.swap.handdrawing

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel

class DrawingViewModel : ViewModel() {

    val paths = mutableStateListOf<PathData>()
    val undonePaths = mutableStateListOf<PathData>()

    var currentPath by mutableStateOf<Path?>(null)
        private set

    var pathUpdateTrigger by mutableLongStateOf(0L)
        private set

    var isEraserMode by mutableStateOf(false)
        private set

    var selectedColor by mutableStateOf(Color.Black)
        private set

    var strokeWidth by mutableFloatStateOf(8f)
        private set

    var paperStyle by mutableStateOf(PaperStyle.PLAIN)
        private set

    var backgroundImage by mutableStateOf<Bitmap?>(null)
        private set

    var showBrushSettingsSheet by mutableStateOf(false)

    var showClearConfirmDialog by mutableStateOf(false)

    private var previousPoint: Offset? = null
    private var activePathStrokeWidth = 8f
    private var activePathIsEraser = false
    private var activePathColor = Color.Black

    fun startPath(offset: Offset) {
        undonePaths.clear()
        activePathStrokeWidth = if (isEraserMode) 60f else strokeWidth
        activePathIsEraser = isEraserMode
        activePathColor = selectedColor

        val newPath = Path().apply {
            moveTo(offset.x, offset.y)
        }
        currentPath = newPath
        previousPoint = offset
        pathUpdateTrigger++
    }

    fun movePath(offset: Offset) {
        val prev = previousPoint ?: return
        val current = currentPath ?: return

        val midX = (prev.x + offset.x) / 2f
        val midY = (prev.y + offset.y) / 2f

        current.quadraticTo(prev.x, prev.y, midX, midY)
        previousPoint = offset
        pathUpdateTrigger++
    }

    fun endPath() {
        currentPath?.let { path ->
            paths.add(
                PathData(
                    path = path,
                    color = activePathColor,
                    strokeWidth = activePathStrokeWidth,
                    isEraser = activePathIsEraser
                )
            )
        }
        currentPath = null
        previousPoint = null
        pathUpdateTrigger++
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            undonePaths.add(paths.removeAt(paths.size - 1))
            pathUpdateTrigger++
        }
    }

    fun redo() {
        if (undonePaths.isNotEmpty()) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            pathUpdateTrigger++
        }
    }

    fun clearCanvas() {
        paths.clear()
        undonePaths.clear()
        currentPath = null
        backgroundImage = null
        pathUpdateTrigger++
    }

    fun toggleEraserMode() {
        isEraserMode = !isEraserMode
        if (isEraserMode) {
            showBrushSettingsSheet = false
        }
    }

    fun updateSelectedColor(color: Color) {
        selectedColor = color
        if (isEraserMode) {
            isEraserMode = false
        }
    }

    fun updateStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun updatePaperStyle(style: PaperStyle) {
        paperStyle = style
    }

    fun updateBackgroundImage(bitmap: Bitmap?) {
        backgroundImage = bitmap
        pathUpdateTrigger++
    }
}

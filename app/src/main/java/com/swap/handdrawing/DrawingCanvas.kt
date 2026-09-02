package com.swap.handdrawing

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

data class PathData(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class PaperStyle {
    PLAIN, GRID, DOTS
}

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    paths: List<PathData>,
    currentPath: Path?,
    pathUpdateTrigger: Long,
    currentPathColor: Color,
    currentPathStrokeWidth: Float,
    isEraserMode: Boolean,
    paperStyle: PaperStyle = PaperStyle.PLAIN,
    onPathStarted: (Offset) -> Unit,
    onPathMoved: (Offset) -> Unit,
    onPathEnded: () -> Unit,
    backgroundImage: Bitmap? = null
) {
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var cachedCanvas by remember { mutableStateOf<androidx.compose.ui.graphics.Canvas?>(null) }

    // Transformation state for Zoom and Pan
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    // Cache state to track what's already drawn in the bitmap
    class CacheState {
        var drawnPathsCount = 0
        var lastBackgroundImage: Bitmap? = null
        var lastCachedSize = IntSize.Zero
    }
    val cacheState = remember { CacheState() }

    // Synchronize the cache with the current state
    fun updateCache(width: Int, height: Int) {
        val canvas = cachedCanvas ?: return
        
        val needsFullRedraw = cacheState.drawnPathsCount > paths.size || 
                            backgroundImage != cacheState.lastBackgroundImage || 
                            cacheState.lastCachedSize.width != width || 
                            cacheState.lastCachedSize.height != height

        if (needsFullRedraw) {
            // Clear and redraw everything
            canvas.nativeCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            
            backgroundImage?.let { bg ->
                canvas.nativeCanvas.drawBitmap(
                    bg, 
                    null,
                    Rect(0, 0, width, height),
                    null
                )
            }
            
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            
            paths.forEach { pathData ->
                paint.strokeWidth = pathData.strokeWidth
                if (pathData.isEraser) {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    paint.xfermode = null
                    paint.color = pathData.color.toArgb()
                }
                canvas.nativeCanvas.drawPath(pathData.path.asAndroidPath(), paint)
            }
            cacheState.drawnPathsCount = paths.size
        } else if (cacheState.drawnPathsCount < paths.size) {
            // Incremental draw: only draw new paths
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            
            for (i in cacheState.drawnPathsCount until paths.size) {
                val pathData = paths[i]
                paint.strokeWidth = pathData.strokeWidth
                if (pathData.isEraser) {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    paint.xfermode = null
                    paint.color = pathData.color.toArgb()
                }
                canvas.nativeCanvas.drawPath(pathData.path.asAndroidPath(), paint)
            }
            cacheState.drawnPathsCount = paths.size
        }
        
        cacheState.lastBackgroundImage = backgroundImage
        cacheState.lastCachedSize = IntSize(width, height)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            // Handle Zoom and Pan (Double finger)
            .transformable(state = transformState)
            // Handle Drawing (Single finger)
            .pointerInput(scale, offset) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    
                    // If more than one finger is down initially, don't start drawing
                    // We check the last change's pointer count
                    if (currentEvent.changes.size > 1) return@awaitEachGesture
                    
                    val canvasDown = (down.position - offset) / scale
                    onPathStarted(canvasDown)
                    
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        
                        // If multi-touch starts during drawing, finish the path
                        if (event.changes.size > 1) {
                            onPathEnded()
                            break
                        }

                        if (anyPressed) {
                            val change = event.changes.first()
                            val canvasPos = (change.position - offset) / scale
                            onPathMoved(canvasPos)
                            change.consume()
                        } else {
                            onPathEnded()
                            break
                        }
                    }
                }
            }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        // Observer pathUpdateTrigger to force recomposition
        val _trigger = pathUpdateTrigger
        
        val width = size.width.toInt()
        val height = size.height.toInt()

        if (cachedBitmap == null || cachedBitmap!!.width != width || cachedBitmap!!.height != height) {
            val newBitmap = ImageBitmap(width, height)
            cachedBitmap = newBitmap
            cachedCanvas = androidx.compose.ui.graphics.Canvas(newBitmap)
        }

        updateCache(width, height)

        // Apply Zoom and Pan transformations
        withTransform({
            translate(offset.x, offset.y)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // Draw Paper Style
            when (paperStyle) {
                PaperStyle.GRID -> {
                    val step = 40.dp.toPx()
                    for (x in 0..(width / step).toInt()) {
                        drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(x * step, 0f), Offset(x * step, size.height))
                    }
                    for (y in 0..(height / step).toInt()) {
                        drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, y * step), Offset(size.width, y * step))
                    }
                }
                PaperStyle.DOTS -> {
                    val step = 40.dp.toPx()
                    for (x in 0..(width / step).toInt()) {
                        for (y in 0..(height / step).toInt()) {
                            drawCircle(Color.LightGray.copy(alpha = 0.5f), radius = 2f, center = Offset(x * step, y * step))
                        }
                    }
                }
                PaperStyle.PLAIN -> {}
            }

            cachedBitmap?.let {
                drawImage(it)
            }

            currentPath?.let {
                drawPath(
                    path = it,
                    color = if (isEraserMode) Color.Transparent else currentPathColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = currentPathStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = if (isEraserMode) BlendMode.Clear else BlendMode.SrcOver
                )
            }
        }
    }
}

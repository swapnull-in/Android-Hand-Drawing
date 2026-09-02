package com.swap.handdrawing

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isEraserMode by remember { mutableStateOf(false) }
    var backgroundImage by remember { mutableStateOf<Bitmap?>(null) }
    var paperStyle by remember { mutableStateOf(PaperStyle.PLAIN) }
    
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    
    val drawingState = remember { DrawingState() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showBrushSettings by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                backgroundImage = BitmapFactory.decodeStream(stream)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            backgroundImage = it
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (_: Exception) {
                Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (_: Exception) {
                Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Drawing Pro", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { drawingState.undo() }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                },
                actions = {
                    IconButton(onClick = { drawingState.redo() }) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = {
                        drawingState.clear()
                        backgroundImage = null
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            Column {
                // Brush Settings Panel
                AnimatedVisibility(
                    visible = showBrushSettings,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    BrushSettingsPanel(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                        strokeWidth = strokeWidth,
                        onStrokeWidthChanged = { strokeWidth = it },
                        paperStyle = paperStyle,
                        onPaperStyleChanged = { paperStyle = it }
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { galleryLauncher.launch("image/*") },
                        icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Gallery") },
                        label = { Text("Gallery") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { launchCamera() },
                        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Camera") },
                        label = { Text("Camera") }
                    )
                    NavigationBarItem(
                        selected = showBrushSettings,
                        onClick = { 
                            showBrushSettings = !showBrushSettings 
                            if (showBrushSettings) isEraserMode = false
                        },
                        icon = { Icon(if (showBrushSettings) Icons.Default.Brush else Icons.Outlined.Brush, contentDescription = "Brush") },
                        label = { Text("Tools") }
                    )
                    NavigationBarItem(
                        selected = isEraserMode,
                        onClick = { 
                            isEraserMode = !isEraserMode 
                            if (isEraserMode) showBrushSettings = false
                        },
                        icon = { Icon(if (isEraserMode) Icons.Default.AutoFixHigh else Icons.Outlined.AutoFixOff, contentDescription = "Eraser") },
                        label = { Text("Eraser") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val uri = saveImageToMediaStore(context, drawingState.paths, backgroundImage, canvasSize, paperStyle)
                    if (uri != null) {
                        Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            DrawingCanvas(
                paths = drawingState.paths,
                currentPath = drawingState.currentPath,
                pathUpdateTrigger = drawingState.pathUpdateTrigger,
                currentPathColor = selectedColor,
                currentPathStrokeWidth = if (isEraserMode) 60f else strokeWidth,
                isEraserMode = isEraserMode,
                paperStyle = paperStyle,
                onPathStarted = { offset ->
                    drawingState.startPath(offset, if (isEraserMode) 60f else strokeWidth, isEraserMode, selectedColor)
                },
                onPathMoved = { offset ->
                    drawingState.movePath(offset)
                },
                onPathEnded = {
                    drawingState.endPath()
                },
                backgroundImage = backgroundImage
            )
            
            // Floating Share Button
            SmallFloatingActionButton(
                onClick = {
                    val uri = saveImageToMediaStore(context, drawingState.paths, backgroundImage, canvasSize, paperStyle)
                    if (uri != null) shareImage(context, uri)
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun BrushSettingsPanel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChanged: (Float) -> Unit,
    paperStyle: PaperStyle,
    onPaperStyleChanged: (PaperStyle) -> Unit
) {
    val colors = listOf(
        Color.Black, Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF388E3C), 
        Color(0xFFFBC02D), Color(0xFF7B1FA2), Color(0xFF0097A7), Color(0xFFE64A19),
        Color(0xFF5D4037), Color(0xFF455A64)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Colors", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(colors) { color ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == color) 4.dp else 0.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 1f..100f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("${strokeWidth.toInt()}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Canvas Style", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperStyleChip("Plain", paperStyle == PaperStyle.PLAIN) { onPaperStyleChanged(PaperStyle.PLAIN) }
                PaperStyleChip("Grid", paperStyle == PaperStyle.GRID) { onPaperStyleChanged(PaperStyle.GRID) }
                PaperStyleChip("Dots", paperStyle == PaperStyle.DOTS) { onPaperStyleChanged(PaperStyle.DOTS) }
            }
        }
    }
}

@Composable
fun PaperStyleChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null
    )
}

class DrawingState {
    val paths = mutableStateListOf<PathData>()
    val undonePaths = mutableStateListOf<PathData>()
    
    private var _currentPath by mutableStateOf<Path?>(null)
    val currentPath: Path? get() = _currentPath

    private var previousPoint: Offset? = null
    private var lastStrokeWidth: Float = 5f
    private var lastIsEraser: Boolean = false
    private var lastColor: Color = Color.Black

    var pathUpdateTrigger by mutableStateOf(0L)
        private set

    fun startPath(offset: Offset, strokeWidth: Float, isEraser: Boolean, color: Color) {
        undonePaths.clear()
        lastStrokeWidth = strokeWidth
        lastIsEraser = isEraser
        lastColor = color
        
        val newPath = Path().apply {
            moveTo(offset.x, offset.y)
        }
        _currentPath = newPath
        previousPoint = offset
        pathUpdateTrigger++
    }

    fun movePath(offset: Offset) {
        val prev = previousPoint ?: return
        val current = _currentPath ?: return

        val midX = (prev.x + offset.x) / 2
        val midY = (prev.y + offset.y) / 2
        
        current.quadraticTo(prev.x, prev.y, midX, midY)
        
        previousPoint = offset
        
        pathUpdateTrigger++
    }

    fun endPath() {
        _currentPath?.let {
            paths.add(PathData(it, lastColor, lastStrokeWidth, lastIsEraser))
        }
        _currentPath = null
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

    fun clear() {
        paths.clear()
        undonePaths.clear()
        _currentPath = null
        pathUpdateTrigger++
    }
}

fun saveImageToMediaStore(context: Context, paths: List<PathData>, backgroundImage: Bitmap?, size: IntSize, paperStyle: PaperStyle): Uri? {
    if (size.width <= 0 || size.height <= 0) return null

    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    // Draw Paper Style on Bitmap
    val paperPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        alpha = 50
        strokeWidth = 1f
    }
    
    when (paperStyle) {
        PaperStyle.GRID -> {
            val step = 100f // Larger step for high res bitmap
            for (x in 0..(size.width / step).toInt()) {
                canvas.drawLine(x * step, 0f, x * step, size.height.toFloat(), paperPaint)
            }
            for (y in 0..(size.height / step).toInt()) {
                canvas.drawLine(0f, y * step, size.width.toFloat(), y * step, paperPaint)
            }
        }
        PaperStyle.DOTS -> {
            val step = 100f
            for (x in 0..(size.width / step).toInt()) {
                for (y in 0..(size.height / step).toInt()) {
                    canvas.drawCircle(x * step, y * step, 5f, paperPaint)
                }
            }
        }
        PaperStyle.PLAIN -> {}
    }

    backgroundImage?.let {
        val src = android.graphics.Rect(0, 0, it.width, it.height)
        val dst = android.graphics.Rect(0, 0, size.width, size.height)
        canvas.drawBitmap(it, src, dst, null)
    }

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    paths.forEach { pathData ->
        if (pathData.isEraser) {
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        } else {
            paint.xfermode = null
            paint.color = pathData.color.toArgb()
        }
        paint.strokeWidth = pathData.strokeWidth
        canvas.drawPath(pathData.path.asAndroidPath(), paint)
    }

    val filename = "Drawing_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
        outputStream?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
    return uri
}

fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Drawing"))
}

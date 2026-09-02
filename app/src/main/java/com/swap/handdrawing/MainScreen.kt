package com.swap.handdrawing

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoFixOff
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DrawingViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val sheetState = rememberModalBottomSheetState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    viewModel.updateBackgroundImage(bitmap)
                }
            } catch (_: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to load image from gallery")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            try {
                context.contentResolver.openInputStream(photoUri!!)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    viewModel.updateBackgroundImage(bitmap)
                }
            } catch (_: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to load photo")
                }
            }
        }
    }

    fun launchCameraWithUri() {
        try {
            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            photoUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera unavailable on this device")
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraWithUri()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera permission is required")
            }
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraWithUri()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (viewModel.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showClearConfirmDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to clear your drawing and background? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCanvas()
                        viewModel.showClearConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Canvas cleared")
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Drawing Pro", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.paths.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (viewModel.paths.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.undonePaths.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (viewModel.undonePaths.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.showClearConfirmDialog = true },
                        enabled = viewModel.paths.isNotEmpty() || viewModel.backgroundImage != null
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear All"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
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
                    selected = viewModel.showBrushSettingsSheet,
                    onClick = {
                        viewModel.showBrushSettingsSheet = true
                    },
                    icon = { Icon(if (viewModel.showBrushSettingsSheet) Icons.Default.Brush else Icons.Outlined.Brush, contentDescription = "Tools") },
                    label = { Text("Tools") }
                )
                NavigationBarItem(
                    selected = viewModel.isEraserMode,
                    onClick = { viewModel.toggleEraserMode() },
                    icon = { Icon(if (viewModel.isEraserMode) Icons.Default.AutoFixHigh else Icons.Outlined.AutoFixOff, contentDescription = "Eraser") },
                    label = { Text("Eraser") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val uri = saveImageToMediaStore(
                        context = context,
                        paths = viewModel.paths,
                        backgroundImage = viewModel.backgroundImage,
                        size = canvasSize,
                        paperStyle = viewModel.paperStyle
                    )
                    if (uri != null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Drawing saved to Pictures")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Drawing")
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
                paths = viewModel.paths,
                currentPath = viewModel.currentPath,
                pathUpdateTrigger = viewModel.pathUpdateTrigger,
                currentPathColor = viewModel.selectedColor,
                currentPathStrokeWidth = if (viewModel.isEraserMode) 60f else viewModel.strokeWidth,
                isEraserMode = viewModel.isEraserMode,
                paperStyle = viewModel.paperStyle,
                onPathStarted = { offset -> viewModel.startPath(offset) },
                onPathMoved = { offset -> viewModel.movePath(offset) },
                onPathEnded = { viewModel.endPath() },
                backgroundImage = viewModel.backgroundImage
            )

            SmallFloatingActionButton(
                onClick = {
                    val uri = saveImageToMediaStore(
                        context = context,
                        paths = viewModel.paths,
                        backgroundImage = viewModel.backgroundImage,
                        size = canvasSize,
                        paperStyle = viewModel.paperStyle
                    )
                    if (uri != null) {
                        shareImage(context, uri)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
            }
        }

        if (viewModel.showBrushSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.showBrushSettingsSheet = false },
                sheetState = sheetState
            ) {
                BrushSettingsPanel(
                    selectedColor = viewModel.selectedColor,
                    onColorSelected = { color -> viewModel.updateSelectedColor(color) },
                    strokeWidth = viewModel.strokeWidth,
                    onStrokeWidthChanged = { width -> viewModel.updateStrokeWidth(width) },
                    paperStyle = viewModel.paperStyle,
                    onPaperStyleChanged = { style -> viewModel.updatePaperStyle(style) }
                )
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
        Color(0xFF5D4037), Color(0xFF455A64), Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Colors", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color) 3.5.dp else 1.dp,
                            color = if (selectedColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Brush Size", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((strokeWidth / 2f).coerceIn(4f, 28f).dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChanged,
                valueRange = 2f..80f,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("${strokeWidth.toInt()} px", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Canvas Style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaperStyleChip("Plain", paperStyle == PaperStyle.PLAIN) { onPaperStyleChanged(PaperStyle.PLAIN) }
            PaperStyleChip("Grid", paperStyle == PaperStyle.GRID) { onPaperStyleChanged(PaperStyle.GRID) }
            PaperStyleChip("Dots", paperStyle == PaperStyle.DOTS) { onPaperStyleChanged(PaperStyle.DOTS) }
            PaperStyleChip("Ruled", paperStyle == PaperStyle.RULED) { onPaperStyleChanged(PaperStyle.RULED) }
        }
        Spacer(modifier = Modifier.height(24.dp))
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

fun saveImageToMediaStore(
    context: Context,
    paths: List<PathData>,
    backgroundImage: Bitmap?,
    size: IntSize,
    paperStyle: PaperStyle
): Uri? {
    if (size.width <= 0 || size.height <= 0) return null

    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paperPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        alpha = 60
        strokeWidth = 2f
    }

    when (paperStyle) {
        PaperStyle.GRID -> {
            val step = 100f
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
                    canvas.drawCircle(x * step, y * step, 6f, paperPaint)
                }
            }
        }
        PaperStyle.RULED -> {
            val step = 120f
            for (y in 1..(size.height / step).toInt()) {
                canvas.drawLine(0f, y * step, size.width.toFloat(), y * step, paperPaint)
            }
            val redMarginPaint = Paint().apply {
                color = android.graphics.Color.RED
                alpha = 100
                strokeWidth = 4f
            }
            canvas.drawLine(150f, 0f, 150f, size.height.toFloat(), redMarginPaint)
        }
        PaperStyle.PLAIN -> {}
    }

    backgroundImage?.let {
        val src = Rect(0, 0, it.width, it.height)
        val dst = Rect(0, 0, size.width, size.height)
        canvas.drawBitmap(it, src, dst, null)
    }

    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    paths.forEach { pathData ->
        if (pathData.isEraser) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
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

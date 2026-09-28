package com.example.foodmemory.feature.dish

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import android.view.Surface
import android.view.WindowManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.foodmemory.R
import com.example.foodmemory.domain.model.DishIngredient
import com.example.foodmemory.domain.model.IngredientEvidence
import com.example.foodmemory.domain.model.NewDish
import com.example.foodmemory.feature.shared.decodeUprightBitmap
import com.example.foodmemory.feature.shared.normalizeJpegOrientation
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast

private data class IngredientDraft(val name: String, val evidence: IngredientEvidence)

@Composable
fun DishCaptureScreen(viewModel: DishCaptureViewModel, onConfirm: (NewDish) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureState by viewModel.state.collectAsStateWithLifecycle()
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var analysisRequested by rememberSaveable { mutableStateOf(false) }
    val analysis = captureState.analysis
    val isAnalyzing = captureState.isAnalyzing
    var cameraError by remember { mutableStateOf<String?>(null) }
    var dishName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val ingredients = remember { mutableStateListOf<IngredientDraft>() }
    var reviewError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(photoPath, analysisRequested, analysis, isAnalyzing) {
        val path = photoPath
        if (path != null && analysisRequested && analysis == null && !isAnalyzing) {
            viewModel.analyze(path)
        }
    }
    LaunchedEffect(analysis) {
        analysis?.let { result ->
            dishName = result.dishName
            description = result.description
            ingredients.clear()
            ingredients.addAll(result.ingredients.map { IngredientDraft(it.name, it.evidence) })
        }
    }
    LaunchedEffect(captureState.error) {
        if (captureState.error != null) analysisRequested = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onCancel, modifier = Modifier.size(44.dp)) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dish_capture_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.dish_capture_subtitle), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            !hasCameraPermission -> PermissionMessage(onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            photoPath == null -> {
                CameraPreview(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onCaptureReady = { capture = it },
                    onCaptureError = { cameraError = it }
                )
                cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                Button(
                    onClick = {
                        val imageCapture = capture ?: return@Button
                        if (isCapturing) return@Button
                        isCapturing = true
                        cameraError = null
                        imageCapture.targetRotation = currentDisplayRotation(context)
                        val outputFile = File(context.cacheDir, "dish-photo-${System.currentTimeMillis()}.jpg")
                        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                        try {
                            imageCapture.takePicture(
                                options,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { normalizeJpegOrientation(outputFile) }
                                                photoPath = outputFile.absolutePath
                                            } catch (_: Exception) {
                                                outputFile.delete()
                                                cameraError = context.getString(R.string.dish_capture_error)
                                            } finally {
                                                isCapturing = false
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        outputFile.delete()
                                        cameraError = context.getString(R.string.dish_capture_error)
                                        isCapturing = false
                                    }
                                }
                            )
                        } catch (_: IllegalStateException) {
                            outputFile.delete()
                            cameraError = context.getString(R.string.dish_capture_error)
                            isCapturing = false
                        }
                    },
                    enabled = capture != null && !isCapturing,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    if (isCapturing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.dish_capture_button))
                }
            }
            isAnalyzing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.dish_analyzing))
                }
            }
            analysis == null -> {
                Text(stringResource(R.string.dish_review_hint), style = MaterialTheme.typography.bodyMedium)
                PhotoPreview(photoPath!!, Modifier.weight(1f).fillMaxWidth())
                (captureState.error ?: cameraError)?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                OutlinedButton(onClick = { shareDishPhoto(context, photoPath!!) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dish_share_photo))
                }
                Button(onClick = { analysisRequested = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.dish_analyze_button)) }
                OutlinedButton(
                    onClick = {
                        File(photoPath!!).delete()
                        photoPath = null
                        analysisRequested = false
                        viewModel.clearAnalysis()
                        cameraError = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.dish_repeat_photo)) }
            }
            else -> {
                val result = analysis!!
                Text(stringResource(R.string.dish_review_hint), style = MaterialTheme.typography.bodyMedium)
                if (!result.isFood) {
                    Text(stringResource(R.string.dish_unknown_food), color = MaterialTheme.colorScheme.error)
                }
                if (result.uncertainty.isNotBlank()) {
                    Text(result.uncertainty, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    dishName,
                    { dishName = it; reviewError = null },
                    label = { Text(stringResource(R.string.dish_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text(stringResource(R.string.dish_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Text(stringResource(R.string.dish_ingredients), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.dish_ingredient_evidence), style = MaterialTheme.typography.bodySmall)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(ingredients) { index, ingredient ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = ingredient.name,
                                onValueChange = { ingredients[index] = ingredient.copy(name = it) },
                                label = { Text(if (ingredient.evidence == IngredientEvidence.VISIBLE) stringResource(R.string.dish_visible) else stringResource(R.string.dish_typical)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { ingredients.removeAt(index) }) { Text("×") }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { ingredients.add(IngredientDraft("", IngredientEvidence.TYPICAL)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.dish_add_ingredient)) }
                    }
                }
                reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        if (dishName.isBlank()) {
                            reviewError = context.getString(R.string.dish_name_error)
                        } else {
                            isSavingPhoto = true
                            scope.launch {
                                runCatching { viewModel.cachePhoto(photoPath!!) }
                                    .onSuccess { savedPhoto ->
                                        onConfirm(
                                            NewDish(
                                                name = dishName.trim(),
                                                rating = null,
                                                description = description.trim(),
                                                photoPath = savedPhoto,
                                                ingredients = ingredients.filter { it.name.isNotBlank() }
                                                    .map { DishIngredient(it.name.trim(), it.evidence) }
                                            )
                                        )
                                    }
                                    .onFailure { reviewError = context.getString(R.string.dish_photo_save_error) }
                                isSavingPhoto = false
                            }
                        }
                    },
                    enabled = !isSavingPhoto,
                    modifier = Modifier.fillMaxWidth()
                ) { if (isSavingPhoto) CircularProgressIndicator() else Text(stringResource(R.string.dish_confirm)) }
                OutlinedButton(onClick = { shareDishPhoto(context, photoPath!!) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dish_share_photo))
                }
                OutlinedButton(
                    onClick = {
                        File(photoPath!!).delete()
                        photoPath = null
                        analysisRequested = false
                        ingredients.clear()
                        viewModel.clearAnalysis()
                        cameraError = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.dish_repeat_photo)) }
            }
        }
    }
}

private fun shareDishPhoto(context: android.content.Context, photoPath: String) {
    val imageUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(photoPath)
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        clipData = android.content.ClipData.newUri(context.contentResolver, context.getString(R.string.dish_share_photo), imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage(INSTAGRAM_PACKAGE)
    }
    try {
        context.startActivity(sendIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.dish_instagram_unavailable), Toast.LENGTH_LONG).show()
    }
}

private const val INSTAGRAM_PACKAGE = "com.instagram.android"

@Suppress("DEPRECATION")
private fun currentDisplayRotation(context: Context): Int =
    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

@Composable
private fun PermissionMessage(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.dish_capture_permission))
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.dish_capture_allow)) }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    onCaptureReady: (ImageCapture?) -> Unit,
    onCaptureError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
    DisposableEffect(lifecycleOwner, previewView) {
        var cameraProvider: ProcessCameraProvider? = null
        var disposed = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (disposed) return@addListener
            try {
                cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                cameraProvider?.unbind(preview, imageCapture)
                cameraProvider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                onCaptureReady(imageCapture)
            } catch (_: Exception) {
                onCaptureError(context.getString(R.string.dish_camera_start_error))
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            cameraProvider?.unbindAll()
            onCaptureReady(null)
        }
    }
}

@Composable
private fun PhotoPreview(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = androidx.compose.runtime.produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { decodeUprightBitmap(path) }
    }.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = context.getString(R.string.dish_capture_title),
            modifier = modifier.padding(vertical = 12.dp)
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) { Text(stringResource(R.string.dish_photo_preview_error)) }
    }
}

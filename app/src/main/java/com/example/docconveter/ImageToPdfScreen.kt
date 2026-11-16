package com.example.docconveter

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var showPdfSuccessDialog by remember {
        mutableStateOf(false)
    }

    var createdPdfUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
            statusMessage = ""
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri ->

        if (outputUri != null) {
            isProcessing = true
            statusMessage = null

            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        createPdfFromImages(
                            context = context,
                            imageUris = selectedImages,
                            outputUri = outputUri
                        )
                    }
                }

                isProcessing = false

                statusMessage = if (result.isSuccess) {
                    "PDF created successfully!"
                } else {
                    "Unable to create PDF: ${
                        result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    }"
                }

                if (result.isSuccess) {
                    createdPdfUri = outputUri
                    showPdfSuccessDialog = true
                    selectedImages = emptyList()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Image to PDF",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF6F7FB)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(25.dp))

            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = Color(0xFF3157D5),
                modifier = Modifier.size(65.dp)
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Convert Images to PDF",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select multiple images and combine them into one PDF document.",
                fontSize = 14.sp,
                color = Color(0xFF737D91),
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedButton(
                onClick = {
                    imagePicker.launch(arrayOf("image/*"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text("Select Images")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selectedImages.size}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3157D5)
                    )

                    Text(
                        text = "Images selected",
                        color = Color(0xFF737D91)
                    )

                    if (selectedImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(15.dp))

                        selectedImages.forEachIndexed { index, _ ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Image ${index + 1}",
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "Selected",
                                    color = Color(0xFF16A34A),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                selectedImages = emptyList()
                                statusMessage = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Clear selection")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No images selected yet",
                            color = Color(0xFF737D91),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            Button(
                onClick = {
                    savePdfLauncher.launch("DocConveter_Images.pdf")
                },
                enabled = selectedImages.isNotEmpty() && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Creating PDF...")
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create PDF")
                }
            }

            statusMessage?.let { message ->
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = message,
                    color = if (message.startsWith("PDF created")) {
                        Color(0xFF168344)
                    } else {
                        Color(0xFFD32F2F)
                    },
                    fontSize = 13.sp
                )
            }
        }
    }
    if (showPdfSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showPdfSuccessDialog = false
            },
            title = {
                Text("PDF Created Successfully!")
            },
            text = {
                Text(
                    "Your PDF has been saved successfully. " +
                            "Would you like to open it now?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPdfSuccessDialog = false

                        createdPdfUri?.let { uri ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    uri,
                                    "application/pdf"
                                )
                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }

                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "No PDF viewer installed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Open PDF")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPdfSuccessDialog = false
                    }
                ) {
                    Text("Later")
                }
            }
        )
    }
}

private fun createPdfFromImages(
    context: Context,
    imageUris: List<Uri>,
    outputUri: Uri
) {
    require(imageUris.isNotEmpty()) {
        "Please select at least one image."
    }

    val pdfDocument = PdfDocument()

    try {
        imageUris.forEachIndexed { index, uri ->

            val bitmap = decodeImage(context, uri)

            try {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    595,
                    842,
                    index + 1
                ).create()

                val page = pdfDocument.startPage(pageInfo)

                try {
                    val margin = 28f
                    val pageWidth = 595f
                    val pageHeight = 842f

                    val availableWidth = pageWidth - 2 * margin
                    val availableHeight = pageHeight - 2 * margin

                    val scale = minOf(
                        availableWidth / bitmap.width.toFloat(),
                        availableHeight / bitmap.height.toFloat()
                    )

                    val scaledWidth = bitmap.width * scale
                    val scaledHeight = bitmap.height * scale

                    val left = (pageWidth - scaledWidth) / 2f
                    val top = (pageHeight - scaledHeight) / 2f

                    val destination = RectF(
                        left,
                        top,
                        left + scaledWidth,
                        top + scaledHeight
                    )

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        isFilterBitmap = true
                    }

                    page.canvas.drawBitmap(
                        bitmap,
                        null,
                        destination,
                        paint
                    )
                } finally {
                    pdfDocument.finishPage(page)
                }

            } finally {
                bitmap.recycle()
            }
        }

        // Write the completed PDF to the selected destination.
        val outputStream = requireNotNull(
            context.contentResolver.openOutputStream(outputUri, "wt")
        ) {
            "Unable to open the destination file."
        }

        outputStream.use { stream ->
            pdfDocument.writeTo(stream)
            stream.flush()
        }


    } finally {
        pdfDocument.close()
    }
}



private fun decodeImage(
    context: Context,
    uri: Uri
): Bitmap {

    val imageBytes = context.contentResolver
        .openInputStream(uri)
        ?.use { it.readBytes() }
        ?: throw IllegalStateException(
            "Cannot open image. Provider: ${uri.authority}"
        )

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeByteArray(
        imageBytes,
        0,
        imageBytes.size,
        bounds
    )

    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
        "Unsupported or corrupted image: ${uri.lastPathSegment}"
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(
            bounds.outWidth,
            bounds.outHeight
        )
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    return BitmapFactory.decodeByteArray(
        imageBytes,
        0,
        imageBytes.size,
        options
    ) ?: throw IllegalStateException(
        "Unable to decode image: ${uri.lastPathSegment}"
    )
}

private fun calculateSampleSize(
    width: Int,
    height: Int
): Int {
    var sampleSize = 1

    while (
        width / (sampleSize * 2) >= 1400 &&
        height / (sampleSize * 2) >= 1400
    ) {
        sampleSize *= 2
    }

    return sampleSize
}
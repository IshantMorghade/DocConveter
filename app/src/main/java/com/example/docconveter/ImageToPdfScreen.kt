package com.example.docconveter

import android.content.Context
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

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = uris
            statusMessage = null
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

                val pageWidth = 595f
                val pageHeight = 842f
                val margin = 28f

                val availableWidth = pageWidth - (margin * 2)
                val availableHeight = pageHeight - (margin * 2)

                val scale = minOf(
                    availableWidth / bitmap.width,
                    availableHeight / bitmap.height
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

                pdfDocument.finishPage(page)

            } finally {
                bitmap.recycle()
            }
        }

        val outputStream = requireNotNull(
            context.contentResolver.openOutputStream(outputUri)
        ) {
            "Cannot open the selected save location."
        }

        outputStream.use {
            pdfDocument.writeTo(it)
        }

    } finally {
        pdfDocument.close()
    }
}

private fun decodeImage(
    context: Context,
    uri: Uri
): Bitmap {

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    } ?: throw IllegalStateException("Cannot read selected image.")

    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
        "Invalid image file."
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(
            bounds.outWidth,
            bounds.outHeight
        )
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: throw IllegalStateException("Unable to decode image.")
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
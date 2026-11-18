
package com.example.docconveter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri

import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.DocumentsContract


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class PdfPreviewResult(
    val pageCount: Int,
    val previewBitmap: Bitmap
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfName by remember {
        mutableStateOf<String?>(null)
    }

    var pageCount by remember {
        mutableIntStateOf(0)
    }

    var previewBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }
    var isConverting by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var selectedPdfUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var firstImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var showSuccessDialog by remember {
        mutableStateOf(false)
    }
    var convertedPageCount by remember {
        mutableIntStateOf(0)
    }

    var pageSelectionMode by remember {
        mutableStateOf("All pages")
    }

    var pageSelectionInput by remember {
        mutableStateOf("")
    }

    // Start PDF conversion

    val convertPdf: (Uri, Uri) -> Unit = { pdfUri, folderUri ->

        val selectedPages = try {
            parsePageSelection(
                mode = pageSelectionMode,
                input = pageSelectionInput,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Invalid page selection."
            null
        }

        if (selectedPages != null) {

            isConverting = true
            errorMessage = null
            showSuccessDialog = false
            firstImageUri = null

            scope.launch {

                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        savePdfPagesAsImages(
                            context = context,
                            pdfUri = pdfUri,
                            folderUri = folderUri,
                            selectedPageIndexes = selectedPages
                        )
                    }
                }

                isConverting = false

                if (result.isSuccess) {

                    val conversion = result.getOrThrow()

                    firstImageUri = conversion.firstImageUri
                    convertedPageCount = conversion.pageCount
                    errorMessage = null

                    showSuccessDialog = true

                } else {

                    errorMessage =
                        "Conversion failed: ${
                            result.exceptionOrNull()?.localizedMessage
                                ?: "Unknown error"
                        }"
                }
            }
        }
    }


    // Ask user to choose an output folder
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->

        if (folderUri != null) {

            try {

                context.contentResolver.takePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                selectedPdfUri?.let { pdfUri ->
                    convertPdf(pdfUri, folderUri)
                }

            } catch (e: Exception) {

                errorMessage =
                    "Unable to access selected folder: ${
                        e.localizedMessage ?: "Permission error"
                    }"
            }
        }
    }

    // PDF picker
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {
            selectedPdfUri = uri

            selectedPdfName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?: "Selected PDF"

            previewBitmap = null
            pageCount = 0
            errorMessage = null
            isLoading = true

            scope.launch {

                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        loadPdfPreview(context, uri)
                    }
                }

                isLoading = false

                if (result.isSuccess) {

                    val pdfResult = result.getOrThrow()

                    pageCount = pdfResult.pageCount
                    previewBitmap = pdfResult.previewBitmap

                } else {

                    errorMessage =
                        result.exceptionOrNull()?.localizedMessage
                            ?: "Unable to open PDF."
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF3157D5),
            background = Color(0xFFF6F7FB),
            surface = Color.White
        )
    ) {


        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "PDF to Image",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(65.dp)
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "Convert PDF to Images",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Extract PDF pages as high-quality images.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Select PDF button
                OutlinedButton(
                    onClick = {
                        pdfPicker.launch(
                            arrayOf("application/pdf")
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text("Select PDF")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Loading indicator
                if (isLoading) {

                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Loading PDF...",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Selected PDF information
                if (selectedPdfName != null && !isLoading) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "Selected PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = selectedPdfName ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "$pageCount pages",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // First page preview
                previewBitmap?.let { bitmap ->

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "First Page Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF first page preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Page selection options
                if (previewBitmap != null && !isLoading) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Text(
                                text = "Select Pages",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            listOf(
                                "All pages",
                                "Page range",
                                "Individual pages"
                            ).forEach { option ->

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = pageSelectionMode == option,
                                        onClick = {
                                            pageSelectionMode = option
                                            pageSelectionInput = ""
                                            errorMessage = null
                                        }
                                    )

                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (pageSelectionMode != "All pages") {

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = pageSelectionInput,
                                    onValueChange = {
                                        pageSelectionInput = it
                                        errorMessage = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = {
                                        Text(
                                            if (pageSelectionMode == "Page range")
                                                "Page range"
                                            else
                                                "Page numbers"
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            if (pageSelectionMode == "Page range")
                                                "Example: 1-10"
                                            else
                                                "Example: 1,3,7,12"
                                        )
                                    },
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Total PDF pages: $pageCount",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Convert PDF Button
                if (previewBitmap != null && !isLoading) {

                    Spacer(modifier = Modifier.height(25.dp))

                    Button(
                        onClick = {

                            val selectedPages = try {
                                parsePageSelection(
                                    mode = pageSelectionMode,
                                    input = pageSelectionInput,
                                    pageCount = pageCount
                                )
                            } catch (e: Exception) {
                                errorMessage =
                                    e.localizedMessage ?: "Invalid page selection."
                                null
                            }

                            if (selectedPages != null) {
                                errorMessage = null

                                if (selectedPdfUri != null) {
                                    folderPicker.launch(null)
                                }
                            }
                        },
                        enabled = !isConverting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isConverting) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text("Converting...")

                        } else {

                            Text(
                                text = "Convert to Images",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Error message
                errorMessage?.let { message ->

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = message,
                        color = Color(0xFFEF5350),
                        fontSize = 13.sp
                    )
                }
            }
        }


        // Conversion Success Dialog
        if (showSuccessDialog) {

            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                },

                title = {
                    Text(
                        text = "Conversion Successful!",
                        fontWeight = FontWeight.Bold
                    )
                },

                text = {
                    Text(
                        text = "Successfully converted $convertedPageCount pages. " +
                                "Your images have been saved. Would you like to open the first image?"
                    )
                },

                confirmButton = {

                    TextButton(
                        onClick = {

                            showSuccessDialog = false

                            firstImageUri?.let { imageUri ->

                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(imageUri, "image/jpeg")
                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                                try {
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    errorMessage =
                                        "No image viewer is installed on this device."
                                }
                            }
                        }
                    ) {
                        Text("Open Image")
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                        }
                    ) {
                        Text("Later")
                    }
                }
            )
        }
    }
}


// Load PDF and render its first page
private fun loadPdfPreview(
    context: Context,
    uri: Uri
): PdfPreviewResult {

    val fileDescriptor = context.contentResolver
        .openFileDescriptor(uri, "r")
        ?: throw IllegalStateException(
            "Unable to open selected PDF."
        )

    fileDescriptor.use { descriptor ->

        val renderer = PdfRenderer(descriptor)

        try {

            val totalPages = renderer.pageCount

            require(totalPages > 0) {
                "This PDF does not contain any pages."
            }

            val page = renderer.openPage(0)

            try {

                val previewWidth = 1000

                val previewHeight = (
                        previewWidth.toFloat() *
                                page.height.toFloat() /
                                page.width.toFloat()
                        ).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(
                    previewWidth,
                    previewHeight,
                    Bitmap.Config.ARGB_8888
                )

                bitmap.eraseColor(AndroidColor.WHITE)

                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                return PdfPreviewResult(
                    pageCount = totalPages,
                    previewBitmap = bitmap
                )

            } finally {

                page.close()
            }

        } finally {

            renderer.close()
        }
    }
}


private data class PdfConversionResult(
    val pageCount: Int,
    val firstImageUri: Uri
)


private fun parsePageSelection(
    mode: String,
    input: String,
    pageCount: Int
): List<Int> {

    return PdfPageSelection.parsePageSelection(
        mode = mode,
        input = input,
        pageCount = pageCount
    )
}




private fun savePdfPagesAsImages(
    context: Context,
    pdfUri: Uri,
    folderUri: Uri,
    selectedPageIndexes: List<Int>
): PdfConversionResult {

    val descriptor = context.contentResolver
        .openFileDescriptor(pdfUri, "r")
        ?: throw IllegalStateException("Unable to open PDF.")

    descriptor.use { fileDescriptor ->

        val renderer = PdfRenderer(fileDescriptor)

        try {

            val totalPages = renderer.pageCount

            require(totalPages > 0) {
                "PDF contains no pages."
            }

            require(
                selectedPageIndexes.isNotEmpty() &&
                        selectedPageIndexes.all { it in 0 until totalPages }
            ) {
                "Invalid page selection."
            }

            var firstSavedImage: Uri? = null

            for (pageIndex in selectedPageIndexes) {

                val page = renderer.openPage(pageIndex)

                try {

                    val targetWidth = 1600

                    val targetHeight = (
                            targetWidth.toFloat() *
                                    page.height.toFloat() /
                                    page.width.toFloat()
                            ).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(
                        targetWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888
                    )

                    try {

                        bitmap.eraseColor(AndroidColor.WHITE)

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        val savedUri = saveJpegImage(
                            context = context,
                            folderUri = folderUri,
                            bitmap = bitmap,
                            fileName = "DocConveter_Page_${pageIndex + 1}.jpg"
                        )

                        if (firstSavedImage == null) {
                            firstSavedImage = savedUri
                        }

                    } finally {

                        bitmap.recycle()
                    }

                } finally {

                    page.close()
                }
            }

            return PdfConversionResult(
                pageCount = selectedPageIndexes.size,
                firstImageUri = requireNotNull(firstSavedImage)
            )

        } finally {

            renderer.close()
        }
    }
}



private fun saveJpegImage(
    context: Context,
    folderUri: Uri,
    bitmap: Bitmap,
    fileName: String
): Uri {

    val parentFolderUri =
        DocumentsContract.buildDocumentUriUsingTree(
            folderUri,
            DocumentsContract.getTreeDocumentId(folderUri)
        )

    val imageUri = DocumentsContract.createDocument(
        context.contentResolver,
        parentFolderUri,
        "image/jpeg",
        fileName
    ) ?: throw IllegalStateException(
        "Unable to create image in selected folder."
    )

    try {

        val outputStream = context.contentResolver
            .openOutputStream(imageUri)
            ?: throw IllegalStateException(
                "Unable to open image output."
            )

        outputStream.use { stream ->

            check(
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    stream
                )
            ) {
                "Unable to save image."
            }
        }

        return imageUri

    } catch (e: Exception) {

        context.contentResolver.delete(
            imageUri,
            null,
            null
        )

        throw e
    }
}

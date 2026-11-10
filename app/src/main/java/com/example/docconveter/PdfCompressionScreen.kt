
package com.example.docconveter

import android.net.Uri
import android.provider.OpenableColumns

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.cos.COSStream

import java.io.ByteArrayOutputStream


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast

import java.io.File
import java.io.FileOutputStream
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader


private fun compressPdf(
    inputFile: File,
    outputFile: File,
    level: String
) {
    val quality = when (level) {
        "Low" -> 85
        "Medium" -> 70
        else -> 50
    }

    val maxDimension = when (level) {
        "Low" -> 2400
        "Medium" -> 1800
        else -> 1400
    }

    PDDocument.load(inputFile).use { document ->

        // Prevent processing the same PDF objects repeatedly.
        val processedImages = mutableSetOf<COSStream>()
        val processedForms = mutableSetOf<COSStream>()

        fun processResources(
            resources: com.tom_roush.pdfbox.pdmodel.PDResources,
            depth: Int = 0
        ) {
            // Protect against deeply nested or circular Form XObjects.
            if (depth > 20) return

            val names = resources.xObjectNames.toList()

            for (name in names) {
                val xObject = try {
                    resources.getXObject(name)
                } catch (_: Exception) {
                    continue
                }

                when (xObject) {

                    is PDImageXObject -> {

                        if (!processedImages.add(xObject.cosObject)) {
                            continue
                        }

                        if (
                            xObject.softMask != null ||
                            xObject.mask != null
                        ) {
                            continue
                        }

                        val originalBitmap = try {
                            xObject.image
                        } catch (_: Exception) {
                            null
                        } ?: continue

                        val scale = minOf(
                            1f,
                            maxDimension.toFloat() /
                                    maxOf(
                                        originalBitmap.width,
                                        originalBitmap.height
                                    )
                        )

                        val resizedBitmap = if (scale < 1f) {
                            Bitmap.createScaledBitmap(
                                originalBitmap,
                                (originalBitmap.width * scale)
                                    .toInt().coerceAtLeast(1),
                                (originalBitmap.height * scale)
                                    .toInt().coerceAtLeast(1),
                                true
                            )
                        } else {
                            originalBitmap
                        }

                        val rgbBitmap = Bitmap.createBitmap(
                            resizedBitmap.width,
                            resizedBitmap.height,
                            Bitmap.Config.RGB_565
                        )

                        try {
                            Canvas(rgbBitmap).apply {
                                drawColor(AndroidColor.WHITE)
                                drawBitmap(
                                    resizedBitmap,
                                    0f,
                                    0f,
                                    null
                                )
                            }

                            val imageBytes = ByteArrayOutputStream()

                            rgbBitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                quality,
                                imageBytes
                            )

                            val compressedImage =
                                JPEGFactory.createFromByteArray(
                                    document,
                                    imageBytes.toByteArray()
                                )

                            resources.put(name, compressedImage)

                        } finally {
                            rgbBitmap.recycle()

                            if (resizedBitmap !== originalBitmap) {
                                resizedBitmap.recycle()
                            }
                        }
                    }

                    is PDFormXObject -> {

                        if (!processedForms.add(xObject.cosObject)) {
                            continue
                        }

                        val formResources = xObject.resources

                        if (formResources != null) {
                            processResources(
                                formResources,
                                depth + 1
                            )
                        }
                    }
                }
            }
        }

        for (page in document.pages) {
            val resources = page.resources ?: continue
            processResources(resources)
        }

        document.save(outputFile)
    }
}

private fun formatPdfSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 ->
            String.format("%.2f KB", bytes / 1024.0)
        else ->
            String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfCompressionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var originalSize by remember { mutableLongStateOf(0L) }

    var compressionLevel by remember {
        mutableStateOf("Medium")
    }

    var showSuccessDialog by remember {
        mutableStateOf(false)
    }

    var compressedPdfUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var compressionReduced by remember {
        mutableStateOf(false)
    }

    var compressionMode by remember {
        mutableStateOf("Standard")
    }


    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->

        val sourceUri = selectedPdf

        if (uri != null && sourceUri != null) {

            scope.launch {
                isLoading = true
                errorMessage = ""

                try {
                    val wasCompressed = withContext(Dispatchers.IO) {

                        val sourceFile = File.createTempFile(
                            "docconveter_source_",
                            ".pdf",
                            context.cacheDir
                        )

                        val candidateFile = File.createTempFile(
                            "docconveter_candidate_",
                            ".pdf",
                            context.cacheDir
                        )

                        try {
                            context.contentResolver
                                .openInputStream(sourceUri)
                                ?.use { input ->
                                    FileOutputStream(sourceFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                ?: throw Exception("Unable to open source PDF.")

                            if (compressionMode == "Standard") {

                                // Existing offline compression
                                PDFBoxResourceLoader.init(context.applicationContext)

                                compressPdf(
                                    sourceFile,
                                    candidateFile,
                                    compressionLevel
                                )

                            } else {

                                // Advanced online compression
                                compressPdfWithApi(
                                    sourceFile,
                                    candidateFile
                                )
                            }

                            val useCompressedFile =
                                candidateFile.exists() &&
                                        candidateFile.length() > 0L &&
                                        candidateFile.length() < sourceFile.length()

                            val finalFile = if (useCompressedFile) {
                                candidateFile
                            } else {
                                sourceFile
                            }

                            context.contentResolver
                                .openOutputStream(uri)
                                ?.use { output ->
                                    finalFile.inputStream().use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                ?: throw Exception("Unable to save output PDF.")

                            useCompressedFile

                        } finally {
                            sourceFile.delete()
                            candidateFile.delete()
                        }
                    }

                    compressionReduced = wasCompressed
                    compressedPdfUri = uri
                    showSuccessDialog = true

                } catch (e: Exception) {
                    errorMessage = e.localizedMessage
                        ?: "Unable to compress PDF."
                } finally {
                    isLoading = false
                }
            }
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {
            selectedPdf = uri
            originalSize = 0L
            pdfName = "Selected PDF"
            errorMessage = ""

            scope.launch {
                isLoading = true

                try {
                    val fileInfo = withContext(Dispatchers.IO) {

                        var name = "Selected PDF"
                        var size = 0L

                        context.contentResolver.query(
                            uri,
                            null,
                            null,
                            null,
                            null
                        )?.use { cursor ->

                            val nameIndex = cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                            )

                            val sizeIndex = cursor.getColumnIndex(
                                OpenableColumns.SIZE
                            )

                            if (cursor.moveToFirst()) {
                                if (nameIndex >= 0) {
                                    name = cursor.getString(nameIndex)
                                        ?: name
                                }

                                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                                    size = cursor.getLong(sizeIndex)
                                }
                            }
                        }

                        // Fallback when the provider doesn't report file size.
                        if (size <= 0L) {
                            context.contentResolver
                                .openAssetFileDescriptor(uri, "r")
                                ?.use { descriptor ->
                                    size = descriptor.length
                                }
                        }

                        Pair(name, size)
                    }

                    pdfName = fileInfo.first
                    originalSize = fileInfo.second

                    if (originalSize <= 0L) {
                        errorMessage =
                            "Unable to determine the PDF file size."
                    }

                } catch (e: Exception) {
                    errorMessage = e.localizedMessage
                        ?: "Unable to read the selected PDF."
                    selectedPdf = null

                } finally {
                    isLoading = false
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
                            "Compress PDF",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            enabled = !isLoading
                        ) {
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
            bottomBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            pdfSaver.launch("DocConveter_Compressed.pdf")
                        },
                        enabled = selectedPdf != null && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 12.dp
                            )
                            .height(55.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isLoading) {
                                "Compressing..."
                            } else {
                                "Compress PDF"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            containerColor = Color(0xFFF6F7FB)
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            )  {

                Text(
                    "Reduce PDF File Size",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Choose a PDF and select how much compression you need.",
                    fontSize = 14.sp,
                    color = Color(0xFF737D91)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
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

                    Text("Select PDF File")
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                if (selectedPdf != null && originalSize > 0L) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                pdfName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Original size",
                                fontSize = 13.sp,
                                color = Color(0xFF737D91)
                            )

                            Text(
                                formatPdfSize(originalSize),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3157D5)
                            )
                        }
                    }
                    Text(
                        "Compression Mode",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val modes = listOf("Standard", "Advanced")

                    modes.forEach { mode ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (compressionMode == mode)
                                    Color(0xFFEAF0FF)
                                else
                                    Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = compressionMode == mode,
                                    onClick = {
                                        compressionMode = mode
                                    }
                                )

                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        if (mode == "Standard")
                                            "Standard Compression"
                                        else
                                            "Advanced Compression",
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        if (mode == "Standard")
                                            "Offline compression on your device"
                                        else
                                            "Online compression using DocConveter server",
                                        fontSize = 12.sp,
                                        color = Color(0xFF737D91)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (compressionMode == "Standard") {

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Compression Level",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val levels = listOf(
                            "Low",
                            "Medium",
                            "High"
                        )

                        levels.forEach { level ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (
                                        compressionLevel == level
                                    ) Color(0xFFEAF0FF)
                                    else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = compressionLevel == level,
                                        onClick = {
                                            compressionLevel = level
                                        }
                                    )

                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            level,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            when (level) {
                                                "Low" ->
                                                    "Prioritize document quality"
                                                "Medium" ->
                                                    "Balance quality and file size"
                                                else ->
                                                    "Prioritize smaller file size"
                                            },
                                            fontSize = 12.sp,
                                            color = Color(0xFF737D91)
                                        )
                                    }
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(12.dp))

                    val levels = listOf(
                        "Low",
                        "Medium",
                        "High"
                    )

                    levels.forEach { level ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (
                                    compressionLevel == level
                                ) Color(0xFFEAF0FF)
                                else Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = compressionLevel == level,
                                    onClick = {
                                        compressionLevel = level
                                    }
                                )

                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        level,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        when (level) {
                                            "Low" ->
                                                "Prioritize document quality"
                                            "Medium" ->
                                                "Balance quality and file size"
                                            else ->
                                                "Prioritize smaller file size"
                                        },
                                        fontSize = 12.sp,
                                        color = Color(0xFF737D91)
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        errorMessage,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }
                if (showSuccessDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showSuccessDialog = false
                        },
                        title = {
                            Text(
                                text = if (compressionReduced) {
                                    "Compression Successful"
                                } else {
                                    "PDF Saved Without Reduction"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = if (compressionReduced) {
                                    "Your PDF size has been reduced successfully. Would you like to open it now?"
                                } else {
                                    "This PDF could not be reduced further. The original file was saved without increasing its size. Would you like to open it?"
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showSuccessDialog = false

                                    compressedPdfUri?.let { uri ->
                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW
                                            ).apply {
                                                setDataAndType(
                                                    uri,
                                                    "application/pdf"
                                                )
                                                addFlags(
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                )
                                            }

                                            context.startActivity(
                                                Intent.createChooser(
                                                    intent,
                                                    "Open compressed PDF"
                                                )
                                            )

                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                "No PDF viewer installed.",
                                                Toast.LENGTH_SHORT
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
                                    showSuccessDialog = false
                                }
                            ) {
                                Text("Done")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

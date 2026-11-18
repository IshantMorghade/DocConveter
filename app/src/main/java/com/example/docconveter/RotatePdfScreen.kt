
package com.example.docconveter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatePdfScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var selectedPdfName by remember {
        mutableStateOf("")
    }

    var selectedPdfFile by remember {
        mutableStateOf<File?>(null)
    }

    var pageCount by remember {
        mutableIntStateOf(0)
    }

    var selectedRotation by remember {
        mutableIntStateOf(90)
    }

    var pageRange by remember {
        mutableStateOf("")
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var statusMessage by remember {
        mutableStateOf("")
    }

    var pendingOutputFile by remember {
        mutableStateOf<File?>(null)
    }

    var showSuccessDialog by remember {
        mutableStateOf(false)
    }

    var savedPdfUri by remember {
        mutableStateOf<Uri?>(null)
    }


    val openPdfLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                scope.launch {
                    isProcessing = true
                    statusMessage = "Reading PDF..."

                    try {
                        val result = withContext(Dispatchers.IO) {

                            PDFBoxResourceLoader.init(
                                context.applicationContext
                            )

                            val inputFile = File(
                                context.cacheDir,
                                "rotate_input_${System.currentTimeMillis()}.pdf"
                            )

                            context.contentResolver
                                .openInputStream(uri)
                                ?.use { input ->
                                    inputFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                ?: throw Exception(
                                    "Unable to read selected PDF."
                                )

                            val count = PDDocument.load(inputFile).use {
                                it.numberOfPages
                            }

                            Pair(inputFile, count)
                        }

                        selectedPdfFile = result.first
                        pageCount = result.second

                        selectedPdfName =
                            uri.lastPathSegment
                                ?.substringAfterLast("/")
                                ?: "Selected PDF"

                        pageRange = ""
                        statusMessage = "PDF loaded successfully."

                    } catch (e: Exception) {

                        statusMessage =
                            "Unable to open PDF: ${e.message}"

                    } finally {
                        isProcessing = false
                    }
                }
            }
        }


    val savePdfLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/pdf"
            )
        ) { uri ->

            if (uri != null) {

                scope.launch {
                    isProcessing = true
                    statusMessage = "Saving PDF..."

                    try {
                        val outputFile = pendingOutputFile
                            ?: throw Exception(
                                "Rotated PDF file not found."
                            )

                        withContext(Dispatchers.IO) {

                            context.contentResolver
                                .openOutputStream(uri)
                                ?.use { output ->
                                    outputFile.inputStream().use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                ?: throw Exception(
                                    "Unable to save PDF."
                                )
                        }

                        savedPdfUri = uri
                        showSuccessDialog = true
                        statusMessage = "PDF saved successfully."

                    } catch (e: Exception) {

                        statusMessage =
                            "Unable to save PDF: ${e.message}"

                    } finally {
                        isProcessing = false
                    }
                }
            }
        }


    fun parsePageRange(
        range: String,
        totalPages: Int
    ): Set<Int> {

        if (range.isBlank()) {
            return (1..totalPages).toSet()
        }

        val pages = mutableSetOf<Int>()

        range.split(",").forEach { part ->

            val value = part.trim()

            if (value.contains("-")) {

                val limits = value.split("-")

                if (limits.size != 2) {
                    throw Exception("Invalid page range.")
                }

                val start = limits[0].trim().toIntOrNull()
                    ?: throw Exception("Invalid page number.")

                val end = limits[1].trim().toIntOrNull()
                    ?: throw Exception("Invalid page number.")

                if (
                    start < 1 ||
                    end > totalPages ||
                    start > end
                ) {
                    throw Exception(
                        "Enter a valid range between 1 and $totalPages."
                    )
                }

                pages.addAll(start..end)

            } else {

                val page = value.toIntOrNull()
                    ?: throw Exception("Invalid page number.")

                if (page < 1 || page > totalPages) {
                    throw Exception(
                        "Page number must be between 1 and $totalPages."
                    )
                }

                pages.add(page)
            }
        }

        if (pages.isEmpty()) {
            throw Exception("Enter at least one page number.")
        }

        return pages
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rotate PDF",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        containerColor = colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Filled.RotateRight,
                contentDescription = null,
                modifier = Modifier.size(65.dp),
                tint = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Change PDF page orientation",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )

            Text(
                text = "Rotate all pages or select specific pages.",
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(25.dp))


            OutlinedButton(
                onClick = {
                    openPdfLauncher.launch(
                        arrayOf("application/pdf")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Select PDF")
            }


            if (selectedPdfFile != null) {

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = selectedPdfName,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )

                        Text(
                            text = "$pageCount pages",
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }


                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Rotation angle",
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(90, 180, 270).forEach { angle ->

                        FilterChip(
                            selected = selectedRotation == angle,
                            onClick = {
                                selectedRotation = angle
                            },
                            label = {
                                Text("$angle°")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = colorScheme.primary
                            )
                        )
                    }
                }


                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Pages to rotate",
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = pageRange,
                    onValueChange = {
                        pageRange = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Page numbers or ranges")
                    },
                    placeholder = {
                        Text("Example: 1-3, 5, 8-10")
                    },
                    supportingText = {
                        Text(
                            "Leave empty to rotate all $pageCount pages."
                        )
                    },
                    singleLine = true
                )


                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {

                        val inputFile = selectedPdfFile
                            ?: return@Button

                        scope.launch {
                            isProcessing = true
                            statusMessage = "Rotating PDF..."

                            try {
                                val outputFile = withContext(Dispatchers.IO) {

                                    val pagesToRotate = parsePageRange(
                                        pageRange,
                                        pageCount
                                    )

                                    val output = File(
                                        context.cacheDir,
                                        "rotated_${System.currentTimeMillis()}.pdf"
                                    )

                                    PDDocument.load(inputFile).use { document ->

                                        for (
                                        index in 0 until document.numberOfPages
                                        ) {

                                            if ((index + 1) in pagesToRotate) {

                                                val page =
                                                    document.getPage(index)

                                                val newRotation =
                                                    (page.rotation +
                                                            selectedRotation) % 360

                                                page.rotation = newRotation
                                            }
                                        }

                                        document.save(output)
                                    }

                                    output
                                }

                                pendingOutputFile = outputFile

                                statusMessage =
                                    "Choose where to save your PDF."

                                savePdfLauncher.launch(
                                    "DocConveter_Rotated.pdf"
                                )

                            } catch (e: Exception) {

                                statusMessage =
                                    "Unable to rotate PDF: ${e.message}"

                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = selectedPdfFile != null && !isProcessing
                ) {
                    Text("Rotate and Save PDF")
                }
            }


            if (isProcessing) {

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    color = colorScheme.primary
                )
            }


            if (statusMessage.isNotBlank()) {

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = statusMessage,
                    color = colorScheme.primary
                )
            }
        }
    }


    if (showSuccessDialog) {

        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
            },
            title = {
                Text(
                    "PDF saved successfully",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Would you like to open the rotated PDF now?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false

                        savedPdfUri?.let { uri ->

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

                            try {
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        "Open PDF"
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {

                                Toast.makeText(
                                    context,
                                    "No PDF viewer found.",
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
                    Text("Later")
                }
            }
        )
    }
}


package com.example.docconveter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.graphics.pdf.PdfRenderer

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

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.InputStream

private fun parsePageRanges(
    input: String,
    totalPages: Int
): List<Int> {

    require(input.isNotBlank()) {
        "Please enter the page numbers or ranges."
    }

    val pages = linkedSetOf<Int>()

    input.split(",").forEach { part ->

        val range = part.trim()

        require(range.isNotEmpty()) {
            "Invalid page range."
        }

        if (range.contains("-")) {

            val numbers = range.split("-")

            require(numbers.size == 2) {
                "Invalid range: $range"
            }

            val start = numbers[0].trim().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid range: $range")

            val end = numbers[1].trim().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid range: $range")

            require(start >= 1 && end >= start && end <= totalPages) {
                "Range $range is invalid. Enter pages between 1 and $totalPages."
            }

            for (page in start..end) {
                pages.add(page - 1)
            }

        } else {

            val page = range.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid page number: $range")

            require(page in 1..totalPages) {
                "Page $page is invalid. Enter pages between 1 and $totalPages."
            }

            pages.add(page - 1)
        }
    }

    require(pages.isNotEmpty()) {
        "No pages selected."
    }

    return pages.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }

    var pageRange by remember { mutableStateOf("") }
    var pendingPages by remember { mutableStateOf<List<Int>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var isExtracting by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var extractedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {

            selectedPdf = uri
            totalPages = 0
            pageRange = ""
            errorMessage = ""

            pdfName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?: "Selected PDF"

            scope.launch {

                isLoading = true

                try {
                    val pageCount = withContext(Dispatchers.IO) {

                        val descriptor =
                            context.contentResolver.openFileDescriptor(uri, "r")
                                ?: throw Exception("Unable to open this PDF.")

                        descriptor.use {
                            PdfRenderer(it).use { renderer ->
                                renderer.pageCount
                            }
                        }
                    }

                    totalPages = pageCount

                    if (pageCount == 0) {
                        throw Exception("This PDF does not contain any pages.")
                    }

                } catch (e: Exception) {

                    errorMessage = e.localizedMessage
                        ?: "Unable to read the selected PDF."

                    selectedPdf = null
                    totalPages = 0

                    showErrorDialog = true

                } finally {
                    isLoading = false
                }
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri ->

        if (destinationUri != null) {

            scope.launch {

                isExtracting = true

                try {

                    val sourceUri = selectedPdf
                        ?: throw Exception("Please select a PDF first.")

                    withContext(Dispatchers.IO) {

                        PDFBoxResourceLoader.init(context.applicationContext)

                        val inputStream: InputStream =
                            context.contentResolver.openInputStream(sourceUri)
                                ?: throw Exception("Unable to read the original PDF.")

                        inputStream.use { input ->

                            PDDocument.load(input).use { sourceDocument ->

                                PDDocument().use { outputDocument ->

                                    pendingPages.forEach { pageIndex ->

                                        outputDocument.importPage(
                                            sourceDocument.getPage(pageIndex)
                                        )
                                    }

                                    val outputStream =
                                        context.contentResolver.openOutputStream(
                                            destinationUri,
                                            "w"
                                        ) ?: throw Exception(
                                            "Unable to create the output PDF."
                                        )

                                    outputStream.use { output ->
                                        outputDocument.save(output)
                                    }
                                }
                            }
                        }
                    }

                    extractedPdfUri = destinationUri
                    showSuccessDialog = true

                } catch (e: Exception) {

                    errorMessage = e.localizedMessage
                        ?: "Something went wrong while extracting pages."

                    showErrorDialog = true

                } finally {
                    isExtracting = false
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
                            "Split PDF",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            enabled = !isLoading && !isExtracting
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
            containerColor = Color(0xFFF6F7FB)
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {

                Text(
                    "Extract Pages from PDF",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Select a PDF and enter the pages you want to extract.",
                    fontSize = 14.sp,
                    color = Color(0xFF737D91)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    },
                    enabled = !isLoading && !isExtracting,
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

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                if (selectedPdf != null && totalPages > 0) {

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
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Total pages: $totalPages",
                                color = Color(0xFF3157D5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        "Pages to extract",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pageRange,
                        onValueChange = { pageRange = it },
                        enabled = !isExtracting,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Example: 1-5, 8, 10-12")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Use commas to separate pages or ranges. " +
                                "Valid pages: 1 to $totalPages.",
                        fontSize = 12.sp,
                        color = Color(0xFF737D91)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {

                        try {

                            pendingPages = parsePageRanges(
                                pageRange,
                                totalPages
                            )

                            errorMessage = ""

                            savePdfLauncher.launch(
                                "DocConveter_Split.pdf"
                            )

                        } catch (e: Exception) {

                            errorMessage = e.localizedMessage
                                ?: "Please enter a valid page range."

                            showErrorDialog = true
                        }
                    },
                    enabled = selectedPdf != null &&
                            totalPages > 0 &&
                            !isLoading &&
                            !isExtracting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    if (isExtracting) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Extracting Pages...")

                    } else {

                        Text(
                            "Extract Pages",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {

        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
            },
            title = {
                Text("Pages Extracted Successfully!")
            },
            text = {
                Text(
                    "${pendingPages.size} pages have been saved " +
                            "to DocConveter_Split.pdf."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {

                        showSuccessDialog = false

                        try {

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    extractedPdfUri,
                                    "application/pdf"
                                )
                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }

                            context.startActivity(intent)

                        } catch (_: ActivityNotFoundException) {

                            errorMessage =
                                "No PDF viewer is installed on your device."

                            showErrorDialog = true
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

    if (showErrorDialog) {

        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
            },
            title = {
                Text("Unable to Extract Pages")
            },
            text = {
                Text(errorMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

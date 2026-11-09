package com.example.docconveter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedPdfs = remember {
        mutableStateListOf<Uri>()
    }

    var pendingPdfs by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    var isMerging by remember {
        mutableStateOf(false)
    }

    var showSuccessDialog by remember {
        mutableStateOf(false)
    }

    var showErrorDialog by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var mergedPdfUri by remember {
        mutableStateOf<Uri?>(null)
    }

    // Select multiple PDF files
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->

        uris.forEach { uri ->
            if (!selectedPdfs.contains(uri)) {
                selectedPdfs.add(uri)
            }
        }
    }

    // Select destination and filename
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri ->

        if (destinationUri != null) {

            coroutineScope.launch {

                isMerging = true

                try {
                    withContext(Dispatchers.IO) {

                        // Initialize PDFBox
                        PDFBoxResourceLoader.init(context.applicationContext)

                        val merger = PDFMergerUtility()
                        val inputStreams = mutableListOf<InputStream>()

                        try {

                            // Add source PDFs in selected order
                            pendingPdfs.forEach { pdfUri ->

                                val inputStream =
                                    context.contentResolver.openInputStream(pdfUri)
                                        ?: throw Exception(
                                            "Unable to open one of the selected PDF files."
                                        )

                                inputStreams.add(inputStream)
                                merger.addSource(inputStream)
                            }

                            // Create destination PDF
                            val outputStream =
                                context.contentResolver.openOutputStream(destinationUri)
                                    ?: throw Exception(
                                        "Unable to create the output PDF file."
                                    )

                            outputStream.use { output ->

                                merger.setDestinationStream(output)

                                merger.mergeDocuments(
                                    MemoryUsageSetting.setupMainMemoryOnly()
                                )
                            }

                        } finally {

                            inputStreams.forEach { input ->
                                try {
                                    input.close()
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }

                    mergedPdfUri = destinationUri
                    showSuccessDialog = true

                } catch (e: Exception) {

                    errorMessage = e.localizedMessage
                        ?: "Something went wrong while merging PDFs."

                    showErrorDialog = true

                } finally {

                    isMerging = false
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
                            text = "Merge PDF",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            enabled = !isMerging
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
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
                    text = "Combine Multiple PDFs",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select two or more PDF files to combine them into one document.",
                    fontSize = 14.sp,
                    color = Color(0xFF737D91)
                )

                Spacer(modifier = Modifier.height(22.dp))

                OutlinedButton(
                    onClick = {
                        pdfPicker.launch(
                            arrayOf("application/pdf")
                        )
                    },
                    enabled = !isMerging,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text("Select PDF Files")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Selected Files (${selectedPdfs.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedPdfs.isEmpty()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No PDF files selected yet.",
                            color = Color(0xFF737D91),
                            fontSize = 14.sp
                        )
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        items(
                            items = selectedPdfs,
                            key = { it.toString() }
                        ) { uri ->

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFF3157D5),
                                        modifier = Modifier.size(30.dp)
                                    )

                                    Spacer(
                                        modifier = Modifier.width(12.dp)
                                    )

                                    Text(
                                        text = uri.lastPathSegment
                                            ?.substringAfterLast('/')
                                            ?: "Selected PDF",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                        maxLines = 2
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Column {

                                            TextButton(
                                                onClick = {
                                                    val index = selectedPdfs.indexOf(uri)

                                                    if (index > 0) {
                                                        selectedPdfs.removeAt(index)
                                                        selectedPdfs.add(index - 1, uri)
                                                    }
                                                },
                                                enabled = !isMerging &&
                                                        selectedPdfs.indexOf(uri) > 0
                                            ) {
                                                Text("↑")
                                            }

                                            TextButton(
                                                onClick = {
                                                    val index = selectedPdfs.indexOf(uri)

                                                    if (index < selectedPdfs.lastIndex) {
                                                        selectedPdfs.removeAt(index)
                                                        selectedPdfs.add(index + 1, uri)
                                                    }
                                                },
                                                enabled = !isMerging &&
                                                        selectedPdfs.indexOf(uri) < selectedPdfs.lastIndex
                                            ) {
                                                Text("↓")
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                selectedPdfs.remove(uri)
                                            },
                                            enabled = !isMerging
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove PDF",
                                                tint = Color(0xFFD32F2F)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                OutlinedButton(
                    onClick = {
                        selectedPdfs.clear()
                    },
                    enabled = selectedPdfs.isNotEmpty() && !isMerging,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        pendingPdfs = selectedPdfs.toList()

                        savePdfLauncher.launch(
                            "DocConveter_Merged.pdf"
                        )
                    },
                    enabled = selectedPdfs.size >= 2 && !isMerging,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    if (isMerging) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Merging PDFs...")

                    } else {

                        Text(
                            text = "Merge PDFs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {

        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
            },
            title = {
                Text("PDFs Merged Successfully!")
            },
            text = {
                Text(
                    "Your merged PDF has been saved as DocConveter_Merged.pdf."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false

                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    mergedPdfUri,
                                    "application/pdf"
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

    // Error dialog
    if (showErrorDialog) {

        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
            },
            title = {
                Text("Unable to Merge PDFs")
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
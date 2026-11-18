package com.example.docconveter

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandBlue = Color(0xFF3157D5)

private data class PdfTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DocConveterApp()
        }
    }
}

@Composable
fun DocConveterApp() {

    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "docconveter_settings",
            Context.MODE_PRIVATE
        )
    }

    var isDarkTheme by remember {
        mutableStateOf(
            preferences.getBoolean("dark_theme", false)
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    var showImageToPdf by remember { mutableStateOf(false) }
    var showPdfToImage by remember { mutableStateOf(false) }
    var showMergePdf by remember { mutableStateOf(false) }
    var showSplitPdf by remember { mutableStateOf(false) }
    var showPdfCompression by remember { mutableStateOf(false) }
    var showRotatePdf by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // App color scheme
    val appColorScheme = if (isDarkTheme) {

        darkColorScheme(
            primary = Color(0xFF9DB5FF),
            onPrimary = Color(0xFF17243A),
            background = Color(0xFF171B24),
            onBackground = Color(0xFFF1F3F8),
            surface = Color(0xFF222936),
            onSurface = Color(0xFFF1F3F8),
            onSurfaceVariant = Color(0xFFA7B0C1),
            outline = Color(0xFF3B4556),
            outlineVariant = Color(0xFF303A4A)
        )

    } else {

        lightColorScheme(
            primary = Color(0xFF3157D5),
            onPrimary = Color.White,
            background = Color(0xFFF5F6F8),
            onBackground = Color(0xFF17243A),
            surface = Color.White,
            onSurface = Color(0xFF17243A),
            onSurfaceVariant = Color(0xFF657187),
            outline = Color(0xFFDCE1E9),
            outlineVariant = Color(0xFFE8EBF0)
        )
    }

    // IMPORTANT:
    // Apply MaterialTheme to the entire app,
    // not only the home screen.
    MaterialTheme(
        colorScheme = appColorScheme
    ) {

        // Handle Android system Back button
        BackHandler(
            enabled = showImageToPdf ||
                    showPdfToImage ||
                    showMergePdf ||
                    showSplitPdf ||
                    showPdfCompression ||
                    showRotatePdf ||
                    showSettings
        ) {
            when {
                showImageToPdf -> showImageToPdf = false
                showPdfToImage -> showPdfToImage = false
                showMergePdf -> showMergePdf = false
                showSplitPdf -> showSplitPdf = false
                showPdfCompression -> showPdfCompression = false
                showRotatePdf -> showRotatePdf = false
                showSettings -> showSettings = false
            }
        }

        // Image to PDF
        if (showImageToPdf) {

            ImageToPdfScreen(
                onBack = {
                    showImageToPdf = false
                }
            )

        }

        // PDF to Image
        else if (showPdfToImage) {

            PdfToImageScreen(
                onBack = {
                    showPdfToImage = false
                }
            )

        }

        // Merge PDF
        else if (showMergePdf) {

            MergePdfScreen(
                onBack = {
                    showMergePdf = false
                }
            )

        }

        // Split PDF
        else if (showSplitPdf) {

            SplitPdfScreen(
                onBack = {
                    showSplitPdf = false
                }
            )

        }

        // Compress PDF
        else if (showPdfCompression) {

            PdfCompressionScreen(
                onBack = {
                    showPdfCompression = false
                }
            )

        }

        // Rotate PDF
        else if (showRotatePdf) {

            RotatePdfScreen(
                onBack = {
                    showRotatePdf = false
                }
            )

        }

        // Settings
        else if (showSettings) {

            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeChange = { darkMode ->

                    isDarkTheme = darkMode

                    preferences.edit()
                        .putBoolean("dark_theme", darkMode)
                        .apply()
                },
                onBack = {
                    showSettings = false
                }
            )

        }

        // Home screen
        else {

            val tools = listOf(
                PdfTool(
                    "PDF to Image",
                    "Convert pages to images",
                    Icons.Filled.Image
                ),
                PdfTool(
                    "Image to PDF",
                    "Create PDF from images",
                    Icons.Filled.PictureAsPdf
                ),
                PdfTool(
                    "Merge PDF",
                    "Combine multiple PDFs",
                    Icons.Filled.CallMerge
                ),
                PdfTool(
                    "Split PDF",
                    "Extract selected pages",
                    Icons.Filled.CallSplit
                ),
                PdfTool(
                    "Compress PDF",
                    "Reduce PDF file size",
                    Icons.Filled.Compress
                ),
                PdfTool(
                    "Rotate PDF",
                    "Change page orientation",
                    Icons.Filled.RotateRight
                )
            )

            val filteredTools = tools.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.subtitle.contains(searchQuery, ignoreCase = true)
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {

                    Spacer(modifier = Modifier.height(18.dp))

                    // App branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "DocConveter logo",
                                tint = Color.White,
                                modifier = Modifier.size(27.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "DocConveter",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Every file. One solution.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                showSettings = true
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Welcome heading
                    Text(
                        text = "What would you like",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "to do today?",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Search PDF tools...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Dashboard introduction
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column {

                            Text(
                                text = "Your PDF workspace",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Choose a tool to get started",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "PDF workspace",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Tools heading
                    Text(
                        text = "PDF Tools",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Everything you need in one place",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tool cards
                    if (filteredTools.isEmpty()) {

                        Text(
                            text = "No tools found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )

                    } else {

                        filteredTools.chunked(2).forEach { rowTools ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {

                                rowTools.forEach { tool ->

                                    ToolCard(
                                        tool = tool,
                                        modifier = Modifier.weight(1f),
                                        onClick = {

                                            when (tool.title) {

                                                "PDF to Image" -> {
                                                    showPdfToImage = true
                                                }

                                                "Image to PDF" -> {
                                                    showImageToPdf = true
                                                }

                                                "Merge PDF" -> {
                                                    showMergePdf = true
                                                }

                                                "Split PDF" -> {
                                                    showSplitPdf = true
                                                }

                                                "Compress PDF" -> {
                                                    showPdfCompression = true
                                                }

                                                "Rotate PDF" -> {
                                                    showRotatePdf = true
                                                }

                                                else -> {
                                                    Toast.makeText(
                                                        context,
                                                        "${tool.title} is coming soon!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    )
                                }

                                if (rowTools.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Recent files
                    Text(
                        text = "Recent Files",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(38.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "No recent files",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Your processed documents will appear here.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: PdfTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(145.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.12f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tool.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

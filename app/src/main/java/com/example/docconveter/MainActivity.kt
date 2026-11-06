package com.example.docconveter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
private val PageBackground = Color(0xFFF6F7FB)
private val DarkText = Color(0xFF172033)
private val SecondaryText = Color(0xFF737D91)

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
    var searchQuery by remember { mutableStateOf("") }
    var showImageToPdf by remember { mutableStateOf(false) }

    // Open the Image to PDF screen
    if (showImageToPdf) {
        ImageToPdfScreen(
            onBack = {
                showImageToPdf = false
            }
        )
        return
    }

    val tools = listOf(
        PdfTool("PDF to Image", "Convert pages to images", Icons.Filled.Image),
        PdfTool("Image to PDF", "Create PDF from images", Icons.Filled.PictureAsPdf),
        PdfTool("Merge PDF", "Combine multiple PDFs", Icons.Filled.CallMerge),
        PdfTool("Split PDF", "Extract selected pages", Icons.Filled.CallSplit),
        PdfTool("Rotate PDF", "Change page orientation", Icons.Filled.RotateRight)
    )

    val filteredTools = tools.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandBlue,
            background = PageBackground,
            surface = Color.White
        )
    ) {
        Scaffold(
            containerColor = PageBackground
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

                    Column {
                        Text(
                            text = "DocConveter",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Text(
                            text = "Every file. One solution.",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Welcome heading
                Text(
                    text = "What would you like",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text = "to do today?",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search PDF tools...")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE1E5EF),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Featured action
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BrandBlue
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {
                        Text(
                            text = "FEATURED TOOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Compress PDF",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = "Reduce file size for easy sharing.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.Compress,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "PDF compression is coming soon!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = BrandBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Get started",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Tools heading
                Text(
                    text = "PDF Tools",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Text(
                    text = "Everything you need in one place",
                    fontSize = 13.sp,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tool cards
                if (filteredTools.isEmpty()) {
                    Text(
                        text = "No tools found.",
                        color = SecondaryText,
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
                                        if (tool.title == "Image to PDF") {
                                            showImageToPdf = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "${tool.title} is coming soon!",
                                                Toast.LENGTH_SHORT
                                            ).show()
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

                // Recent files section
                Text(
                    text = "Recent Files",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
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
                            tint = Color(0xFFADB6C8),
                            modifier = Modifier.size(38.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No recent files",
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText
                        )

                        Text(
                            text = "Your processed documents will appear here.",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
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
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEDF1FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tool.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                color = SecondaryText,
                lineHeight = 15.sp
            )
        }
    }
}
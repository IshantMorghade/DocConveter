
package com.example.docconveter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onSurface
                    )
                }

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Text(
                text = "Personalize your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    ThemeOption(
                        title = "Light theme",
                        description = "Clean and bright appearance",
                        icon = {
                            Icon(
                                Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        },
                        selected = !isDarkTheme,
                        onClick = { onThemeChange(false) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = colors.outlineVariant
                    )

                    ThemeOption(
                        title = "Dark theme",
                        description = "Comfortable in low light",
                        icon = {
                            Icon(
                                Icons.Filled.DarkMode,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        },
                        selected = isDarkTheme,
                        onClick = { onThemeChange(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .background(
                color = if (selected) {
                    colors.primary.copy(alpha = 0.10f)
                } else {
                    colors.surface
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

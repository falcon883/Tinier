package com.durvank883.tinier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavHostController
import com.durvank883.tinier.util.Helper.gesturesDisabled
import com.durvank883.tinier.util.ThemeMode
import com.durvank883.tinier.viewmodel.SettingsViewModel


@Composable
fun Settings(navController: NavHostController, viewModel: SettingsViewModel) {

    val context = LocalContext.current
    val selectedTheme by viewModel.themeMode.collectAsState()
    val showImageResolution by viewModel.showResolution.collectAsState()
    val appendNameAtStart by viewModel.appendNameAtStart.collectAsState()
    val isCleaningCache by viewModel.isCleaningCache.collectAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.gesturesDisabled(disabled = isCleaningCache)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Settings")
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Go Back",
                            )
                        }
                    },
                    elevation = 12.dp
                )
            },
        ) {
            Surface(
                color = MaterialTheme.colors.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tinier),
                            contentDescription = null,
                            modifier = Modifier.size(128.dp),
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.onSurface)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Version: ")
                            }
                            append("1.0.0")
                        })
                    }
                    SpacedDivider()
                    Text(text = "Theme:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row {
                        RadioButton(
                            selected = selectedTheme == ThemeMode.SYSTEM_DEFAULT_THEME.modeNo,
                            onClick = {
                                viewModel.setThemeMode(mode = ThemeMode.SYSTEM_DEFAULT_THEME.modeNo)
                            })
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "System Default")
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row {
                        RadioButton(
                            selected = selectedTheme == ThemeMode.LIGHT_THEME.modeNo,
                            onClick = {
                                viewModel.setThemeMode(mode = ThemeMode.LIGHT_THEME.modeNo)
                            })
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "Light Mode")
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row {
                        RadioButton(
                            selected = selectedTheme == ThemeMode.DARK_THEME.modeNo,
                            onClick = {
                                viewModel.setThemeMode(mode = ThemeMode.DARK_THEME.modeNo)
                            })
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "Dark Mode")
                    }

                    SpacedDivider()

                    Text(text = "Preferences:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildAnnotatedString {
                                append("Image Resolution: \n")
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Thin
                                    )
                                ) {
                                    append("By enabling you can resize image by specified resolution.")
                                }
                            },
                            modifier = Modifier.weight(.5f)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Switch(
                            checked = showImageResolution,
                            onCheckedChange = {
                                viewModel.setShowResolution(shouldShow = !showImageResolution)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.secondary
                            )
                        )
                    }
                    SpacedDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildAnnotatedString {
                                append("Append Name At: \n")
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Thin
                                    )
                                ) {
                                    append(
                                        "Appends the specified name either at start/end of the " +
                                                "original name"
                                    )
                                }
                            },
                            modifier = Modifier.weight(.5f)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        IconToggleButton(
                            checked = appendNameAtStart,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colors.secondary),
                            onCheckedChange = {
                                viewModel.setAppendNameAtStart(pos = !appendNameAtStart)
                            }) {
                            Text(
                                text = if (appendNameAtStart) {
                                    "Start"
                                } else {
                                    "End"
                                }, color = MaterialTheme.colors.onSecondary
                            )
                        }
                    }
                    SpacedDivider()

                    Button(
                        onClick = { viewModel.clearCacheDir(context = context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Clear Cached Images")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isCleaningCache,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.onBackground.copy(alpha = .5f))
            ) {
                Popup(alignment = Alignment.Center) {
                    Box(
                        Modifier
                            .wrapContentSize()
                            .background(MaterialTheme.colors.surface, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(30.dp)
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Clearing cache", color = MaterialTheme.colors.onSurface)
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun SpacedDivider() {
    Spacer(modifier = Modifier.height(15.dp))
    Divider()
    Spacer(modifier = Modifier.height(15.dp))
}

/*
* PREVIEW
* */

@Preview
@Composable
fun TestScreen2() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go Back",
                        )
                    }
                },
                elevation = 12.dp
            )
        },
    ) {
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tinier),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colors.onSurface)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Version: ")
                        }
                        append("1.0.0")
                    })
                }
                SpacedDivider()

                Text(text = "Theme:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = { })
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "System Default")
                }
                Spacer(modifier = Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = { })
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "Light Mode")
                }
                Spacer(modifier = Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = { })
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "Dark Mode")
                }
                SpacedDivider()

                Text(text = "Preferences:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            append("Image Resolution: \n")
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Thin
                                )
                            ) {
                                append("By enabling you can resize image by specified resolution.")
                            }
                        },
                        modifier = Modifier.weight(.5f)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Switch(checked = false, onCheckedChange = {})
                }
                SpacedDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            append("Append Name At: \n")
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Thin
                                )
                            ) {
                                append(
                                    "Appends the specified name either at start/end of the " +
                                            "original name"
                                )
                            }
                        },
                        modifier = Modifier.weight(.5f)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    IconToggleButton(
                        checked = false,
                        modifier = Modifier.background(MaterialTheme.colors.primarySurface),
                        onCheckedChange = {}) {
                        Text(text = "Start")
                    }
                }
                SpacedDivider()

                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Clear Cached Images")
                }
            }
        }
    }
}
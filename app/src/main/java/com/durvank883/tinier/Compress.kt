package com.durvank883.tinier

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.durvank883.tinier.model.ImageRes
import com.durvank883.tinier.route.MainRoutes
import com.durvank883.tinier.service.ImageCompressorService
import com.durvank883.tinier.util.Helper.getActivity
import com.durvank883.tinier.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Compress(navController: NavHostController, viewModel: MainViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Tinier")
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
            val focusManager = LocalFocusManager.current

            val totalPhotos = viewModel.photos.collectAsState().value.size
            var sliderPosition by remember { mutableStateOf(80f) }
            var expanded by remember { mutableStateOf(false) }
            val exportFormats = listOf("original", "png", "jpg", "webp")
            var selectedIndex by remember { mutableStateOf(0) }
            var imageSize by remember { mutableStateOf("0") }
            var isSizeInKB by remember { mutableStateOf(true) }
            var fileName by remember { mutableStateOf("") }
            val showResolution by viewModel.showResolution.collectAsState()

            var imageWidth by remember { mutableStateOf("0") }
            var imageHeight by remember { mutableStateOf("0") }
            var aspectRationLock by remember { mutableStateOf(true) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Text(text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Total Photos: ")
                    }
                    append(totalPhotos.toString())
                })
                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                Text(text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Image Quality: ")
                    }
                    append(sliderPosition.roundToInt().toString() + "%\n")
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp
                        )
                    ) {
                        append("(Default is 80%)")
                    }
                })
                Spacer(modifier = Modifier.padding(vertical = 5.dp))

                Box(contentAlignment = Alignment.Center) {
                    VerticalLines((0..10).map { it.toString() }.toList())
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        valueRange = 0f..100f
                    )
                }
                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                if (showResolution) {
                    Text(text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Image Resolution:\n")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp
                            )
                        ) {
                            append("(0 to ignore)")
                        }
                    })
                    Spacer(modifier = Modifier.padding(vertical = 5.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = imageWidth,
                            onValueChange = { imageWidth = it },
                            label = { Text("Width") },
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent {
                                    if (it.key.keyCode == Key.Tab.keyCode) {
                                        focusManager.moveFocus(FocusDirection.Right)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = {
                                focusManager.moveFocus(FocusDirection.Right)
                            }),
                            singleLine = true
                        )

                        IconToggleButton(
                            checked = aspectRationLock,
                            onCheckedChange = { aspectRationLock = !aspectRationLock },
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (aspectRationLock) {
                                        R.drawable.ic_lock_24
                                    } else {
                                        R.drawable.ic_lock_open_24
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        OutlinedTextField(
                            value = imageHeight,
                            onValueChange = { imageHeight = it },
                            label = { Text("Height") },
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent {
                                    if (it.key.keyCode == Key.Tab.keyCode) {
                                        focusManager.moveFocus(FocusDirection.Down)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = {
                                focusManager.moveFocus(FocusDirection.Down)
                            }),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.padding(vertical = 10.dp))
                }

                Text(text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Max Image Size:\n")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp
                        )
                    ) {
                        append("(0 to ignore)")
                    }
                })
                Spacer(modifier = Modifier.padding(vertical = 5.dp))

                OutlinedTextField(
                    value = imageSize,
                    onValueChange = { imageSize = it },
                    label = { Text("Enter max image size") },
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent {
                            if (it.key.keyCode == Key.Tab.keyCode) {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            } else {
                                false
                            }
                        },
                    trailingIcon = {
                        IconToggleButton(
                            checked = isSizeInKB,
                            onCheckedChange = {
                                isSizeInKB = !isSizeInKB
                            }
                        ) {
                            if (isSizeInKB) {
                                Text(text = "KB")
                            } else {
                                Text(text = "MB")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                Text(text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Export Format: ")
                    }
                })
                Spacer(modifier = Modifier.padding(vertical = 5.dp))

                Column(
                    modifier = Modifier
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable(onClick = { expanded = true })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = exportFormats[selectedIndex],
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width((LocalConfiguration.current.screenWidthDp - 30).dp)
                    ) {
                        exportFormats.forEachIndexed { index, s ->
                            DropdownMenuItem(onClick = {
                                selectedIndex = index
                                expanded = false
                            }) {
                                Text(text = s)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                Text(text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("File Name to append:\n")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp
                        )
                    ) {
                        append("(keep blank for original)")
                    }
                })
                Spacer(modifier = Modifier.padding(vertical = 5.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Enter file name") },
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                    }),
                    singleLine = true
                )
                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                Button(
                    onClick = {
                        viewModel.setCompressConfig(
                            quality = sliderPosition.roundToInt(),
                            maxImageSize = mapOf(
                                when (isSizeInKB) {
                                    true -> "KB"
                                    false -> "MB"
                                } to imageSize
                            ),
                            exportFormat = exportFormats[selectedIndex],
                            appendName = fileName,
                            resolution = ImageRes(
                                imageWidth = imageWidth.toInt(),
                                imageHeight = imageHeight.toInt()
                            )
                        ).invokeOnCompletion {
                            Log.d(TAG, "Compress Config Error: $it")
                            navController.navigate(MainRoutes.CompressProgress.route)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Compress",
                        modifier = Modifier.padding(end = 5.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.shrink),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalLines(dates: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val drawPadding: Float = with(LocalDensity.current) { 10.dp.toPx() }
        val lineColor = MaterialTheme.colors.primary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val yStart = 0f
            val yEnd = size.height
            val distance: Float = (size.width.minus(2 * drawPadding)).div(dates.size.minus(1))
            dates.forEachIndexed { index, _ ->
                drawLine(
                    color = lineColor,
                    start = Offset(x = drawPadding + index.times(distance), y = yStart),
                    end = Offset(x = drawPadding + index.times(distance), y = yEnd),
                    strokeWidth = 5f
                )
            }
        }
    }
}

@Composable
fun CompressProgress(
    navController: NavHostController,
    viewModel: MainViewModel,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {

    val context = LocalContext.current
    val activity: ComponentActivity? = context.getActivity()

    if (activity == null) {
        Toast.makeText(
            context,
            "Activity context is null." +
                    " Please try again. " +
                    "If the issue persists please report it.",
            Toast.LENGTH_LONG
        ).show()
        navController.popBackStack()
    } else {
        val totalPhotos by viewModel.toCompress.collectAsState()
        val totalCompressed by viewModel.totalCompressed.collectAsState()
        val totalPathResolved by viewModel.totalImagePathResolved.collectAsState()
        val compressionStatus by viewModel.compressionStatus.collectAsState()

        var progress by remember {
            mutableStateOf(0)
        }

        DisposableEffect(lifecycleOwner) {

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        if (compressionStatus in listOf(
                                ImageCompressorService.COMPRESS_NOT_STARTED,
                                ImageCompressorService.COMPRESS_STOPPED,
                                ImageCompressorService.COMPRESS_DONE
                            )
                        ) {
                            viewModel.startCompressService(context, activity)
                            Log.i(TAG, "CompressProgress: Service bound")
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        viewModel.unbindCompressService(context)
                        Log.i(TAG, "CompressProgress: Service unbound")
                    }
                    else -> {
                        Log.i(TAG, "CompressProgress: Lifecycle.Event: ${event.name}")
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        if (totalPhotos != 0) {
            progress = (totalCompressed * 100) / totalPhotos
        }

        Surface(
            color = MaterialTheme.colors.background
        ) {
            AnimatedVisibility(
                visible = compressionStatus !in listOf(
                    ImageCompressorService.COMPRESS_STOPPED,
                    ImageCompressorService.COMPRESS_DONE
                ),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),
                ) {

                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = (progress / 100f),
                                    modifier = Modifier.size(240.dp)
                                )
                                Text(
                                    text = if (
                                        compressionStatus == ImageCompressorService.COMPRESS_STARTED
                                    ) {
                                        "$progress%"
                                    } else {
                                        compressionStatus
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Total Photos: ")
                            }
                            append(totalPhotos.toString())
                        })

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Total Image Path Resolved: ")
                            }
                            append(totalPathResolved.toString())
                        })

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Total Compressed: ")
                            }
                            append(totalCompressed.toString())
                        })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row {
                        OutlinedButton(
                            onClick = { viewModel.stopCompressService(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Stop")
                        }
                        Button(
                            onClick = {
                                viewModel.startForeground()
                                context.getActivity()?.moveTaskToBack(true)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Background")
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = compressionStatus == ImageCompressorService.COMPRESS_STOPPED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

                LaunchedEffect(Unit) {
                    delay(2000)
                    navController.backQueue.clear()
                    navController.navigate(MainRoutes.Dashboard.route)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        colorFilter = ColorFilter.tint(color = Color(255, 105, 95))
                    )
                    Text(
                        text = "Task Failed 😟",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(
                visible = compressionStatus == ImageCompressorService.COMPRESS_DONE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {

                LaunchedEffect(Unit) {
                    delay(2000)
                    navController.backQueue.clear()
                    navController.navigate(MainRoutes.Dashboard.route)
                }

                Box {
                    Image(
                        painter = painterResource(id = R.drawable.confetti),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.TopCenter,
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_check_circle_24),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            colorFilter = ColorFilter.tint(color = Color(119, 221, 119))
                        )
                        Text(
                            text = "It's Done 🥳",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
package com.durvank883.tinier

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.route.MainRoutes
import com.durvank883.tinier.service.ImageCompressorService
import com.durvank883.tinier.ui.theme.TinierTheme
import com.durvank883.tinier.util.Helper.getActivity
import com.durvank883.tinier.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


val TAG: String? = MainActivity::class.java.canonicalName

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TinierTheme {
                MainScreen()
//                TestScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MainRoutes.Dashboard.route) {

        composable(MainRoutes.Dashboard.route) {
            CheckPermission {
                val viewModel = hiltViewModel<MainViewModel>()
                Dashboard(navController = navController, viewModel = viewModel)
            }
        }

        composable(MainRoutes.CompressConfig.route) {
            val parentEntry = remember {
                navController.getBackStackEntry(MainRoutes.Dashboard.route)
            }
            val parentViewModel = hiltViewModel<MainViewModel>(
                parentEntry
            )
            Compress(navController = navController, viewModel = parentViewModel)
        }

        composable(MainRoutes.CompressProgress.route) {

            BackHandler { /* Do Nothing */ }

            val parentEntry = remember {
                navController.getBackStackEntry(MainRoutes.Dashboard.route)
            }
            val parentViewModel = hiltViewModel<MainViewModel>(
                parentEntry
            )
            CompressProgress(
                navController = navController,
                viewModel = parentViewModel,
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckPermission(content: @Composable () -> Unit) {

    // Track if the user doesn't want to see the rationale any more.
    var doNotShowRationale by rememberSaveable { mutableStateOf(false) }
    val multiplePermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    )

    Surface(
        color = MaterialTheme.colors.background
    ) {
        when {
            // If all permissions are granted, then show screen with the feature enabled
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && multiplePermissionsState.permissions[0].hasPermission) ||
                    multiplePermissionsState.allPermissionsGranted -> {
                content()
            }
            // If the user denied any permission but a rationale should be shown, or the user sees
            // the permissions for the first time, explain why the feature is needed by the app and
            // allow the user decide if they don't want to see the rationale any more.
            multiplePermissionsState.shouldShowRationale ||
                    !multiplePermissionsState.permissionRequested -> {
                if (doNotShowRationale) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(15.dp)
                    ) {
                        val context = LocalContext.current

                        Image(
                            painter = painterResource(id = R.drawable.ic_tinier),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.folders),
                                contentDescription = null,
                                modifier = Modifier.size(128.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Storage read and write permissions are denied. Please, grant us " +
                                        "access on the Settings screen.",
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Settings")
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(15.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_tinier),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.folders),
                                contentDescription = null,
                                modifier = Modifier.size(128.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Tinier requires storage read and write permissions. " +
                                        "Please grant them for the app to function properly.",
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = { doNotShowRationale = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    multiplePermissionsState.launchMultiplePermissionRequest()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Request")
                            }
                        }
                    }
                }
            }
            // If the criteria above hasn't been met, the user denied some permission. Let's present
            // the user with a FAQ in case they want to know more and send them to the Settings screen
            // to enable them the future there if they want to.
            else -> {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                ) {
                    val context = LocalContext.current

                    Image(
                        painter = painterResource(id = R.drawable.ic_tinier),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.folders),
                            contentDescription = null,
                            modifier = Modifier.size(128.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Storage read/write permission is denied." +
                                    "We need the storage permission to access and to save the images locally." +
                                    "We do not store any of your images. " +
                                    "The whole process takes place in your device",
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}

@Composable
fun Dashboard(navController: NavHostController, viewModel: MainViewModel) {

    val photoList by viewModel.photos.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        viewModel.setPhotos(context, uriList)
    }

    val totalSelected by viewModel.totalSelected.collectAsState()
    val isActionMode by viewModel.isActionMode.collectAsState()

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isActionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Text(text = "Selected: $totalSelected")
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleActionMode() }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Exit Action Mode"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.removeSelectedPhotos() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_remove_24),
                                contentDescription = "Remove Photos"
                            )
                        }
                        IconButton(onClick = { viewModel.selectAllPhotos() }) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Select All Photos"
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.secondary,
                    elevation = 12.dp
                )
            }

            AnimatedVisibility(
                visible = !isActionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Text(text = "Tinier")
                    },
                    actions = {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.sum_24),
                                contentDescription = "Add Photos",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.primarySurface,
                    elevation = 12.dp
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isActionMode && !photoList.isNullOrEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(MainRoutes.CompressConfig.route) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Proceed"
                    )
                }
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colors.background
        ) {
            if (photoList.isNullOrEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gallery),
                        contentDescription = "No Photos",
                        modifier = Modifier.padding(10.dp)
                    )
                    Text(text = "There are no photos here!", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Start adding your photos",
                        modifier = Modifier.padding(5.dp),
                        fontSize = 12.sp
                    )
                }
            } else {
                PhotoGrid(photoList = photoList.toList())
            }
        }
    }
}

@Composable
fun <T> LazyGridFor(
    items: List<T>,
    rowSize: Int = 1,
    listState: LazyListState,
    itemContent: @Composable BoxScope.(T) -> Unit,
) {
    LazyColumn(state = listState) {
        items(items = items.chunked(rowSize), key = { row -> row.hashCode() }) { row ->
            Row(
                modifier = Modifier.fillParentMaxWidth(),
            ) {
                for ((index, item) in row.withIndex()) {
                    Box(
                        Modifier.fillMaxWidth(1f / (rowSize - index)),
                        contentAlignment = Alignment.Center
                    ) {
                        itemContent(item)
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGrid(photoList: List<Photo>, viewModel: MainViewModel = viewModel()) {

    val isActionMode by viewModel.isActionMode.collectAsState()
    val listState = rememberLazyListState()

    LazyGridFor(
        items = photoList,
        rowSize = LocalConfiguration.current.screenWidthDp / 128,
        listState = listState
    ) { photo ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.wrapContentSize()
        ) {
            Image(
                painter = rememberImagePainter(
                    data = photo.uri,
                    builder = {
                        crossfade(true)
                        placeholder(R.drawable.ic_image_24)
                    }),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp)
                    .padding(2.dp)
                    .clip(shape = RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (isActionMode) {
                                    Log.d(TAG, "PhotoGrid: Tapped on ${photo.uri}")
                                    viewModel.togglePhotoSelection(photo)
                                }
                            },
                            onLongPress = {
                                if (!isActionMode) {
                                    viewModel.toggleActionMode()
                                }
                                viewModel.togglePhotoSelection(photo)
                            }
                        )
                    },
                contentScale = ContentScale.Crop
            )

            if (photo.isSelected) {
                Image(
                    painter = rememberImagePainter(
                        data = R.drawable.ic_check_circle_24,
                        builder = {
                            crossfade(true)
                        }),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
                )
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
                            trailingName = fileName
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
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        viewModel.unbindCompressService(context)
                    }
                    else -> {}
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
                        Spacer(modifier = Modifier.height(10.dp))

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
                            onClick = { },
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

@Composable
fun TestScreen() {
    val isCompleted by remember {
        mutableStateOf(false)
    }

    Surface(
        color = MaterialTheme.colors.background
    ) {
        AnimatedVisibility(
            visible = !isCompleted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
        AnimatedVisibility(visible = isCompleted) {
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
                    Text(text = "It's Done 🥳", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TinierTheme {
        TestScreen()
    }
}
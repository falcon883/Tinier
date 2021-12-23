package com.durvank883.tinier

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.route.MainRoutes
import com.durvank883.tinier.ui.theme.Purple200
import com.durvank883.tinier.ui.theme.TinierTheme
import com.durvank883.tinier.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
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

        composable(MainRoutes.Compress.route) {
            val parentEntry = remember {
                navController.getBackStackEntry(MainRoutes.Dashboard.route)
            }
            val parentViewModel = hiltViewModel<MainViewModel>(
                parentEntry
            )
            Compress(navController = navController, viewModel = parentViewModel)
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
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(15.dp)
                    ) {
                        val context = LocalContext.current

                        Text(
                            "Storage read and write permissions are denied. Please, grant us " +
                                    "access on the Settings screen."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(onClick = { doNotShowRationale = true }) {
                                Text("Don't show again")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    multiplePermissionsState.launchMultiplePermissionRequest()
                                }
                            ) {
                                Text("Request permissions")
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
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                ) {
                    val context = LocalContext.current

                    Text(
                        "Storage read/write permission is denied. See this FAQ with " +
                                "information about why we need this permission. Please, grant us " +
                                "access on the Settings screen."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Dashboard(navController: NavHostController, viewModel: MainViewModel) {

    val photoList by viewModel.photos.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        viewModel.setPhotos(uriList)
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
                    backgroundColor = Color.DarkGray,
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
                    onClick = { navController.navigate(MainRoutes.Compress.route) },
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

@OptIn(ExperimentalFoundationApi::class)
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
                    .padding(3.dp)
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
                    colorFilter = ColorFilter.tint(Color.DarkGray)
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val yStart = 0f
            val yEnd = size.height
            val distance: Float = (size.width.minus(2 * drawPadding)).div(dates.size.minus(1))
            dates.forEachIndexed { index, _ ->
                drawLine(
                    color = Purple200,
                    start = Offset(x = drawPadding + index.times(distance), y = yStart),
                    end = Offset(x = drawPadding + index.times(distance), y = yEnd),
                    strokeWidth = 5f
                )
            }
        }
    }
}

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
            val totalPhotos = viewModel.photos.collectAsState().value.size
            var sliderPosition by remember { mutableStateOf(80f) }
            var expanded by remember { mutableStateOf(false) }
            val items = listOf("original", "png", "jpg", "webp")
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
                        append("Image Size:\n")
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
                    label = { Text("Enter image size") },
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth(),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                            text = items[selectedIndex],
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
                        items.forEachIndexed { index, s ->
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                Button(
                    onClick = { /*TODO*/ },
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
fun TestScreen() {
    Surface(
        color = MaterialTheme.colors.background
    ) {
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
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = { }) {
                    Text("Don't show again")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { }
                ) {
                    Text("Request permissions")
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
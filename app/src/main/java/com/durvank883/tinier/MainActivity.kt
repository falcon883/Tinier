package com.durvank883.tinier

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.durvank883.tinier.prefs.SettingsDataStore
import com.durvank883.tinier.route.MainRoutes
import com.durvank883.tinier.ui.theme.TinierTheme
import com.durvank883.tinier.util.ThemeMode
import com.durvank883.tinier.viewmodel.MainViewModel
import com.durvank883.tinier.viewmodel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


val TAG: String? = MainActivity::class.java.canonicalName

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settingsDataStore.themeModeFlow.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM_DEFAULT_THEME.modeNo -> isSystemInDarkTheme()
                ThemeMode.LIGHT_THEME.modeNo -> false
                ThemeMode.DARK_THEME.modeNo -> true
                else -> isSystemInDarkTheme()
            }

            TinierTheme(darkTheme = isDarkTheme) {
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

        composable(MainRoutes.Settings.route) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            Settings(navController = navController, viewModel = viewModel)
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
                                painter = painterResource(id = R.drawable.remove_image_96),
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
                                painter = painterResource(id = R.drawable.add_image_96),
                                contentDescription = "Add Photos",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { navController.navigate(MainRoutes.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
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
    LazyColumn(state = listState, modifier = Modifier
        .fillMaxSize()
        .padding(2.dp)) {
        items(items = items.chunked(rowSize)) { row ->
            LazyRow(
                modifier = Modifier.fillParentMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsIndexed(items = row, key = { _, item -> item.hashCode() }) { index, item ->
                    Box(modifier = Modifier.fillMaxWidth(1f / (rowSize - index))) {
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
    var imageSize by remember { mutableStateOf(128) }
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val rowSize = maxOf(screenWidth / imageSize, 1).also {
        imageSize = (screenWidth / it) - (4 * 2)
    }

    LazyGridFor(
        items = photoList,
        rowSize = rowSize,
        listState = listState
    ) { photo ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .wrapContentSize()
                .padding(2.dp)
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
                    .size(imageSize.dp)
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
                    modifier = Modifier
                        .size(64.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.secondary)
                )
            }
        }
    }
}

@Composable
fun TestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    TestScreen()
}
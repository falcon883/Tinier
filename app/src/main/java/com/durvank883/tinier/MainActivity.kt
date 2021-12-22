package com.durvank883.tinier

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.route.MainRoutes
import com.durvank883.tinier.ui.theme.TinierTheme
import com.durvank883.tinier.viewmodel.MainViewModel


val TAG: String? = MainActivity::class.java.canonicalName

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
            Dashboard(navController = navController)
        }

        composable(MainRoutes.Compress.route) {
            Compress(navController = navController)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Dashboard(navController: NavHostController, viewModel: MainViewModel = viewModel()) {
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

//    LazyVerticalGrid(cells = GridCells.Adaptive(minSize = 128.dp), state = listState) {
//        items(photoList) { photo ->
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier.wrapContentSize()
//            ) {
//                Image(
//                    painter = rememberImagePainter(
//                        data = photo.uri,
//                        builder = {
//                            crossfade(true)
//                            placeholder(R.drawable.ic_image_24)
//                        }),
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(128.dp)
//                        .padding(3.dp)
//                        .pointerInput(Unit) {
//                            detectTapGestures(
//                                onTap = {
//                                    if (isActionMode) {
//                                        Log.d(TAG, "PhotoGrid: Tapped on ${photo.uri}")
//                                        viewModel.togglePhotoSelection(photo)
//                                    }
//                                },
//                                onLongPress = {
//                                    if (!isActionMode) {
//                                        viewModel.toggleActionMode()
//                                    }
//                                    viewModel.togglePhotoSelection(photo)
//                                }
//                            )
//                        },
//                    contentScale = ContentScale.Crop
//                )
//
//                if (photo.isSelected) {
//                    Image(
//                        painter = rememberImagePainter(
//                            data = R.drawable.ic_check_circle_24,
//                            builder = {
//                                crossfade(true)
//                            }),
//                        contentDescription = null,
//                        modifier = Modifier.size(64.dp),
//                        colorFilter = ColorFilter.tint(Color.DarkGray)
//                    )
//                }
//            }
//        }
//    }
}

@Composable
fun Compress(navController: NavHostController, viewModel: MainViewModel = viewModel()) {
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

        }
    }
}

@Composable
fun TestScreen() {

    var actionMode by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            if (actionMode) {
                TopAppBar(
                    title = {
                        Text(text = "Selected: 10")
                    },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Exit Action Mode"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_remove_24),
                                contentDescription = "Remove Photos"
                            )
                        }
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Select All Photos"
                            )
                        }
                    },
                    backgroundColor = Color.DarkGray,
                    elevation = 12.dp
                )
            } else {
                TopAppBar(
                    title = {
                        Text(text = "Tinier")
                    },
                    actions = {
                        IconButton(onClick = { }) {
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
            FloatingActionButton(
                onClick = { actionMode = !actionMode },
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Add New Photos"
                )
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colors.background
        ) {
            val data = (1..100).map(Integer::toString)
            val listState = rememberLazyListState()

            LazyGridFor(
                items = data,
                rowSize = LocalConfiguration.current.screenWidthDp / 128,
                listState = listState
            ) { item ->
                Image(
                    painter = painterResource(id = R.drawable.gallery),
                    contentDescription = null,
                )
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
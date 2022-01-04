package com.durvank883.tinier.route

sealed class MainRoutes(val route: String) {
    object Dashboard : MainRoutes("dashboard")
    object CompressConfig : MainRoutes("compress_config")
    object CompressProgress : MainRoutes("compress_progress")
    object Settings : MainRoutes("settings")
}

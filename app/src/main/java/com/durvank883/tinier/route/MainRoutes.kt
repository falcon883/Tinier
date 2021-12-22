package com.durvank883.tinier.route

sealed class MainRoutes(val route: String) {
    object Dashboard : MainRoutes("dashboard")
    object Compress : MainRoutes("compress")
}

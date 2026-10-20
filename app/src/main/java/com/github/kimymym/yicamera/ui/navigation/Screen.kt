package com.github.kimymym.yicamera.ui.navigation

sealed class Screen(val route: String) {
    data object Connect : Screen("connect")
    data object Main : Screen("main")
}

package com.yodgorbek.jellyapp.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yodgorbek.jellyapp.presentation.camera.CameraScreen
import com.yodgorbek.jellyapp.presentation.feed.FeedScreen
import com.yodgorbek.jellyapp.presentation.gallery.GalleryScreen
import com.yodgorbek.jellyapp.presentation.detail.DetailScreen
import androidx.compose.runtime.getValue
import com.yodgorbek.jellyapp.presentation.player.VideoPlayerScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "feed",
                    onClick = { navController.navigate("feed") },
                    icon = { Icon(Icons.Filled.DynamicFeed, contentDescription = "Feed") },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = currentRoute == "camera",
                    onClick = { navController.navigate("camera") },
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera") },
                    label = { Text("Camera") }
                )
                NavigationBarItem(
                    selected = currentRoute == "gallery",
                    onClick = { navController.navigate("gallery") },
                    icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("Gallery") }
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "feed") {
            composable("feed") { FeedScreen(navController = navController) }
            composable("camera") { CameraScreen(navController = navController) }
            composable("gallery") { GalleryScreen(navController = navController) }
            composable("player/{videoUrl}") { backStackEntry ->
                //val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                val encodedUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                val videoUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                VideoPlayerScreen(navController = navController, videoUrl = videoUrl)
            }
            composable("detail/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                DetailScreen(videoId = videoId)
            }
        }
    }
}
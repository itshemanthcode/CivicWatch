package com.example.claudeapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.claudeapp.R
import com.example.claudeapp.ui.screens.auth.AuthScreen
import com.example.claudeapp.ui.screens.auth.LoginScreen
import com.example.claudeapp.ui.screens.auth.SignupScreen
import com.example.claudeapp.ui.screens.auth.EmailVerificationScreen
import com.example.claudeapp.ui.screens.splash.SplashScreen
import com.example.claudeapp.ui.screens.home.SimpleModernHomeScreen
import com.example.claudeapp.ui.screens.map.MapScreen
import com.example.claudeapp.ui.screens.report.ReportScreen
import com.example.claudeapp.ui.screens.ranking.RankingScreen
import com.example.claudeapp.ui.screens.profile.ProfileScreen
import com.example.claudeapp.data.preferences.ThemeState

// CivicWatch Brand Colors
private val BrandPrimary = Color(0xFF1E3A8A) // Dark Blue
private val BrandSecondary = Color(0xFF4CAF50) // Green
private val BrandAccent = Color(0xFF1976D2) // Light Blue
private val DarkSurface = Color(0xFF1E1E1E) // Dark surface for bottom nav
private val LightText = Color(0xFFFFFFFF) // White text
private val SubtleText = Color(0xFFB0B0B0) // Subtle text

@Composable
fun CivicWatchNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onGoogleSignInClick: () -> Unit = {},
    onGoogleSignInResult: ((com.google.android.gms.auth.api.signin.GoogleSignInAccount?) -> Unit) -> Unit = {},
    onRequestPermissions: ((Boolean) -> Unit) -> Unit = {},
    permissionsGranted: Boolean = false
) {
    // Start with splash screen, then navigate to login
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            println("CivicWatchNavigation: Showing splash screen")
            SplashScreen(
                onSplashFinished = {
                    println("CivicWatchNavigation: Splash finished, navigating to login")
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    println("CivicWatchNavigation: User already signed in, navigating to main")
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onRequestPermissions = onRequestPermissions,
                permissionsGranted = permissionsGranted
            )
        }
        composable("login") {
            LoginScreen(
                onAuthSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate("signup")
                },
                onGoogleSignInClick = onGoogleSignInClick,
                onGoogleSignInResult = onGoogleSignInResult
            )
        }
        composable("signup") {
            SignupScreen(
                onAuthSuccess = {
                    navController.navigate("email_verification") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate("login") },
                onGoogleSignInClick = onGoogleSignInClick,
                onGoogleSignInResult = onGoogleSignInResult
            )
        }
        
        composable("email_verification") {
            EmailVerificationScreen(
                onVerificationSuccess = {
                    navController.navigate("main") {
                        popUpTo("email_verification") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.navigate("login") }
            )
        }
        
        composable("main") {
            MainScreen(navController = navController)
        }
    }
}

@Composable
fun MainScreen(
    navController: NavHostController
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = bottomNavController
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable("home") {
                SimpleModernHomeScreen(
                    navController = bottomNavController,
                    highlightIssueId = null,
                    onNavigateToReport = {
                        bottomNavController.navigate("report")
                    },
                    onNavigateToMap = {
                        bottomNavController.navigate("map")
                    },
                    onNavigateToProfile = {
                        bottomNavController.navigate("profile")
                    },
                    onNavigateToRanking = {
                        bottomNavController.navigate("ranking")
                    }
                )
            }
            composable(
                "home_issue/{issueId}",
                arguments = listOf(androidx.navigation.navArgument("issueId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val issueId = backStackEntry.arguments?.getString("issueId")
                SimpleModernHomeScreen(
                    navController = bottomNavController,
                    highlightIssueId = issueId,
                    onNavigateToReport = {
                        bottomNavController.navigate("report")
                    },
                    onNavigateToMap = {
                        bottomNavController.navigate("map")
                    },
                    onNavigateToProfile = {
                        bottomNavController.navigate("profile")
                    },
                    onNavigateToRanking = {
                        bottomNavController.navigate("ranking")
                    }
                )
            }
            composable("map/{issueId}") { backStackEntry ->
                val issueId = backStackEntry.arguments?.getString("issueId")
                MapScreen(selectedIssueId = issueId)
            }
            composable("map") {
                MapScreen()
            }
            composable("report") {
                ReportScreen(
                    onBack = {
                        bottomNavController.popBackStack()
                        // Refresh home screen when returning
                        bottomNavController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }
            composable("ranking") {
                RankingScreen(navController = bottomNavController)
            }
            composable(
                "user_issues/{userId}?initialIssueId={initialIssueId}",
                arguments = listOf(
                    androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("initialIssueId") { 
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val initialIssueId = backStackEntry.arguments?.getString("initialIssueId")
                com.example.claudeapp.ui.screens.profile.UserIssuesScreen(
                    navController = bottomNavController,
                    userId = userId,
                    initialIssueId = initialIssueId
                )
            }
            composable("profile") {
                ProfileScreen(
                    navController = bottomNavController,
                    onSignOut = {
                        // Navigate back to login screen using parent navController
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onIssueClick = { issueId ->
                        // Get current user ID from Firebase Auth
                        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@ProfileScreen
                        bottomNavController.navigate("user_issues/$userId?initialIssueId=$issueId")
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val items = listOf(
        BottomNavItem("home", stringResource(R.string.nav_home), Icons.Default.Home),
        BottomNavItem("map", stringResource(R.string.nav_map), Icons.Default.LocationOn),
        BottomNavItem("report", stringResource(R.string.nav_report), Icons.Default.Add),
        BottomNavItem("ranking", stringResource(R.string.nav_ranking), Icons.Default.EmojiEvents), // Trophy icon
        BottomNavItem("profile", stringResource(R.string.nav_profile), Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = Modifier.height(64.dp) // Reduced height from default 80dp
    ) {
        NavigationBar(
            containerColor = DarkSurface // Dark background
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { 
                        Icon(
                            item.icon, 
                            contentDescription = item.title,
                            tint = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                                BrandSecondary // Green for selected
                            } else {
                                SubtleText // Subtle gray for unselected
                            }
                        ) 
                    },
                    label = { 
                        Text(
                            text = item.title,
                            color = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                                BrandSecondary // Green for selected
                            } else {
                                SubtleText // Subtle gray for unselected
                            }
                        ) 
                    },
                    selected = currentDestination?.hierarchy?.any { 
                        it.route == item.route || 
                        (item.route == "home" && it.route?.startsWith("home_issue") == true) ||
                        (item.route == "profile" && it.route?.startsWith("user_issues") == true)
                    } == true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandSecondary,
                        selectedTextColor = BrandSecondary,
                        unselectedIconColor = SubtleText,
                        unselectedTextColor = SubtleText,
                        indicatorColor = BrandSecondary.copy(alpha = 0.2f) // Subtle green highlight
                    ),
                    onClick = {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

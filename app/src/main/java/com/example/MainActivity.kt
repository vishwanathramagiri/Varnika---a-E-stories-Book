package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.VarnikaViewModel
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SanctuaryScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.VaultLockScreen
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.VarnikaTheme

const val ROUTE_LIBRARY = "library"
const val ROUTE_STUDIO = "studio"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_SANCTUARY = "sanctuary"
const val ROUTE_READER = "reader"

data class VarnikaNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_ITEMS = listOf(
    VarnikaNavItem(
        route = ROUTE_LIBRARY,
        labelRes = R.string.nav_library,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    ),
    VarnikaNavItem(
        route = ROUTE_STUDIO,
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote
    ),
    VarnikaNavItem(
        route = ROUTE_INSIGHTS,
        labelRes = R.string.nav_insights,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    VarnikaNavItem(
        route = ROUTE_SANCTUARY,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VarnikaViewModel = viewModel(factory = VarnikaViewModel.Factory)
            VarnikaAppRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun VarnikaAppRoot(viewModel: VarnikaViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()

    VarnikaTheme(themeMode = preferences.themeMode) {
        if (preferences.vaultLockEnabled && !isVaultUnlocked) {
            VaultLockScreen(onUnlockAttempt = viewModel::unlockWithPinOrRecovery)
        } else {
            VarnikaMainShell(viewModel = viewModel)
        }
    }
}

@Composable
fun VarnikaMainShell(viewModel: VarnikaViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_LIBRARY

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val allStories by viewModel.stories.collectAsStateWithLifecycle()
    val filteredStories by viewModel.filteredStories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedShelf by viewModel.selectedShelf.collectAsStateWithLifecycle()
    val isConnected by viewModel.isNetworkConnected.collectAsStateWithLifecycle()

    val activeStory by viewModel.activeStory.collectAsStateWithLifecycle()
    val activeChapters by viewModel.activeChapters.collectAsStateWithLifecycle()
    val activeVersions by viewModel.activeVersions.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeAuthorMessages.collectAsStateWithLifecycle()
    val activeHighlights by viewModel.activeStoryHighlights.collectAsStateWithLifecycle()
    val allHighlights by viewModel.allHighlights.collectAsStateWithLifecycle()
    val readingSessions by viewModel.readingSessions.collectAsStateWithLifecycle()
    val cacheTelemetry by viewModel.cacheTelemetry.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.snackEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isReaderRoute = currentRoute == ROUTE_READER

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && !isReaderRoute

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail && !isReaderRoute) {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_LIBRARY,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_LIBRARY) {
                        LibraryScreen(
                            stories = filteredStories,
                            preferences = preferences,
                            searchQuery = searchQuery,
                            selectedShelf = selectedShelf,
                            isNetworkConnected = isConnected,
                            onSearchChange = viewModel::setSearchQuery,
                            onShelfSelect = viewModel::setSelectedShelf,
                            onApplyPreset = viewModel::applyAccessibilityPreset,
                            onToggleTheme = {
                                val next = if (preferences.themeMode == ThemeMode.DARK) {
                                    ThemeMode.LIGHT
                                } else {
                                    ThemeMode.DARK
                                }
                                viewModel.setThemeMode(next)
                            },
                            onSyncNow = viewModel::syncLocalVaultNow,
                            onOpenStory = { storyId ->
                                viewModel.selectStory(storyId)
                                navController.navigate(ROUTE_READER)
                            },
                            onOpenStudio = { storyId ->
                                viewModel.selectStory(storyId)
                                navigateTopLevel(ROUTE_STUDIO)
                            },
                            onToggleOffline = { story ->
                                viewModel.toggleOfflineCached(story.id, story.isOfflineCached)
                            },
                            onChangeShelf = { story, newShelf ->
                                viewModel.updateStoryShelf(story.id, newShelf)
                            },
                            onImportTextFile = viewModel::importStoryFile,
                            onCreateStory = { title, sub, author, shelf, age, syn, body, preset, uri, collab, cA, cB ->
                                viewModel.createOrUploadStory(
                                    title, sub, author, shelf, age, syn, body,
                                    preset, uri, collab, cA, cB
                                ) {
                                    navController.navigate(ROUTE_READER)
                                }
                            }
                        )
                    }

                    composable(ROUTE_READER) {
                        ReaderScreen(
                            story = activeStory,
                            chapters = activeChapters,
                            highlights = activeHighlights,
                            preferences = preferences,
                            ttsState = ttsState,
                            onBack = { navController.popBackStack() },
                            onOpenStudio = { storyId ->
                                viewModel.selectStory(storyId)
                                navController.navigate(ROUTE_STUDIO) {
                                    popUpTo(ROUTE_LIBRARY)
                                }
                            },
                            onStartSession = viewModel::startReadingTimer,
                            onStopSession = viewModel::stopAndRecordReadingSession,
                            onNavigateChapter = viewModel::navigateChapter,
                            onPlayTts = viewModel::playTts,
                            onStopTts = viewModel::stopTts,
                            onChangeTtsRate = viewModel::setTtsRate,
                            onSaveHighlight = { chIdx, paraIdx, quote, note, color ->
                                activeStory?.let { story ->
                                    viewModel.saveHighlight(
                                        story.id, story.title, chIdx, paraIdx, quote, note, color
                                    )
                                }
                            },
                            onSetFontSize = viewModel::setFontSize,
                            onSetLineHeight = viewModel::setLineHeight,
                            onSetReaderFont = viewModel::setReaderFont,
                            onSetThemeMode = viewModel::setThemeMode,
                            onApplyPreset = viewModel::applyAccessibilityPreset,
                            onSetHaptics = viewModel::setHapticPageTurn
                        )
                    }

                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            stories = allStories,
                            activeStory = activeStory ?: allStories.firstOrNull(),
                            chapters = activeChapters,
                            versions = activeVersions,
                            messages = activeMessages,
                            authorName = preferences.readerDisplayName,
                            onSelectStory = viewModel::selectStory,
                            onCommitRevision = { story, chapter, title, content, note ->
                                viewModel.commitChapterRevision(story.id, chapter, title, content, note)
                            },
                            onAddChapter = viewModel::addChapterToStory,
                            onRestoreVersion = viewModel::restoreVersion,
                            onComputeDiff = viewModel::computeDiff,
                            onPostMessage = viewModel::postAuthorMessage
                        )
                    }

                    composable(ROUTE_INSIGHTS) {
                        InsightsScreen(
                            sessions = readingSessions,
                            highlights = allHighlights,
                            preferences = preferences,
                            cacheTelemetry = cacheTelemetry,
                            onDeleteHighlight = viewModel::deleteHighlight,
                            onExportEncrypted = viewModel::exportEncryptedBackup,
                            onImportEncrypted = viewModel::importEncryptedBackup
                        )
                    }

                    composable(ROUTE_SANCTUARY) {
                        SanctuaryScreen(
                            preferences = preferences,
                            onUpdateDisplayName = viewModel::setReaderDisplayName,
                            onSetThemeMode = viewModel::setThemeMode,
                            onApplyPreset = viewModel::applyAccessibilityPreset,
                            onSetFontSize = viewModel::setFontSize,
                            onSetHaptics = viewModel::setHapticPageTurn,
                            onSetTravelBatterySaver = viewModel::setTravelBatterySaver,
                            onSetOfflineOnly = viewModel::setOfflineOnlyMode,
                            onSetDailyGoal = viewModel::setDailyGoalMinutes,
                            onSetReminderSchedule = viewModel::setReminderSchedule,
                            onSendTestReminder = viewModel::sendTestReminderNow,
                            onEnableVaultLock = viewModel::enableVaultLock,
                            onDisableVaultLock = viewModel::disableVaultLock,
                            onSyncLocalVault = viewModel::syncLocalVaultNow
                        )
                    }
                }
            }
        }
    }
}

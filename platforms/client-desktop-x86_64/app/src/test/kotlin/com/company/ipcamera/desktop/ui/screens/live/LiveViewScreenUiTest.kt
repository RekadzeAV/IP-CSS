package com.company.ipcamera.desktop.ui.screens.live

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.company.ipcamera.desktop.ui.viewmodel.LiveViewViewModel
import com.company.ipcamera.desktop.ui.viewmodel.GridLayout
import com.company.ipcamera.shared.domain.usecase.GetCamerasUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.prefs.Preferences

class LiveViewScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var getCamerasUseCase: GetCamerasUseCase

    @Before
    fun setup() {
        getCamerasUseCase = mock()
        runBlocking {
            whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())
        }
        Preferences.userRoot().node("com.company.ipcamera.desktop.live.state").clear()
    }

    @Test
    fun `auto chip switches mode label from custom to auto`() {
        val dispatcher = StandardTestDispatcher()
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = CoroutineScope(dispatcher)
        )

        composeRule.setContent {
            LiveViewScreen(viewModel = viewModel)
        }

        viewModel.setVisibilityGraceMs(2400L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Mode: Custom").fetchSemanticsNode()

        composeRule.onNodeWithText("Auto").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Mode: Auto").fetchSemanticsNode()
    }

    @Test
    fun `auto chip in grid 16 restores recommended 3000ms`() {
        val dispatcher = StandardTestDispatcher()
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = CoroutineScope(dispatcher)
        )

        composeRule.setContent {
            LiveViewScreen(viewModel = viewModel)
        }

        viewModel.setGridLayout(GridLayout.GRID_16)
        viewModel.setVisibilityGraceMs(2400L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Mode: Custom").fetchSemanticsNode()

        composeRule.onNodeWithText("Auto").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("текущее=3000ms (Auto)", substring = true).fetchSemanticsNode()
    }

    @Test
    fun `auto mode updates grace when layout changes from grid4 to grid16`() {
        val dispatcher = StandardTestDispatcher()
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = CoroutineScope(dispatcher)
        )

        composeRule.setContent {
            LiveViewScreen(viewModel = viewModel)
        }

        viewModel.setGridLayout(GridLayout.GRID_4)
        viewModel.setVisibilityGraceAuto()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("текущее=500ms (Auto)", substring = true).fetchSemanticsNode()

        viewModel.setGridLayout(GridLayout.GRID_16)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("текущее=3000ms (Auto)", substring = true).fetchSemanticsNode()
    }

    @Test
    fun `custom mode keeps grace when layout changes`() {
        val dispatcher = StandardTestDispatcher()
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = CoroutineScope(dispatcher)
        )

        composeRule.setContent {
            LiveViewScreen(viewModel = viewModel)
        }

        viewModel.setGridLayout(GridLayout.GRID_4)
        viewModel.setVisibilityGraceMs(2200L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("текущее=2200ms (Custom)", substring = true).fetchSemanticsNode()

        viewModel.setGridLayout(GridLayout.GRID_16)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("текущее=2200ms (Custom)", substring = true).fetchSemanticsNode()
    }
}

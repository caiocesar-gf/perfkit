package com.perfkit.debugui.panel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.perfkit.core.PerfKit

class ViolationPanelActivity : ComponentActivity() {

    private val viewModel by viewModels<ViolationPanelViewModel> {
        ViolationPanelViewModel.factory(
            observeViolations = PerfKit.observeViolations,
            observeSummaries = PerfKit.observeSummaries,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ViolationPanelScreen(viewModel)
            }
        }
    }
}

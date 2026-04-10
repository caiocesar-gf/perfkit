package com.perfkit.debugui.panel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.perfkit.core.PerfKit

/**
 * Activity que exibe o painel completo de violações StrictMode.
 *
 * Não requer Hilt — usa factory manual que obtém os use cases via [PerfKit].
 * Declarada no AndroidManifest do sdk-debug-ui e mesclada automaticamente
 * no manifest do app consumidor pelo Gradle.
 */
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

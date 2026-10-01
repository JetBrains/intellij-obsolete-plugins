package com.intellij.aidebugger.python.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.extensionPoints.TraceEventViewModelComponentSelector
import com.intellij.aidebugger.common.viewModels.EventVM
import kotlin.reflect.KClass

open class LangChainViewModel : EventVM()

class LangChainTraceEventViewModelComponentSelector : TraceEventViewModelComponentSelector<LangChainViewModel> {
    override val vmClass: KClass<LangChainViewModel>
        get() = LangChainViewModel::class

    @Composable
    override fun componentSelector(
        viewModel: LangChainViewModel,
        modifier: Modifier
    ): (@Composable () -> Unit)? {
        when (viewModel) {
            is LangChainPromptTemplateViewModel -> LangChainPromptTemplateView(viewModel, modifier)
            is LangChainPrettyKeyValueTableViewModel -> LangChainPrettyKeyValueTable(viewModel.headerName, viewModel.map, modifier)
        }
        return null
    }
}

class LangChainPromptTemplateViewModel(
    val inputs: Map<*, *>,
    val outputText: String,
    val outputTextHTML: String? = null,
) : LangChainViewModel()

class LangChainPrettyKeyValueTableViewModel(
    val headerName: String? = null,
    val map: Map<*, *>,
): LangChainViewModel()
package com.intellij.aidebugger.evaluation

import androidx.compose.ui.geometry.Rect
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.common.onboarding.AnchorBus
import com.intellij.aidebugger.common.onboarding.AnchorSink
import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingExecutor
import com.intellij.aidebugger.evaluation.onboarding.OnboardingAnchorRegistry
import com.intellij.aidebugger.evaluation.onboarding.StartOnboardingListener
import com.intellij.aidebugger.evaluation.onboarding.ui.GotItOnboardingUiBridge
import com.intellij.aidebugger.evaluation.viewModels.DatasetsViewModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.scale.JBUIScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EvaluationStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<EvaluationToolWindowManager>()

        val listener = project.service<DatasetChangeListener>()
        listener.start()

        EvaluationOnboardingExecutor.setUiBridge(GotItOnboardingUiBridge(project))
        AnchorBus.sink = registerAnchorSink(project)

        val onboardingListener = project.service<StartOnboardingListener>()
        onboardingListener.start()
    }

    private fun registerAnchorSink(project: Project) =
        object : AnchorSink {
            override fun set(key: String, rect: Rect) {
                // need to adjust a compose component scale
                val finalRect = if (key == OnboardingAnchorKeys.ADD_TO_DATASET_BUTTON) {
                    val frameComp = WindowManager.getInstance().getIdeFrame(project)?.component
                    if (frameComp != null) {
                        val scale = JBUIScale.sysScale(frameComp)
                        if (scale > 1f) {
                            val scaled = Rect(
                                rect.left / scale,
                                rect.top / scale,
                                rect.right / scale,
                                rect.bottom / scale
                            )
                            scaled
                        } else rect
                    } else rect
                } else rect

                OnboardingAnchorRegistry.set(key, finalRect)
            }

            override fun setFlag(key: String, isActive: Boolean) {
                if (OnboardingAnchorRegistry.getFlag(key) == isActive) return
                OnboardingAnchorRegistry.setFlag(key, isActive)
            }

            override fun remove(key: String) {
                OnboardingAnchorRegistry.remove(key)
            }

            override fun removeFlag(key: String) {
                OnboardingAnchorRegistry.removeFlag(key)
            }
        }
}

@Service(Service.Level.PROJECT)
class DatasetChangeListener(
    private val project: Project,
    private val scope: CoroutineScope
) {
    fun start() {
        val repo = project.service<TracesDatasetsRepository>()

        scope.launch {
            repo.datasetContentChanged.collect { datasetName ->
                if (datasetName != null) {
                    ApplicationManager.getApplication().invokeLater {
                        openEvaluationToolWindowAndSelectDataset(project, datasetName)
                    }
                }
            }
        }
    }

    private fun openEvaluationToolWindowAndSelectDataset(project: Project, datasetName: String) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(EvaluationToolWindowFactory.ID)
            ?: return

        toolWindow.show {
            val contentManager = toolWindow.contentManager
            val datasetsTab = contentManager.contents.getOrNull(0) ?: return@show

            contentManager.setSelectedContent(datasetsTab, true)

            val datasetsViewModel = project.service<DatasetsViewModel>()
            datasetsViewModel.selectDataset(datasetName)
        }
    }
}
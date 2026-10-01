package com.intellij.aidebugger.common.toolWindow

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.intellij.openapi.project.Project

val LocalIdeaProject: ProvidableCompositionLocal<Project?> = staticCompositionLocalOf { null }
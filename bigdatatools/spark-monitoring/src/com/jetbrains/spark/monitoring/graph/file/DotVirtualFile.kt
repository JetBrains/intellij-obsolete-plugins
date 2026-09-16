package com.jetbrains.spark.monitoring.graph.file

import com.intellij.testFramework.LightVirtualFile

data class DotVirtualFile(val connectionId: String,
                          val appId: String,
                          val jobId: String) : LightVirtualFile("Job $jobId", DotFileType, "")
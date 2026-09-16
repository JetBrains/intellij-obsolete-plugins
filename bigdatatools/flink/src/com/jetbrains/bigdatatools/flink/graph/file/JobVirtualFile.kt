package com.jetbrains.bigdatatools.flink.graph.file

import com.intellij.testFramework.LightVirtualFile

data class JobVirtualFile(val connectionId: String, val jobId: String) : LightVirtualFile("Job $jobId", JobFileType, "")
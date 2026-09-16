package com.intellij.bigdatatools.databricks.model

data class ClusterConfiguration(val user: String?, val clusters: List<ClusterInfoPresentable>)
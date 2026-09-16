package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.completion

import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfCompletionUtils
import com.intellij.completion.ml.features.RankingFeaturesOverrides

class SparkScalaRankingFeaturesOverrides : RankingFeaturesOverrides {

  private val mlBaseFeatures = mapOf(
    "ml_ngram_recent_files" to "2.0",
    "ml_recent_places_children_contains" to "2",
    "ml_recent_places_contains" to "2",
    "ml_vcs_declaration_is_changed" to "2",
    "ml_scala_name_name_sim" to 5.0
  )

  override fun getMlElementFeaturesOverrides(features: Map<String, Any>): Map<String, Any> {
    if (DfCompletionUtils.isEnabled()) {
      return super.getDefaultWeigherFeaturesOverrides(features)
    }

    if (features["ml_common_contributor"] != "DfColumnNamesCompletionContributor") return emptyMap()

    val overrides = mutableMapOf<String, Any>()
    for ((name, default) in mlBaseFeatures)
      overrides[name] = default

    return overrides
  }
}
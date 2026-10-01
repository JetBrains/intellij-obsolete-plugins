// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.search

import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceModelSortKey
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object HfSearchResultsCache {
  private val cache: MutableMap<Triple<String, String, String>, Map<String, HuggingFaceEntityBasicApiData>> = mutableMapOf()

  fun getResults(query:
                 String,
                 tags:String,
                 sortKey: HuggingFaceModelSortKey): Map<String, HuggingFaceEntityBasicApiData>? {
    return cache[Triple(query, tags, sortKey.value)]
  }

  fun storeResults(query: String,
                   tags: String,
                   results: Map<String, HuggingFaceEntityBasicApiData>,
                   sortKey: HuggingFaceModelSortKey) {
    cache[Triple(query, tags, sortKey.value)] = results
  }
}
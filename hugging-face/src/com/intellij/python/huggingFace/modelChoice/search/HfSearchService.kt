// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.search

import com.intellij.python.community.impl.huggingFace.api.HuggingFaceApi
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceModelSortKey
import org.jetbrains.annotations.ApiStatus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ApiStatus.Internal
class HfSearchService {
  suspend fun performSearch(query: String,
                            tagsString: String,
                            sortKey: HuggingFaceModelSortKey
  ): Array<HuggingFaceEntityBasicApiData> {
    var result = HfSearchResultsCache.getResults(query, tagsString, sortKey)

    if (result.isNullOrEmpty()) {
      val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
      result = HuggingFaceApi.performSearch(encodedQuery, tagsString, sortKey)
      HfSearchResultsCache.storeResults(query, tagsString, result, sortKey)
    }

    return result.values.toTypedArray()
  }
}

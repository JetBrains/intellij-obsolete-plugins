package com.intellij.aiplayground.ui.utils

import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.openapi.util.NlsSafe

fun getChatName(title: String?): @NlsSafe String {
  return title ?: AIPlaygroundUIBundle.message("new.name")
}
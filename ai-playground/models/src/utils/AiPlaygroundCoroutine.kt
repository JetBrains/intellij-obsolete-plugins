package com.intellij.aiplayground.models.utils

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

@Service
class AiPlaygroundCoroutine(val coroutineScope: CoroutineScope)

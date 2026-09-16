package com.intellij.bigdatatools.notebooks.core.api.executor

class PresentableException(override val message: String, cause: Throwable) : Exception(cause)
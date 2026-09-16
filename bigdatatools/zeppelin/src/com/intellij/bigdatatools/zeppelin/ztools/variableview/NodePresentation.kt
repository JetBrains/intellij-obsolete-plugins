package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation

class XZeppelinValuePresentation(val value: String, type: String?) : XRegularValuePresentation(value, type) {
  override fun renderValue(renderer: XValueTextRenderer) = renderer.renderValue(value.take(XValueNode.MAX_VALUE_LENGTH))
}

class XZeppelinFullValueEvaluator(private val textValue: String) : XFullValueEvaluator() {
  override fun startEvaluation(callback: XFullValueEvaluationCallback) = callback.evaluated(textValue)
}
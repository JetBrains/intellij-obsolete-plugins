package com.intellij.dataWrangler.jupyterPython.serialisation

fun SerializableTransformationStep.migrate(): SerializableTransformationStep =
  when (factoryId) {
    "com.intellij.dataWrangler.jupyterPython.operations.JupyterPyFilterFactory" ->
      migrateJupyterPyFilterFactory(this)
    else ->
      this
  }

private fun migrateJupyterPyFilterFactory(step: SerializableTransformationStep): SerializableTransformationStep {
  if (step.params["value"] == null && step.params["condition"] == null && step.params["column"] == null) return step
  val newParams = step.params.toMutableMap().apply {
    qualify("condition", "value")
    qualify("condition", "condition")
    qualify("condition", "column")
  }
  return SerializableTransformationStep(step.factoryId, newParams)
}

private fun MutableMap<String, Any>.qualify(q: String, p: String) {
  remove(p)?.let { put("$q.$p", it) }
}

package com.intellij.dbt.codeInsight

import com.intellij.jinja.tags.Jinja2TagLibrary

class DbtTagLibrary : Jinja2TagLibrary() {
  override fun getCoreTags(): Set<String> {
    return dbtUnparameterizedTags
  }

  override fun getCoreParameterizedTags(): Set<String> {
    return dbtParameterizedTags
  }

  companion object {
    val dbtParameterizedTags = setOf("debug", "env_var", "fromjson", "fromyaml", "local_md5", "log", "print", "ref", "run_query", "set",
                                     "set_strict", "source", "tojson", "toyaml", "var",
                                     "zip")

    val dbtUnparameterizedTags: Set<String> = setOf(
      "adapter",
      "as_bool",
      "as_native",
      "as_number",
      "as_text",
      "builtins",
      "config",
      "dbt",
      "dbt_version",
      "dispatch",
      "exceptions",
      "execute",
      "flags",
      "graph",
      "invocation_id",
      "model",
      "modules",
      "project_name",
      "return",
      "run_started_at",
      "schema",
      "schemas",
      "selected_resources",
      "target",
      "this"
    )
  }
}
package com.intellij.dbt.codeInsight

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtUtils.Companion.DBT_PROJECT_FILE_NAME
import com.intellij.dbt.DbtUtils.Companion.DBT_SCHEMA_YML_FILE_NAME
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class DbtYmlSchemaProviderFactory : JsonSchemaProviderFactory, DumbAware {
  override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
    return mutableListOf(DbtProjectYmlSchemaProvider, DbtModelYmlSchemaProvider)
  }
}

object DbtProjectYmlSchemaProvider : JsonSchemaFileProvider {
  override fun isAvailable(file: VirtualFile): Boolean {
    return file.name == DBT_PROJECT_FILE_NAME
  }

  override fun getName() = DBT_PROJECT_FILE_NAME

  override fun getSchemaFile(): VirtualFile? {
    return JsonSchemaProviderFactory.getResourceFile(this::class.java, "/schemas/dbt_project_yml_schema.json")
  }

  override fun getSchemaType() = SchemaType.schema
}

object DbtModelYmlSchemaProvider: JsonSchemaFileProvider {
  override fun isAvailable(file: VirtualFile): Boolean {
    return file.name == DBT_SCHEMA_YML_FILE_NAME
  }

  override fun getName() = DbtBundle.message("dbt.schema.model.schema")

  override fun getSchemaFile(): VirtualFile? {
    return JsonSchemaProviderFactory.getResourceFile(this::class.java, "/schemas/dbt_model_yml_schema.json")
  }

  override fun getSchemaType() = SchemaType.schema
}
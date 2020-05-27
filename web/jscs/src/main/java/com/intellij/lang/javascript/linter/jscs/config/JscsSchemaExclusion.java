package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.remote.JsonSchemaCatalogExclusion;
import org.jetbrains.annotations.NotNull;

public class JscsSchemaExclusion implements JsonSchemaCatalogExclusion {
  @Override
  public boolean isExcluded(@NotNull VirtualFile file) {
    return FileTypeRegistry.getInstance().isFileOfType(file, JscsConfigFileType.INSTANCE);
  }
}

package com.intellij.lang.puppet.ide.navigation.plugins.facts;

import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonValue;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class JsonFactIndexer implements PuppetFactDataIndexer {
  @Override
  public @NotNull Map<String, Integer> map(@NotNull FileContent inputData) {
    final PsiFile file = inputData.getPsiFile();
    if (!(file instanceof JsonFile)) {
      return Collections.emptyMap();
    }

    Map<String, Integer> result = new HashMap<>();

    final JsonValue topLevelValue = ((JsonFile)file).getTopLevelValue();

    if (!(topLevelValue instanceof JsonObject)) {
      return result;
    }
    for (JsonProperty property : ((JsonObject)topLevelValue).getPropertyList()) {
      final String name = property.getName();
      result.put(name, property.getTextOffset());
    }

    return result;
  }

  @Override
  public boolean acceptsFile(@NotNull String fileName, @NotNull String parentDirName) {
    return parentDirName.equals(EXTERNAL_FACTS_DIR) && StringUtil.endsWithIgnoreCase(fileName, ".json");
  }

  @Override
  public @NotNull FileType getSuitableFileType() {
    return JsonFileType.INSTANCE;
  }
}

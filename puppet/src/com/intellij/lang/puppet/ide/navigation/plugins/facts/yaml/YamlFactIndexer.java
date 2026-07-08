package com.intellij.lang.puppet.ide.navigation.plugins.facts.yaml;

import com.intellij.lang.puppet.ide.navigation.plugins.facts.PuppetFactDataIndexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class YamlFactIndexer implements PuppetFactDataIndexer {
  @Override
  public @NotNull Map<String, Integer> map(@NotNull FileContent inputData) {
    final PsiFile file = inputData.getPsiFile();
    if (!(file instanceof YAMLFile)) {
      return Collections.emptyMap();
    }

    Map<String, Integer> result = new HashMap<>();

    final List<YAMLDocument> yamlDocuments = ((YAMLFile)file).getDocuments();
    for (YAMLDocument document : yamlDocuments) {
      final YAMLValue topValue = document.getTopLevelValue();
      if (!(topValue instanceof YAMLMapping)) {
        continue;
      }

      for (YAMLKeyValue keyValue : ((YAMLMapping)topValue).getKeyValues()) {
        String name = keyValue.getKeyText();
        result.put(name, keyValue.getTextOffset());
      }
    }

    return result;
  }

  @Override
  public boolean acceptsFile(@NotNull String fileName, @NotNull String parentDirName) {
    return parentDirName.equals(EXTERNAL_FACTS_DIR) && (StringUtil.endsWithIgnoreCase(fileName, ".yaml") ||
                                                        StringUtil.endsWithIgnoreCase(fileName, ".yml"));
  }

  @Override
  public @NotNull FileType getSuitableFileType() {
    return YAMLFileType.YML;
  }
}

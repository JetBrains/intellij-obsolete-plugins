// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.completion;

import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xhtml.XHTMLLanguage;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlFileNSInfoProvider;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;

public final class GspXmlFileNSInfoProvider implements XmlFileNSInfoProvider, Function<String, String[]> {

  private static final String[][] HTML_PREFIXES = new String[][]{new String[]{"", XmlUtil.XHTML_URI}};

  @Override
  public String[] @Nullable [] getDefaultNamespaces(@NotNull XmlFile file) {
    if (file instanceof GspFile) {
      return ContainerUtil.map2Array(GspTagLibUtil.getTagLibClasses(file).keySet(), String[].class, this);
    }
    else {
      FileViewProvider viewProvider = file.getViewProvider();
      if (viewProvider instanceof GspFileViewProvider && viewProvider.getFileType() == GspFileType.GSP_FILE_TYPE) {
        final Language baseLanguage = ((GspFileViewProvider)viewProvider).getTemplateDataLanguage();
        if (baseLanguage == HTMLLanguage.INSTANCE || baseLanguage == XHTMLLanguage.INSTANCE) {
          return HTML_PREFIXES;
        }
      }
    }

    return null;
  }

  @Override
  public boolean overrideNamespaceFromDocType(@NotNull XmlFile file) {
    return false;
  }

  @Override
  public String[] fun(String s) {
    return new String[]{s, s};
  }
}

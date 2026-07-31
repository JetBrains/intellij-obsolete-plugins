package com.intellij.gwt.run.remoteUi;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import static com.intellij.openapi.util.text.StringUtil.trimEnd;

public class GwtErrorFilterTest extends GwtCodeInsightTestCase {

  public void testErrorFilter() {
    PsiFile[] psiFiles = myCodeInsightFixture.configureByFiles("client/MyComponent.ui.xml", "client/MyComponent.java");
    VirtualFile uiXmlFile = psiFiles[0].getVirtualFile();
    VirtualFile javaFile = psiFiles[1].getVirtualFile();


    String[] lines = {
      "   [ERROR] Errors in 'client/MyComponent.java'\n",
      "      [ERROR] Line 2: error 1\n",
      "   [ERROR] Line 10: should not be highlighted\n",
      "   Computing all possible rebind results for 'client.MyComponent'\n",
      "         Invoking generator com.google.gwt.uibinder.rebind.UiBinderGenerator\n",
      "            [ERROR] error 2 (:1)\n",
      "   [ERROR] Errors in '" + javaFile.getUrl() + "'\n",
      "      [ERROR] Line 5: error 3\n"
    };

    VirtualFile[] expectedFiles = {
      null,
      javaFile,
      null,
      null,
      null,
      uiXmlFile,
      null,
      javaFile
    };

    String[] expectedHighlights = {
      null,
      "Line 2: error 1",
      null,
      null,
      null,
      "error 2 (:1)",
      null,
      "Line 5: error 3"
    };

    StringBuilder entireText = new StringBuilder();
    GwtErrorFilter filter = new GwtErrorFilter(myCodeInsightFixture.getModule());
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      entireText.append(line);
      Filter.Result result = filter.applyFilter(line, entireText.length());
      if (expectedFiles[i] != null) {
        assertNotNull("Expected file link at line " + i + ". Entire text:\n" + trimEnd(entireText.toString(), "\n"), result);

        OpenFileHyperlinkInfo hyperlinkInfo = assertInstanceOf(result.getFirstHyperlinkInfo(), OpenFileHyperlinkInfo.class);
        assertEquals(expectedFiles[i], hyperlinkInfo.getDescriptor().getFile());

        Filter.ResultItem item = assertOneElement(result.getResultItems());
        assertEquals(expectedHighlights[i], entireText.substring(item.getHighlightStartOffset(), item.getHighlightEndOffset()));
      }
      else {
        assertNull(result);
      }
    }
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "remoteUi/errorFilter";
  }
}

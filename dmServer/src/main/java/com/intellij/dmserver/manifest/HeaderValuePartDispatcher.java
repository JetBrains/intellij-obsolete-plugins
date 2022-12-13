package com.intellij.dmserver.manifest;

import com.intellij.psi.PsiElement;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.osmorc.manifest.lang.psi.Clause;

import java.util.HashMap;
import java.util.Map;

public class HeaderValuePartDispatcher<C, T extends HeaderValuePartProcessor<C>> {

  private final Map<String, T> myHeaderName2Processor;

  public HeaderValuePartDispatcher(T... processors) {
    myHeaderName2Processor = new HashMap<>();
    for (T processor : processors) {
      myHeaderName2Processor.put(processor.getHeaderName(), processor);
    }
  }

  public void process(PsiElement element, C context) {
    if (!(element instanceof HeaderValuePart)) {
      return;
    }
    if (!(element.getContainingFile() instanceof ManifestFile)) {
      return;
    }
    HeaderValuePart headerValue = (HeaderValuePart)element;
    PsiElement headerValueParent = headerValue.getParent();
    if (!(headerValueParent instanceof Clause)) {
      return;
    }
    Clause clause = (Clause)headerValueParent;
    PsiElement clauseParent = clause.getParent();
    if (!(clauseParent instanceof Header)) {
      return;
    }
    Header header = (Header)clauseParent;
    String headerName = header.getName();
    if (!myHeaderName2Processor.containsKey(headerName)) {
      return;
    }
    myHeaderName2Processor.get(headerName).process(headerValue, context);
  }
}

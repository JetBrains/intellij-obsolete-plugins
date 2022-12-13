package com.intellij.dmserver.editor.wrapper;

import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.osmorc.manifest.lang.psi.Clause;
import org.jetbrains.lang.manifest.psi.Header;

import java.util.ArrayList;
import java.util.List;

public class HeaderWrapper {

  private final String myName;

  private final List<ClauseWrapper> myClauses;

  public HeaderWrapper(PsiFile manifestFile, String headerName) {
    myName = headerName;
    myClauses = new ArrayList<>();
    List<Header> headers = ManifestUtils.getInstance().findHeaders(manifestFile, headerName);
    for (Header header : headers) {
      for (Clause clause = PsiTreeUtil.getChildOfType(header, Clause.class);
           clause != null;
           clause = PsiTreeUtil.getNextSiblingOfType(clause, Clause.class)) {
        ClauseWrapper clauseWrapper = ClauseWrapper.create(clause);
        if (clauseWrapper != null) {
          myClauses.add(clauseWrapper);
        }
      }
    }
  }

  public String getName() {
    return myName;
  }

  public List<ClauseWrapper> getClauses() {
    return myClauses;
  }
}

package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala;

import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage;
import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.scala.ScalaLanguage;
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile;

public class ZeppelinScalaImportOptimizer extends ScalaImportOptimizer {
  @Override
  public Runnable processFile(PsiFile file, ProgressIndicator progressIndicator) {
    PsiFile scalaPsi = file.getViewProvider().getPsi(ScalaLanguage.INSTANCE);
    return super.processFile(scalaPsi != null? scalaPsi : file, progressIndicator);
  }

  @Override
  public boolean supports(PsiFile file) {
    return file.getViewProvider().getPsi(ZeppelinLanguage.INSTANCE) != null && (file instanceof ScalaFile || file instanceof ZeppelinPsiFile);
  }
}

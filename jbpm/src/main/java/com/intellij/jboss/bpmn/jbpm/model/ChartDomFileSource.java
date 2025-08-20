package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomEventListener;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.events.DomEvent;
import org.jetbrains.annotations.NotNull;

public abstract class ChartDomFileSource extends ChartFileSource implements Disposable {
  @NotNull public final Project project;
  @NotNull public final PsiFile psiFile;

  public ChartDomFileSource(@NotNull Project project, @NotNull VirtualFile file) {
    super(file);
    this.project = project;
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    assert psiFile != null;
    this.psiFile = psiFile;
  }

  @Override
  @NotNull
  public PsiFile getPsiFile() {
    return psiFile;
  }

  @Override
  public void addChangeListener(ChartSourceChangeListener listener) {
    DomManager.getDomManager(project).addDomEventListener(new DomEventListener() {
      @Override
      public void eventOccured(@NotNull final DomEvent event) {
        DomElement element = event.getElement();
        if (element == null) {
          return;
        }
        while (element.getParent() != null) {
          element = element.getParent();
        }
        if (element instanceof DomFileElement && ((DomFileElement<?>)element).getFile() != psiFile) {
          return;
        }
        listener.chartSourceHasChanged();
      }
    }, this);
  }
}

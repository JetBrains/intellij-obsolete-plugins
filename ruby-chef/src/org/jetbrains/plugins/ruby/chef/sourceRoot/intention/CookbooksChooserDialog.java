package org.jetbrains.plugins.ruby.chef.sourceRoot.intention;

import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.chef.ChefBundle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

public class CookbooksChooserDialog extends DialogWrapper {
  private final ElementsChooser<PsiDirectory> mySourcePathsChooser;

  public CookbooksChooserDialog(@NotNull Project project, @NotNull List<PsiDirectory> cookbookCandidates) {
    super(project, false);
    setTitle(ChefBundle.message("cookbooks.source.root.inspection.name"));

    mySourcePathsChooser = new ElementsChooser<>(true) {
      @Override
      public @NlsSafe String getItemText(@NotNull PsiDirectory cookbook) {
        return cookbook.getVirtualFile().getCanonicalPath();
      }
    };
    mySourcePathsChooser.setElements(cookbookCandidates, true);

    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setPreferredSize(new Dimension(450, 200));
    final JBLabel label = new JBLabel(ChefBundle.message("cookbooks.source.root.choose.cookbooks"));
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    panel.add(label, BorderLayout.NORTH);
    panel.add(mySourcePathsChooser, BorderLayout.CENTER);
    return panel;
  }

  public List<PsiDirectory> getMarkedElements() {
    return mySourcePathsChooser.getMarkedElements();
  }
}
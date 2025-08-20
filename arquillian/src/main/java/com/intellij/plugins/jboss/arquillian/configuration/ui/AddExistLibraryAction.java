package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianExistLibraryModel;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianLibraryModel;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianExistLibraryState;
import com.intellij.util.Function;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.classpath.ChooseLibrariesFromTablesDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AddExistLibraryAction extends AddLibraryAction {
  private final @NotNull Project project;

  public AddExistLibraryAction(@NotNull Project project) {
    super(PlatformIcons.LIBRARY_ICON, ArquillianBundle.message("arquillian.action.add.exist.library"));
    this.project = project;
  }

  @Override
  Collection<ArquillianLibraryModel> execute() {
    AddLibraryDialog dialog = new AddLibraryDialog(
      ArquillianBundle.message("add.exist.library.dialog.title"),
      project,
      true) {
    };
    dialog.showAndGet();
    return JBIterable.from(dialog.getSelectedLibraries())
      .filter(library -> library.getName() != null)
      .transform((Function<Library, ArquillianLibraryModel>)library -> {
        //noinspection ConstantConditions
        return new ArquillianExistLibraryModel(project,
                                               new ArquillianExistLibraryState(library.getName(), library.getTable().getTableLevel()));
      }).toList();
  }

  private static class AddLibraryDialog extends ChooseLibrariesFromTablesDialog {
    protected AddLibraryDialog(@NotNull @NlsContexts.DialogTitle String title,
                               @NotNull Project project, boolean showCustomLibraryTables) {
      super(title, project, showCustomLibraryTables);
      init();
    }
  }
}

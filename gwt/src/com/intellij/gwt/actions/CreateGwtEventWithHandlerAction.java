package com.intellij.gwt.actions;

import com.intellij.CommonBundle;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class CreateGwtEventWithHandlerAction extends GwtCreateActionBase {
  private final Ref<String> myHandlerClassName = Ref.create(null);
  private final Ref<String> myHandleMethodName = Ref.create(null);

  @Override
  protected void doCheckBeforeCreate(String newName, PsiDirectory directory) throws IncorrectOperationException {
    super.doCheckBeforeCreate(newName, directory);
    JavaDirectoryService.getInstance().checkCreateClass(directory, myHandlerClassName.get());
    PsiUtil.checkIsIdentifier(directory.getManager(), myHandleMethodName.get());
  }

  @Override
  protected void showDialog(@NotNull GwtFacet facet,
                            @NotNull PsiDirectory directory,
                            @NotNull MyInputValidator validator) {
    final Project project = facet.getModule().getProject();
    if (!facet.getSdkVersion().isEventHandlersSupported()) {
      Messages.showErrorDialog(project, GwtBundle.message("error.message.gwt.event.not.supported"),
                               CommonBundle.getErrorTitle());
      return;
    }
    final CreateGwtEventWithHandlerDialog dialog = new CreateGwtEventWithHandlerDialog(project, getDialogTitle(), validator, myHandlerClassName, myHandleMethodName);
    dialog.show();
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String eventClassName, PsiDirectory directory, GwtModule gwtModule) {
    String handlerClassName = myHandlerClassName.get();
    String handleMethodName = myHandleMethodName.get();
    PsiClass eventClass = createClassFromTemplate(directory, eventClassName, JavaFileType.INSTANCE, GwtTemplates.EVENT_JAVA,
                                                  "HANDLER_NAME", handlerClassName,
                                                  "METHOD_NAME", handleMethodName);
    PsiClass handlerClass = createClassFromTemplate(directory, handlerClassName, JavaFileType.INSTANCE, GwtTemplates.EVENT_HANDLER_JAVA,
                                                    "EVENT_NAME", eventClassName,
                                                    "METHOD_NAME", handleMethodName);

    return new PsiElement[] {eventClass, handlerClass};
  }

  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("label.text.event.class.name");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("dialog.title.create.gwt.event.with.handler");
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("action.name.create.gwt.event.with.handler", newName);
  }
}

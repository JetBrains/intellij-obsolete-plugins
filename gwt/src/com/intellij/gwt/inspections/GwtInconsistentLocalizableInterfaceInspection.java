/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.i18n.PropertiesFilesListCellRenderer;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBList;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GwtInconsistentLocalizableInterfaceInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(file);
    if (gwtFacet == null) {
      return null;
    }

    if (file instanceof PropertiesFile propertiesFile) {
      return checkPropertiesFile(manager, propertiesFile, gwtFacet, isOnTheFly);
    }

    if (file instanceof PsiJavaFile) {
      final PsiClass[] psiClasses = ((PsiJavaFile)file).getClasses();
      for (PsiClass psiClass : psiClasses) {
        final ProblemDescriptor[] descriptors = checkPsiClass(manager, psiClass, isOnTheFly);
        if (descriptors != null) {
          return descriptors;
        }
      }
    }
    return null;
  }

  private static ProblemDescriptor @Nullable [] checkPsiClass(final InspectionManager manager, final PsiClass psiClass, boolean onTheFly) {
    final GwtI18nManager i18nManager = GwtI18nManager.getInstance(manager.getProject());
    final PropertiesFile[] files = i18nManager.getPropertiesFiles(psiClass);
    if (files.length == 0) {
      return null;
    }

    List<ProblemDescriptor> descriptors = new ArrayList<>();
    for (PsiMethod psiMethod : psiClass.getMethods()) {
      if (hasDefaultValue(psiMethod)) {
        continue;
      }

      final IProperty[] properties = i18nManager.getProperties(psiMethod);
      if (properties.length == 0) {
        final String description =
          GwtBundle.message("problem.description.method.0.does.not.have.corresponding.property", psiMethod.getName());
        final LocalQuickFix quickFix = onTheFly ? new DefinePropertyQuickfix(psiMethod, GwtI18nUtil.getPropertyName(psiMethod), files) : null;
        descriptors.add(manager.createProblemDescriptor(getElementToHighlight(psiMethod), description, quickFix,
                                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                        onTheFly));
      }
    }
    return descriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static boolean hasDefaultValue(@NotNull PsiMethod psiMethod) {
    for (PsiAnnotation annotation : psiMethod.getModifierList().getAnnotations()) {
      final String name = annotation.getQualifiedName();
      if (name != null
          && (name.startsWith(GwtI18nUtil.CONSTANTS_INTERFACE_NAME + ".Default")
              || name.equals(GwtI18nUtil.MESSAGES_INTERFACE_NAME + ".DefaultMessage"))) {
        return true;
      }
    }
    return false;
  }

  private static ProblemDescriptor @Nullable [] checkPropertiesFile(final InspectionManager manager, final PropertiesFile propertiesFile,
                                                                    final GwtFacet gwtFacet, boolean onTheFly) {
    final GwtI18nManager i18nManager = GwtI18nManager.getInstance(manager.getProject());
    final PsiClass anInterface = i18nManager.getPropertiesInterface(propertiesFile);
    if (anInterface == null) {
        return null;
      }

    List<IProperty> propertiesWithoutMethods = new ArrayList<>();
    final List<IProperty> properties = propertiesFile.getProperties();
    for (IProperty property : properties) {
        final PsiMethod method = i18nManager.getMethod(property);
        if (method == null && property.getUnescapedKey() != null) {
          propertiesWithoutMethods.add(property);
        }
      }

    if (propertiesWithoutMethods.isEmpty()) {
        return null;
      }

    SynchronizeInterfaceQuickFix syncAllQuickfix;
    if (propertiesWithoutMethods.size() > 1) {
        syncAllQuickfix = new SynchronizeInterfaceQuickFix(anInterface, propertiesWithoutMethods, gwtFacet.getSdkVersion());
      }
      else {
        syncAllQuickfix = null;
      }

    List<ProblemDescriptor> problems = new ArrayList<>();
    for (IProperty property : propertiesWithoutMethods) {
        final String key = property.getUnescapedKey();
        final AddMethodToInterfaceQuickFix quickFix = new AddMethodToInterfaceQuickFix(anInterface, key, property.getValue(), gwtFacet.getSdkVersion());
        LocalQuickFix[] fixes = syncAllQuickfix == null ? new LocalQuickFix[]{quickFix} : new LocalQuickFix[]{syncAllQuickfix, quickFix};

      final String description = GwtBundle.message("problem.description.property.0.does.not.have.corresponding.method.in.1", key, anInterface.getName());
      problems.add(manager.createProblemDescriptor(property.getPsiElement(), description, onTheFly, fixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static void defineProperty(final PropertiesFile propertiesFile, final String propertyName) {
    Computable<IProperty> computeProperty = () -> definePropertyImpl(propertiesFile, propertyName);
    IProperty property = WriteCommandAction.runWriteCommandAction(propertiesFile.getProject(), computeProperty);
    if (property != null) {
      GwtI18nUtil.navigateToProperty(property);
    }
  }

  private static @Nullable IProperty definePropertyImpl(final PropertiesFile propertiesFile, final String propertyName) {
    try {
      return propertiesFile.addProperty(propertyName, "");
    }
    catch (IncorrectOperationException e) {
      Logger.getInstance(GwtInconsistentLocalizableInterfaceInspection.class).error(e);
      return null;
    }
  }

  private static boolean ensureWritable(final PsiElement element) {
    return !ReadonlyStatusHandler.getInstance(element.getProject())
      .ensureFilesWritable(Collections.singletonList(element.getContainingFile().getVirtualFile())).hasReadonlyFiles();
  }

  private static class AddMethodToInterfaceQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final String myPropertyName;
    private final String myPropertyValue;
    private final GwtVersion myGwtVersion;

    AddMethodToInterfaceQuickFix(final PsiClass anInterface, final String propertyName, final String value,
                                        final GwtVersion gwtVersion) {
      super(GwtBundle.message("quickfix.family.name.create.missing.methods"), GwtBundle.message("quickfix.name.create.method.for.property.0.in.1", propertyName, anInterface.getName()), anInterface);
      myPropertyName = propertyName;
      myPropertyValue = value;
      myGwtVersion = gwtVersion;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass anInterface)) return;

      if (!ensureWritable(anInterface)) {
        return;
      }
      GwtI18nUtil.addMethod(anInterface, myPropertyName, myPropertyValue, myGwtVersion);
    }
  }

  private static class SynchronizeInterfaceQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final GwtVersion myGwtVersion;
    private final List<IProperty> myProperties;

    SynchronizeInterfaceQuickFix(final PsiClass anInterface, final List<IProperty> properties, final GwtVersion gwtVersion) {
      super(GwtBundle.message("quickfix.family.name.create.missing.methods"), GwtBundle.message("quickfix.name.synchronize.all.methods.in.0", anInterface.getName()), anInterface);
      myProperties = properties;
      myGwtVersion = gwtVersion;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass anInterface)) return;

      if (!ensureWritable(anInterface)) {
        return;
      }

      for (IProperty property : myProperties) {
        GwtI18nUtil.addMethod(anInterface, property.getUnescapedKey(), property.getValue(), myGwtVersion);
      }
    }
  }

  private static class DefinePropertyQuickfix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final String myPropertyName;
    private final PropertiesFile[] myPropertiesFiles;

    DefinePropertyQuickfix(PsiMethod psiMethod, final String propertyName, final PropertiesFile[] propertiesFiles) {
      super(psiMethod);
      myPropertyName = propertyName;
      myPropertiesFiles = propertiesFiles;
    }

    @Override
    public @NotNull String getText() {
      return GwtBundle.message("quickfix.name.create.property.0", myPropertyName);
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return GwtBundle.message("quickfix.family.name.create.property");
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
      if (myPropertiesFiles.length == 1) {
        final PropertiesFile propertiesFile = myPropertiesFiles[0];
        if (ensureWritable(propertiesFile.getContainingFile())) {
          final IProperty property = definePropertyImpl(propertiesFile, myPropertyName);
          if (property != null) {
            GwtI18nUtil.navigateToProperty(property);
          }
        }
        return;
      }

      if (editor != null) {
        final JList list = new JBList(myPropertiesFiles);
        final PropertiesFilesListCellRenderer renderer = new PropertiesFilesListCellRenderer();
        list.setCellRenderer(renderer);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final PopupChooserBuilder builder = JBPopupFactory.getInstance().createListPopupBuilder(list);
        renderer.installSpeedSearch(builder);
        builder
          .setTitle(GwtBundle.message("quickfix.popup.title.choose.properties.file"))
          .setItemChosenCallback(() -> {
            final int index = list.getSelectedIndex();
            if (index != -1) {
              defineProperty(myPropertiesFiles[index], myPropertyName);
            }
          })
          .createPopup()
          .showInBestPositionFor(editor);
      }
    }
  }
}

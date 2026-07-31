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

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GwtInconsistentAsyncInterfaceInspection extends BaseGwtInspection {
  private static final Logger LOG = Logger.getInstance(GwtInconsistentAsyncInterfaceInspection.class);

  @Override
  public ProblemDescriptor @Nullable [] checkClass(final @NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null) return null;

    GwtVersion gwtVersion = gwtFacet.getSdkVersion();
    if (RemoteServiceUtil.isRemoteServiceInterface(aClass)) {
      return checkRemoteServiceForAsync(aClass, gwtVersion, manager, isOnTheFly);
    }

    final PsiClass sync = RemoteServiceUtil.findSynchronousInterface(aClass);
    if (sync != null) {
      return checkAsyncServiceForRemote(sync, aClass, gwtVersion, manager, isOnTheFly);
    }

    return null;
  }

  private static ProblemDescriptor @Nullable [] checkAsyncServiceForRemote(PsiClass sync, PsiClass async, final GwtVersion gwtVersion,
                                                                           InspectionManager manager, boolean onTheFly) {
    List<ProblemDescriptor> problems = new SmartList<>();
    List<PsiMethod> methodsToCopy = new SmartList<>();
    for (PsiMethod method : sync.getMethods()) {
      if (!RemoteServiceUtil.isMethodPresentedInAsync(method, async)) {
        methodsToCopy.add(method);
      }
    }

    for (PsiMethod asyncMethod : async.getMethods()) {
      if (!RemoteServiceUtil.isMethodPresentedInSync(asyncMethod, sync)) {
        final String message = GwtBundle.message("problem.description.async.method.does.not.have.sync.variant", asyncMethod.getName());
        final LocalQuickFix quickFix = new CopyMethodToSyncQuickFix(sync, asyncMethod);
        problems.add(manager.createProblemDescriptor(getElementToHighlight(asyncMethod), message, quickFix,
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING, onTheFly));
      }

      final PsiType returnType = asyncMethod.getReturnType();
      if (returnType == null
          || !returnType.equals(PsiTypes.voidType())
             && !returnType.getCanonicalText().equals("com.google.gwt.http.client.Request")
             && !returnType.getCanonicalText().equals("com.google.gwt.http.client.RequestBuilder")) {
        final String message = GwtBundle.message("problem.description.the.asynchronous.version.of.method.0.must.have.a.return.type.void",
                                                 asyncMethod.getName());
        LocalQuickFix fix = new MakeMethodReturnVoid(asyncMethod);
        problems.add(manager.createProblemDescriptor(getElementToHighlight(asyncMethod), message,
                                                     fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, onTheFly));
      }
    }

    if (!methodsToCopy.isEmpty()) {
      String message = GwtBundle.message("problem.description.methods.of.async.remote.service.0.isn.t.synchronized.with.1",
                                         async.getName(), sync.getName());
      List<LocalQuickFix> quickFixesList = new ArrayList<>();
      for (PsiMethod method : methodsToCopy) {
        quickFixesList.add(new CopyMethodToAsyncQuickFix(async, method, gwtVersion));
      }
      quickFixesList.add(new SynchronizeAllMethodsInAsyncQuickFix(async, sync, gwtVersion));
      LocalQuickFix[] fixes = quickFixesList.toArray(LocalQuickFix.EMPTY_ARRAY);
      problems.add(manager.createProblemDescriptor(getElementToHighlight(async), message, onTheFly, fixes,
                                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }


    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static ProblemDescriptor[] checkRemoteServiceForAsync(PsiClass aClass, final GwtVersion gwtVersion, InspectionManager manager,
                                                                boolean onTheFly) {
    GlobalSearchScope scope = aClass.getResolveScope();


    final PsiClass async = JavaPsiFacade.getInstance(manager.getProject()).findClass(aClass.getQualifiedName() + RemoteServiceUtil.ASYNC_SUFFIX, scope);
    if (async == null) {
      final String description = GwtBundle.message("problem.description.remote.service.0.does.not.have.corresponding.async.variant", aClass.getName());
      return new ProblemDescriptor[] {
        manager.createProblemDescriptor(getElementToHighlight(aClass), description, new CreateAsyncClassQuickFix(aClass, gwtVersion), ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                        onTheFly)
      };
    }

    ArrayList<ProblemDescriptor> result = new ArrayList<>(0);

    for (final PsiMethod method : aClass.getMethods()) {
      if (!RemoteServiceUtil.isMethodPresentedInAsync(method, async)) {
        LocalQuickFix fix = new CopyMethodToAsyncQuickFix(async, method, gwtVersion);
        String message = GwtBundle.message("problem.description.async.remote.service.0.does.not.define.corresponding.method", async.getName());
        result.add(manager.createProblemDescriptor(getElementToHighlight(method), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                   onTheFly));
      }
    }

    return result.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static final class CopyMethodToSyncQuickFix extends BaseGwtLocalQuickFix {
    private final SmartPsiElementPointer<PsiClass> mySync;
    private final SmartPsiElementPointer<PsiMethod> myMethod;

    private CopyMethodToSyncQuickFix(final PsiClass sync, final PsiMethod method) {
      super(GwtBundle.message("quickfix.name.create.sync.method.for.async.0",
                              PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME |
                                                                                       PsiFormatUtilBase.SHOW_PARAMETERS,
                                                         PsiFormatUtilBase.SHOW_TYPE)));
      mySync = SmartPointerManager.createPointer(sync);
      myMethod = SmartPointerManager.createPointer(method);
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return GwtBundle.message("quickfix.family.name.create.sync.variant");
    }

    @Override
    public void applyFix(final @NotNull Project project, final @NotNull ProblemDescriptor descriptor) {
      PsiClass sync = mySync.getElement();
      PsiMethod psiMethod = myMethod.getElement();
      if (sync == null || psiMethod == null) return;

      VirtualFile file = sync.getContainingFile().getVirtualFile();
      if (file == null || ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Collections.singletonList(file)).hasReadonlyFiles()) {
        return;
      }
      try {
        PsiMethod method = RemoteServiceUtil.copyMethodToSync(psiMethod, sync);
        PsiElement reformatted = CodeStyleManager.getInstance(project).reformat(method);
        OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, file);
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(fileDescriptor, true);
        if (editor != null) {
          editor.getCaretModel().moveToOffset(reformatted.getTextRange().getStartOffset());
          if (reformatted instanceof PsiMethod) {
            PsiTypeElement returnTypeElement = ((PsiMethod)reformatted).getReturnTypeElement();
            if (returnTypeElement != null) {
              TextRange typeRange = returnTypeElement.getTextRange();
              editor.getSelectionModel().setSelection(typeRange.getStartOffset(), typeRange.getEndOffset());
            }
          }
        }
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }

  private static class MakeMethodReturnVoid extends BaseGwtLocalQuickFixOnPsiElement {
    MakeMethodReturnVoid(PsiMethod method) {
      super(GwtBundle.message("quickfix.family.name.make.method.return.void"), GwtBundle.message("quickfix.name.make.0.return.void", method.getName()), method);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiMethod method)) return;

      final PsiTypeElement returnTypeElement = method.getReturnTypeElement();
      if (returnTypeElement != null) {
        returnTypeElement.replace(JavaPsiFacade.getElementFactory(project).createTypeElement(PsiTypes.voidType()));
      }
    }
  }

  private static final class CopyMethodToAsyncQuickFix extends BaseGwtLocalQuickFix {
    private final SmartPsiElementPointer<PsiClass> myAsync;
    private final SmartPsiElementPointer<PsiMethod> myMethod;
    private final GwtVersion myGwtVersion;

    private CopyMethodToAsyncQuickFix(final PsiClass async, final PsiMethod method, final GwtVersion gwtVersion) {
      super(GwtBundle.message("quickfix.name.create.async.method.for.sync.0",
                              PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME |
                                                                                       PsiFormatUtilBase.SHOW_PARAMETERS,
                                                         PsiFormatUtilBase.SHOW_TYPE)));
      myAsync = SmartPointerManager.createPointer(async);
      myMethod = SmartPointerManager.createPointer(method);
      myGwtVersion = gwtVersion;
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return GwtBundle.message("quickfix.family.name.create.async.variant");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiClass async = myAsync.getElement();
      PsiMethod method = myMethod.getElement();
      if (async == null || method == null) return;

      if (!FileModificationService.getInstance().preparePsiElementForWrite(async.getContainingFile())) return;

      try {
        PsiMethod newMethod = RemoteServiceUtil.copyMethodToAsync(method, async, myGwtVersion);
        CodeStyleManager.getInstance(project).reformat(newMethod);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }


  private static class SynchronizeAllMethodsInAsyncQuickFix extends BaseGwtLocalQuickFix {
    private final SmartPsiElementPointer<PsiClass> myAsync;
    private final SmartPsiElementPointer<PsiClass> myRemoteServiceInterface;
    private final GwtVersion myGwtVersion;


    SynchronizeAllMethodsInAsyncQuickFix(final PsiClass async, final PsiClass remoteServiceInterface, final GwtVersion gwtVersion) {
      super(GwtBundle.message("quick.fix.name.synchronize.all.methods.of.0.with.1", async.getName(), remoteServiceInterface.getName()));
      myAsync = SmartPointerManager.createPointer(async);
      myRemoteServiceInterface = SmartPointerManager.createPointer(remoteServiceInterface);
      myGwtVersion = gwtVersion;
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return GwtBundle.message("quickfix.family.name.create.missing.methods");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiClass async = myAsync.getElement();
      PsiClass remoteServiceInterface = myRemoteServiceInterface.getElement();
      if (async == null || remoteServiceInterface == null) return;

      if (!FileModificationService.getInstance().preparePsiElementForWrite(async.getContainingFile())) return;

      try {
        RemoteServiceUtil.copyAllMethodsToAsync(remoteServiceInterface, async, myGwtVersion);
        CodeStyleManager.getInstance(project).reformat(async);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }

  private static class CreateAsyncClassQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final GwtVersion myGwtVersion;

    CreateAsyncClassQuickFix(final PsiClass remoteServiceInterface, final GwtVersion gwtVersion) {
      super(GwtBundle.message("quickfix.family.name.create.interface"), GwtBundle.message("quickfix.name.create.interface.0", remoteServiceInterface.getName() + RemoteServiceUtil.ASYNC_SUFFIX),
            remoteServiceInterface);
      myGwtVersion = gwtVersion;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass remoteServiceInterface)) return;

      try {
        PsiJavaFile classFile = (PsiJavaFile)remoteServiceInterface.getContainingFile();
        LOG.assertTrue(classFile != null);
        final String name = remoteServiceInterface.getName() + RemoteServiceUtil.ASYNC_SUFFIX;
        final PsiTypeParameterList typeParameterList = remoteServiceInterface.getTypeParameterList();
        final PsiPackageStatement packageStatement = classFile.getPackageStatement();
        @NonNls StringBuilder source = new StringBuilder();
        source.append(packageStatement != null ? packageStatement.getText() : "").append("\n\n");
        final PsiImportList psiImportList = classFile.getImportList();
        source.append(psiImportList != null ? psiImportList.getText() : "");
        source.append("public interface ").append(name);
        if (typeParameterList != null) {
          source.append(typeParameterList.getText());
        }
        source.append("\n{\n}\n");
        PsiJavaFile asyncFile = (PsiJavaFile)PsiFileFactory.getInstance(project).createFileFromText(name + ".java", source.toString());

        PsiClass async = asyncFile.getClasses()[0];
        RemoteServiceUtil.copyAllMethodsToAsync(remoteServiceInterface, async, myGwtVersion);

        CodeStyleManager.getInstance(project).reformat(asyncFile);

        final PsiDirectory directory = classFile.getContainingDirectory();
        LOG.assertTrue(directory != null);
        directory.add(asyncFile);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }
}

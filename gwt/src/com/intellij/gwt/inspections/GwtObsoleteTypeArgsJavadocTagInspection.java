package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.rpc.GwtGenericsUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.util.IncorrectOperationException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GwtObsoleteTypeArgsJavadocTagInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkClass(final @NotNull PsiClass aClass, final @NotNull InspectionManager manager,
                                        final boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null || !gwtFacet.getSdkVersion().isGenericsSupported()) {
      return null;
    }

    if (RemoteServiceUtil.isRemoteServiceInterface(aClass)) {
      return checkRemoteServiceInterface(aClass, manager, gwtFacet.getSdkVersion(), isOnTheFly);
    }
    return null;
  }

  private static ProblemDescriptor[] checkRemoteServiceInterface(final PsiClass aClass, final InspectionManager manager, final GwtVersion gwtVersion,
                                                                 boolean isOnTheFly) {
    List<ProblemDescriptor> problems = new ArrayList<>();
    for (PsiMethod method : aClass.getMethods()) {
      PsiDocComment comment = method.getDocComment();
      if (comment != null) {
        PsiDocTag[] tags = comment.findTagsByName(GwtGenericsUtil.TYPE_ARGS_TAG);
        if (tags.length > 0) {
          GenerifyServiceMethodFix fix = new GenerifyServiceMethodFix(method, gwtVersion);
          String message = GwtBundle.message("problem.description.gwt.type.args.tag.is.obsolete.in.gwt.1.5");
          problems.add(manager.createProblemDescriptor(comment, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
        }
      }
    }
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static final class GenerifyServiceMethodFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final GwtVersion myGwtVersion;

    private GenerifyServiceMethodFix(final PsiMethod method, final GwtVersion gwtVersion) {
      super(GwtBundle.message("quickfix.family.name.generify.types.in.method"), GwtBundle.message("quickfix.name.generify.types.in.method.0.instead.of.using.gwt.type.args.tags",
                                                PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME |
                                                                                                         PsiFormatUtilBase.SHOW_PARAMETERS,
                                                                           PsiFormatUtilBase.SHOW_TYPE)), method);
      myGwtVersion = gwtVersion;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiMethod psiMethod)) return;

      try {
        PsiType newReturnType;
        String returnTypeParameters = GwtGenericsUtil.getReturnTypeParametersString(psiMethod);
        if (returnTypeParameters != null) {
          PsiType returnType = psiMethod.getReturnType();
          newReturnType = returnType == null ? null : appendTypeParameters(returnType, returnTypeParameters, psiMethod);
        }
        else {
          newReturnType = null;
        }

        Int2ObjectMap<PsiType> newParameterTypes=new Int2ObjectOpenHashMap<>();
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
          PsiParameter parameter = parameters[i];
          String typeParametersString = GwtGenericsUtil.getTypeParametersString(psiMethod, parameter.getName());
          if (typeParametersString != null) {
            PsiType type = appendTypeParameters(parameter.getType(), typeParametersString, psiMethod);
            newParameterTypes.put(i, type);
          }
        }

        if (newReturnType == null && newParameterTypes.isEmpty()) return;

        List<PsiMethod> methods = findImplementations(project, psiMethod);
        if (methods == null) return;

        methods.add(0, psiMethod);

        Set<VirtualFile> affectedFiles = new HashSet<>();
        for (PsiMethod method : methods) {
          affectedFiles.add(method.getContainingFile().getVirtualFile());
        }

        PsiClass async = RemoteServiceUtil.findAsynchronousInterface(psiMethod.getContainingClass());
        PsiMethod asyncMethod = null;
        if (async != null) {
          asyncMethod = RemoteServiceUtil.findAsynchronousMethod(psiMethod);
          affectedFiles.add(async.getContainingFile().getVirtualFile());
        }

        if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(affectedFiles).hasReadonlyFiles()) {
          return;
        }

        SmartPsiElementPointer<PsiMethod> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiMethod);
        for (PsiMethod method : methods) {
          updateSignature(method, newReturnType, newParameterTypes);
        }
        PsiMethod newMethod = pointer.getElement();
        if (newMethod != null) {
          if (asyncMethod != null) {
            asyncMethod.delete();
            RemoteServiceUtil.copyMethodToAsync(newMethod, async, myGwtVersion);
          }
          GwtGenericsUtil.removeTypeArgsJavadocTags(newMethod);
        }
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    private static void updateSignature(final PsiMethod method, final PsiType newReturnType, final Int2ObjectMap<PsiType> newParameterTypes)
        throws IncorrectOperationException {
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();

      if (newReturnType != null) {
        PsiTypeElement returnTypeElement = method.getReturnTypeElement();
        if (returnTypeElement != null) {
          returnTypeElement.replace(elementFactory.createTypeElement(newReturnType));
        }
      }

      PsiParameter[] parameters = method.getParameterList().getParameters();
      for (IntIterator iterator = newParameterTypes.keySet().iterator(); iterator.hasNext(); ) {
        int i = iterator.nextInt();
        parameters[i].getTypeElement().replace(elementFactory.createTypeElement(newParameterTypes.get(i)));
      }
    }

    private static @Nullable List<PsiMethod> findImplementations(final Project project, PsiMethod psiMethod) {
      final List<PsiMethod> methods = new ArrayList<>();
      if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        Collection<PsiElement> elements = DefinitionsScopedSearch.search(psiMethod).findAll();
        for (PsiElement element : elements) {
          if (element instanceof PsiMethod) {
            methods.add((PsiMethod)element);
          }
        }
      }, GwtBundle.message("gwt.searching.for.implementations"), true, project)) {
        return null;
      }

      return methods;
    }

    private static PsiType appendTypeParameters(final @NotNull PsiType type, final @NotNull String typeParametersString,
                                                final @NotNull PsiElement context) throws IncorrectOperationException {
      if (type instanceof PsiClassType classType) {

        if (classType.isRaw()) {
          PsiElementFactory elementFactory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
          return elementFactory.createTypeFromText(type.getCanonicalText() + typeParametersString, context);
        }
      }
      return type;
    }
  }
}

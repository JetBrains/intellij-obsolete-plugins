package com.intellij.gwt.clientBundle.jam;

import com.intellij.gwt.clientBundle.ClientBundleUtil;
import com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

class CssClassNameConverter extends JamConverter<String> {
  @Override
  public String fromString(@Nullable String s, JamStringAttributeElement<String> context) {
    return s;
  }

  @Override
  public PsiReference @NotNull [] createReferences(@NotNull JamStringAttributeElement<String> context,
                                                   @NotNull PsiLanguageInjectionHost injectionHost) {

    final PsiAnnotation annotation = context.getParentAnnotationElement().getPsiElement();
    if (annotation == null) return PsiReference.EMPTY_ARRAY;

    final PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
    if (method == null) return PsiReference.EMPTY_ARRAY;

    final PsiClass psiClass = method.getContainingClass();
    if (psiClass == null) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new CssClassNameReference(injectionHost, ElementManipulators.getValueTextRange(injectionHost), psiClass)};
  }

  private static final class CssClassNameReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final PsiClass myPsiClass;

    private CssClassNameReference(PsiElement element, TextRange range, PsiClass psiClass) {
      super(element, range, false);
      myPsiClass = psiClass;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
      final Collection<CssClass> classes = collectDeclarations().get(getValue());
      return PsiElementResolveResult.createResults(classes);
    }

    private MultiMap<String, CssClass> collectDeclarations() {
      final Set<StylesheetFile> files = ClientBundleUtil.getStylesheetFiles(myPsiClass, true, true);
      final MultiMap<String, CssClass> declarations = new MultiMap<>();
      for (StylesheetFile file : files) {
        GwtCssDeclarationsManager.collectDeclarations(file, CssClass.class, declarations);
      }
      return declarations;
    }

    @Override
    public Object @NotNull [] getVariants() {
      final Set<String> classes = collectDeclarations().keySet();
      return ArrayUtilRt.toStringArray(classes);
    }
  }
}

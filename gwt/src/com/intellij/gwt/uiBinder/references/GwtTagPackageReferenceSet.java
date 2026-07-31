package com.intellij.gwt.uiBinder.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.intellij.gwt.uiBinder.UiBinderUtil.URN_IMPORT_PREFIX;
import static com.intellij.openapi.util.text.StringUtil.trimStart;
import static java.util.Collections.singleton;

public final class GwtTagPackageReferenceSet extends PackageReferenceSet {

  private final String initialContext;

  public GwtTagPackageReferenceSet(@NotNull String tagSubPackage, @NotNull XmlTag element) {
    super(tagSubPackage, element, element.getName().indexOf(tagSubPackage) + 1);
    String namespace = element.getNamespace();
    initialContext = namespace.startsWith(URN_IMPORT_PREFIX) ? trimStart(namespace, URN_IMPORT_PREFIX) : "";
  }

  @Override
  public Set<PsiPackage> getInitialContext() {
    return singleton(JavaPsiFacade.getInstance(getElement().getProject()).findPackage(initialContext));
  }

  @Override
  protected @NotNull PsiPackageReference createReference(TextRange range, int index) {
    return new GwtTagPackageReference(this, range, index);
  }
}

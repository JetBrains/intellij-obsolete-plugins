package com.intellij.gwt.references;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class GwtModuleReference<T extends PsiElement> extends PsiPolyVariantReferenceBase<T> {
  private final GwtModulesManager myGwtModulesManager;
  private final boolean myOutputName;

  public GwtModuleReference(T element, boolean outputName) {
    super(element);
    myOutputName = outputName;
    myGwtModulesManager = GwtModulesManager.getInstance(element.getProject());
  }

  protected @Nullable Module getModule() {
    return ModuleUtilCore.findModuleForPsiElement(myElement);
  }

  @Override
  public Object @NotNull [] getVariants() {
    List<LookupElementBuilder> variants = new ArrayList<>();
    for (GwtModule module : myGwtModulesManager.getAllGwtModules()) {
      final String fullName = myOutputName ? module.getOutputName() : module.getQualifiedName();
      LookupElementBuilder builder = LookupElementBuilder.create(fullName);
      String shortName = StringUtil.getShortName(fullName);
      if (!shortName.equals(fullName)) {
        builder = builder.withLookupString(shortName);
      }
      variants.add(builder);
    }

    return variants.toArray(new LookupElementBuilder[0]);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    String moduleName = getStringValue();
    if (moduleName != null) {
      final Module module = getModule();
      final GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope(myElement.getProject());
      final Collection<GwtModule> gwtModules;
      if (myOutputName) {
        gwtModules = myGwtModulesManager.findGwtModulesByOutputName(moduleName, scope);
      }
      else {
        gwtModules = myGwtModulesManager.findGwtModulesByQualifiedName(moduleName, scope);
      }
      for (GwtModule gwtModule : gwtModules) {
        results.add(new PsiElementResolveResult(gwtModule.getModuleXmlFile()));
      }
    }
    return results.toArray(ResolveResult.EMPTY_ARRAY);
  }

  protected abstract @Nullable String getStringValue();
}

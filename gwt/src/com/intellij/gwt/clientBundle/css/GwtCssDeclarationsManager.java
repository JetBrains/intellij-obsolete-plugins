package com.intellij.gwt.clientBundle.css;

import com.intellij.gwt.clientBundle.css.language.psi.GwtCssDef;
import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.CssElement;
import com.intellij.psi.css.CssElementVisitor;
import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.css.CssSelector;
import com.intellij.psi.css.CssSelectorSuffix;
import com.intellij.psi.css.CssSimpleSelector;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GwtCssDeclarationsManager {
  private static final Key<ParameterizedCachedValue<MultiMap<String, CssElement>, StylesheetFile>> GWT_CSS_DECLARATIONS_KEY = Key.create("GWT_CSS_DECLARATIONS");
  private static final GwtCssDeclarationsProvider DECLARATIONS_PROVIDER = new GwtCssDeclarationsProvider();

  private GwtCssDeclarationsManager() {
  }

  public static void collectCssClasses(final CssRuleset cssRuleset, final MultiMap<String, ? super CssClass> cssClass2Declaration) {
    for (CssSelector selector : cssRuleset.getSelectors()) {
      for (CssSimpleSelector simpleSelector : selector.getSimpleSelectors()) {
        final CssSelectorSuffix[] selectorSuffixes = simpleSelector.getSelectorSuffixes();
        for (CssSelectorSuffix suffix : selectorSuffixes) {
          if (suffix instanceof CssClass cssClass) {
            cssClass2Declaration.putValue(cssClass.getName(), cssClass);
          }
        }
      }
    }
  }

  public static @NotNull Collection<CssElement> findDeclarations(@NotNull StylesheetFile file, @NotNull String name) {
    return getOrCreateDeclarationsMap(file).get(name);
  }

  public static @NotNull <T extends CssElement> Collection<T> findDeclarations(@NotNull StylesheetFile file, @NotNull String name, @NotNull Class<T> aClass) {
    return ContainerUtil.findAll(getOrCreateDeclarationsMap(file).get(name), aClass);
  }

  public static <T extends CssElement> void collectDeclarations(@NotNull StylesheetFile file, @NotNull Class<? extends T> aClass,
                                                                @NotNull MultiMap<String, T> result) {
    final MultiMap<String, CssElement> declarations = getOrCreateDeclarationsMap(file);
    for (String name : declarations.keySet()) {
      for (CssElement element : declarations.get(name)) {
        if (aClass.isInstance(element)) {
          result.putValue(name, aClass.cast(element));
        }
      }
    }
  }

  private static MultiMap<String, CssElement> getOrCreateDeclarationsMap(@NotNull StylesheetFile file) {
    CachedValuesManager manager = CachedValuesManager.getManager(file.getProject());

    List<StylesheetFile> stylesheetFiles = getAllSourceFiles(file);
    if (stylesheetFiles.size() == 1) {
      return manager.getParameterizedCachedValue(file, GWT_CSS_DECLARATIONS_KEY, DECLARATIONS_PROVIDER, false, file);
    }

    MultiMap<String, CssElement> result = new MultiMap<>();
    for (StylesheetFile stylesheetFile : stylesheetFiles) {
      result.putAllValues(manager.getParameterizedCachedValue(stylesheetFile, GWT_CSS_DECLARATIONS_KEY,
                                                              DECLARATIONS_PROVIDER, false, stylesheetFile));
    }
    return result;
  }

  private static class GwtCssDeclarationsProvider implements ParameterizedCachedValueProvider<MultiMap<String, CssElement>, StylesheetFile> {
    @Override
    public CachedValueProvider.Result<MultiMap<String, CssElement>> compute(StylesheetFile param) {
      final MultiMap<String, CssElement> result = new MultiMap<>();
      param.accept(new CssElementVisitor() {
        @Override
        public void visitCssRuleset(CssRuleset ruleset) {
          collectCssClasses(ruleset, result);
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
          if (element instanceof GwtCssDef cssDef) {
            result.putValue(cssDef.getName(), cssDef);
          }
          else {
            element.acceptChildren(this);
          }
        }
      });
      return CachedValueProvider.Result.create(result, param);
    }
  }

  private static @NotNull List<StylesheetFile> getAllSourceFiles(StylesheetFile file) {
    if (file.getVirtualFile() instanceof VirtualFileWindow) {
      PsiElement context = file.getContext();
      if (context != null) {
        XmlTag styleTag = PsiTreeUtil.getParentOfType(context, XmlTag.class);
        if (styleTag != null) {
          return new UiStyleElement(styleTag).getStylesheetFiles();
        }
      }
    }
    return Collections.singletonList(file);
  }
}

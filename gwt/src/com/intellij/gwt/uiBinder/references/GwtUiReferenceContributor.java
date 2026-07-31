package com.intellij.gwt.uiBinder.references;

import com.intellij.gwt.references.GwtToCssClassReferenceProvider;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.psi.css.CssFileType;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.intellij.gwt.uiBinder.GwtUiXmlNamespaceCompletionContributor.XMLNS_VALUE_PATTERN;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_XML_SUFFIX;
import static com.intellij.gwt.uiBinder.UiBinderUtil.URN_IMPORT_PREFIX;
import static com.intellij.openapi.util.text.StringUtil.trimStart;
import static com.intellij.openapi.util.text.StringUtil.unquoteString;
import static com.intellij.patterns.PsiJavaPatterns.*;
import static com.intellij.patterns.XmlPatterns.*;

public final class GwtUiReferenceContributor extends PsiReferenceContributor {
  public static final class Holder {
    private static final ElementPattern<XmlAttribute> ATTRIBUTE_FROM_UI_NAMESPACE_PATTERN =
      or(xmlAttribute().withNamespace(UiBinderUtil.UI_BINDER_NAMESPACE),
         xmlAttribute().withParent(xmlTag().withNamespace(UiBinderUtil.UI_BINDER_NAMESPACE)));
    public static final ElementPattern<XmlFile> UI_XML_FILE_PATTERN = xmlFile().withName(string().endsWith(UI_XML_SUFFIX));
  }

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(xmlAttributeValue(xmlAttribute(UiBinderUtil.UI_FIELD_ATTRIBUTE)
                                         .and(Holder.ATTRIBUTE_FROM_UI_NAMESPACE_PATTERN)
                                         .inFile(Holder.UI_XML_FILE_PATTERN)),
                                        new UiFieldReferenceProvider());

    final ElementPattern<? extends PsiElement> uiHandlerAnnotationValuePattern =
      psiNameValuePair().withName(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME).withParent(
        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
          psiAnnotation().qName(UiBinderUtil.UI_HANDLER_ANNOTATION)));
    registrar.registerReferenceProvider(literalExpression().withParent(or(uiHandlerAnnotationValuePattern,
                                                                          PsiJavaPatterns.psiElement(PsiArrayInitializerMemberValue.class).withParent(uiHandlerAnnotationValuePattern))),
                                        new UiFieldReferenceProvider(), PsiReferenceRegistrar.HIGHER_PRIORITY);

    registrar.registerReferenceProvider(xmlAttributeValue(
      xmlAttribute().withLocalName("addStyleNames", "styleName", "stylePrimaryName")
                    .inFile(Holder.UI_XML_FILE_PATTERN)), new GwtToCssClassReferenceProvider());
    registrar.registerReferenceProvider(xmlAttributeValue().inFile(Holder.UI_XML_FILE_PATTERN),
                                        new QualifiedUiXmlReferenceProvider());

    registrar.registerReferenceProvider(xmlAttributeValue(
      xmlAttribute(UiStyleElement.SRC_ATTRIBUTE)
        .withParent(xmlTag().withLocalName(UiBinderUtil.UI_STYLE_TAG).withNamespace(UiBinderUtil.UI_BINDER_NAMESPACE))),
                                        new CssFileReferenceProvider());

    registrar.registerReferenceProvider(xmlAttributeValue(
      xmlAttribute(UiBinderUtil.UI_TYPE_ATTRIBUTE).and(Holder.ATTRIBUTE_FROM_UI_NAMESPACE_PATTERN)),
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(final @NotNull PsiElement element, @NotNull ProcessingContext context) {
          final JavaClassReferenceProvider provider = new JavaClassReferenceProvider() {
            @Override
            public GlobalSearchScope getScope(@NotNull Project project) {
              return element.getResolveScope();
            }
          };
          provider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.FALSE);
          return provider.getReferencesByElement(element, context);
        }
      });

    registrar.registerReferenceProvider(XMLNS_VALUE_PATTERN, new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        String elementText = unquoteString(element.getText());
        if (!elementText.startsWith(URN_IMPORT_PREFIX)) return PsiReference.EMPTY_ARRAY;

        PackageReferenceSet packageReferenceSet =
          new PackageReferenceSet(trimStart(elementText, URN_IMPORT_PREFIX), element, 1 + URN_IMPORT_PREFIX.length()); // 1 is for quote
        return packageReferenceSet.getPsiReferences();
      }
    });

    registrar.registerReferenceProvider(xmlTag().withNamespace(string().startsWith(URN_IMPORT_PREFIX)).inFile(Holder.UI_XML_FILE_PATTERN),
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
          XmlTag xmlTag = (XmlTag)element;
          String namespacePrefix = xmlTag.getNamespacePrefix();
          if (namespacePrefix.isEmpty()) return PsiReference.EMPTY_ARRAY;

          String xmlTagName = xmlTag.getName();
          String tagSubPackage = xmlTagName.indexOf('.', namespacePrefix.length() + 1) >= 0
                                 ? xmlTagName.substring(namespacePrefix.length() + 1, xmlTagName.lastIndexOf('.')) : "";
          return tagSubPackage.isEmpty() ? PsiReference.EMPTY_ARRAY
                                         : new GwtTagPackageReferenceSet(tagSubPackage, xmlTag).getPsiReferences();
        }
    });
  }

  private static class CssFileReferenceProvider extends PsiReferenceProvider {
    public static final Condition<PsiFileSystemItem> CSS_FILE_CONDITION = psiFileSystemItem -> psiFileSystemItem instanceof StylesheetFile;

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      if (element instanceof XmlAttributeValue) {
        final String path = ((XmlAttributeValue)element).getValue();
        final FileType[] types = {CssFileType.INSTANCE};
        CssFileReferenceSet referenceSet = new CssFileReferenceSet(path, element, types);
        List<FileReference> allReferences = referenceSet.getFullReferenceList();
        return allReferences.toArray(PsiReference.ARRAY_FACTORY.create(allReferences.size()));
      }
      return PsiReference.EMPTY_ARRAY;
    }

    private static class CssFileReferenceSet extends FileReferenceSet {

      private final Project myProject;
      private final Module myModule;
      private final VirtualFile myVirtualFile;
      private int mySkippedLevels = 0;
      private List<FileReference> myAllReferences = Collections.emptyList();

      CssFileReferenceSet(@NotNull String path, @NotNull PsiElement element, FileType @NotNull [] types) {
        super(path, element, 1, null, element.getContainingFile().getViewProvider().getVirtualFile().isCaseSensitive(), true, types, false);
        myProject = element.getProject();
        myModule = ModuleUtilCore.findModuleForPsiElement(element);
        myVirtualFile = element.getContainingFile().getVirtualFile();
        reparse();
        addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, file -> getContextByFileSystemItem(file, mySkippedLevels));
      }

      @Override
      public boolean couldBeConvertedTo(boolean relative) {
        return relative;
      }

      @Override
      protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
        //todo by default completion should take mySuitableTypes into account
        return CSS_FILE_CONDITION;
      }

      @Override
      protected List<FileReference> reparse(String str, int startInElement) {
        myAllReferences = super.reparse(str, startInElement);
        return myAllReferences.subList(mySkippedLevels, myAllReferences.size());
      }

      @Override
      public FileReference createFileReference(TextRange range, int index, String text) {
        if (mySkippedLevels == index && "..".equals(text)) {
          final int fileReferenceIndex = mySkippedLevels;
          mySkippedLevels++;
          return new FileReference(this, range, fileReferenceIndex, text) {

            private Collection<PsiFileSystemItem> myContexts = null;

            @Override
            protected @NotNull Collection<PsiFileSystemItem> getContexts() {
              if (myContexts == null) {
                myContexts = getContextByFileSystemItem(getContainingFile(), fileReferenceIndex);
              }
              return myContexts;
            }
          };
        }
        if (mySkippedLevels == 0 && index == 0 && !isAbsolutePathReference()) {
          return new FileReference(this, range, index, text) {

            private Collection<PsiFileSystemItem> myContexts = null;

            @Override
            protected @NotNull Collection<PsiFileSystemItem> getContexts() {
              if (myContexts == null) {
                myContexts = initContexts();

                if (myContexts == null) {
                  myContexts = super.getContexts();
                }
              }
              return myContexts;
            }

            private Collection<PsiFileSystemItem> initContexts() {
              if (myModule == null || myVirtualFile == null) return null;

              ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(myModule);
              VirtualFile[] roots = moduleRootManager.getSourceRoots(ProjectRootsUtil.isInTestSource(myVirtualFile, myProject));

              Collection<PsiFileSystemItem> superContexts = super.getContexts();
              Collection<PsiFileSystemItem> contexts = new HashSet<>(superContexts.size() + roots.length);

              PsiManager psiManager = PsiManager.getInstance(myProject);
              for (VirtualFile root : roots) {
                ContainerUtil.addIfNotNull(contexts, FileReferenceHelper.getPsiFileSystemItem(psiManager, root));
              }

              contexts.addAll(superContexts);
              return contexts;
            }
          };
        }
        return new FileReference(this, range, index - mySkippedLevels, text);
      }

      public List<FileReference> getFullReferenceList() {
        return myAllReferences;
      }

      private @NotNull Collection<PsiFileSystemItem> getContextByFileSystemItem(@Nullable PsiFileSystemItem item, int levelsToSkip) {
        for (int i = 0; item != null && i < levelsToSkip; i++) {
          item = item.getParent();
        }

        if (item == null) {
          return Collections.emptyList();
        }

        return super.getContextByFileSystemItem(item);
      }
    }
  }

  private static class UiFieldReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      if (element instanceof XmlAttributeValue) {
        return new PsiReference[] {new GwtUiFieldFromAttributeReference((XmlAttributeValue)element)};
      }
      if (element instanceof PsiLiteralExpression literalExpression) {
        if (literalExpression.getValue() instanceof String) {
          return new PsiReference[]{new GwtUiFieldFromHandlerReference(literalExpression)};
        }
      }
      return PsiReference.EMPTY_ARRAY;
    }
  }
}

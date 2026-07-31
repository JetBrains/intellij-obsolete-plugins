package com.intellij.gwt.uiBinder;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.TypeDescriptor;
import com.intellij.xml.impl.schema.XmlNSTypeDescriptorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.psi.search.PackageScope.packageScope;

public class GwtUiComponentsNSDescriptor implements XmlNSDescriptor, XmlNSTypeDescriptorProvider {
  private final XmlFile myFile;
  private final String myPackageName;
  private final Module myModule;
  private final String myNamespace;
  private final XmlNSDescriptor myDefaultNSDescriptor;
  private final Map<String, CachedValue<GwtUiComponentDescriptor>> myClassDescriptors = new HashMap<>();
  private final Map<String, CachedValue<GwtUiComponentDescriptor[]>> myAllDescriptors = new HashMap<>();

  public GwtUiComponentsNSDescriptor(@NotNull Module module, @NotNull String namespace, @Nullable XmlNSDescriptor defaultNSDescriptor) {
    myModule = module;
    myDefaultNSDescriptor = defaultNSDescriptor;
    myNamespace = namespace;
    myPackageName = myNamespace.substring(UiBinderUtil.URN_IMPORT_PREFIX.length());
    myFile = (XmlFile)PsiFileFactory.getInstance(module.getProject()).createFileFromText(myPackageName + ".xsd", "<root xmlns='" + myNamespace + "'/>");
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(@NotNull XmlTag tag) {
    final PsiFile file = tag.getContainingFile();
    if (!(file instanceof XmlFile) || !UiBinderUtil.isUiXmlFile((XmlFile)file)) {
      return null;
    }

    final String namespace = tag.getNamespace();
    if (!namespace.startsWith(UiBinderUtil.URN_IMPORT_PREFIX)) {
      return null;
    }

    String packageName = namespace.substring(UiBinderUtil.URN_IMPORT_PREFIX.length());
    final PsiClass psiClass = findClassForTag(tag, packageName);
    if (psiClass == null) {
      final XmlElementDescriptor childDescriptor = getUiChildDescriptor(tag, namespace, packageName);
      if (childDescriptor != null) {
        return childDescriptor;
      }
      if (myDefaultNSDescriptor != null) {
        return myDefaultNSDescriptor.getElementDescriptor(tag);
      }
      return null;
    }

    final GwtUiComponentDescriptor componentDescriptor = getOrCreateClassDescriptor(psiClass, packageName, tag.getNamespacePrefix());
    if (myDefaultNSDescriptor != null) {
      final XmlElementDescriptor additional = myDefaultNSDescriptor.getElementDescriptor(tag);
      if (additional != null) {
        return new GwtCompositeXmlElementDescriptor(componentDescriptor, additional);
      }
    }
    return componentDescriptor;
  }

  private @Nullable XmlElementDescriptor getUiChildDescriptor(XmlTag tag, String namespace, String packageName) {
    final XmlTag parentTag = tag.getParentTag();
    if (parentTag != null && namespace.equals(parentTag.getNamespace())) {
      PsiClass parentClass = findClassForTag(parentTag, packageName);
      if (parentClass != null) {
        final GwtUiComponentDescriptor classDescriptor = getOrCreateClassDescriptor(parentClass, packageName, tag.getNamespacePrefix());
        final XmlElementDescriptor childDescriptor = classDescriptor.getElementDescriptor(tag, parentTag);
        if (childDescriptor instanceof GwtUiChildElementDescriptor) {
          return childDescriptor;
        }
      }
    }
    return null;
  }

  private static @Nullable PsiClass findClassForTag(XmlTag tag, String packageName) {
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(tag.getProject());
    final String qualifiedName = StringUtil.getQualifiedName(packageName, tag.getLocalName());
    return psiFacade.findClass(qualifiedName, tag.getResolveScope());
  }

  private @NotNull GwtUiComponentDescriptor getOrCreateClassDescriptor(@NotNull PsiClass aClass, @NotNull String packageName, @NotNull String namespacePrefix) {
    String qualifiedName = aClass.getQualifiedName();
    String tagLocalName = qualifiedName == null ? aClass.getName() : StringUtil.trimStart(qualifiedName, packageName + ".");
    final String key = namespacePrefix + ":" + tagLocalName;
    CachedValue<GwtUiComponentDescriptor> value = myClassDescriptors.get(key);
    if (value == null) {
      value = CachedValuesManager.getManager(myModule.getProject()).createCachedValue(
        new ClassDescriptorCachedValueProvider(this, aClass, packageName, namespacePrefix), false);
      myClassDescriptors.put(key, value);
    }
    return value.getValue();
  }

  @Override
  public XmlElementDescriptor @NotNull [] getRootElementsDescriptors(@Nullable XmlDocument document) {
    if (document == null) return XmlElementDescriptor.EMPTY_ARRAY;

    return getComponentDescriptors(document.getRootTag());
  }

  public XmlElementDescriptor[] getComponentDescriptors(XmlTag tag) {
    if (tag == null) return XmlElementDescriptor.EMPTY_ARRAY;

    final String prefix = tag.getPrefixByNamespace(myNamespace);
    if (prefix == null) return XmlElementDescriptor.EMPTY_ARRAY;

    CachedValue<GwtUiComponentDescriptor[]> cached = myAllDescriptors.get(prefix);
    if (cached == null) {
      cached = CachedValuesManager.getManager(myModule.getProject()).createCachedValue(new AllDescriptorsCachedValueProvider(this, myModule,
                                                                                                                            myPackageName,
                                                                                                                            prefix), false);
      myAllDescriptors.put(prefix, cached);
    }
    return cached.getValue();
  }

  @Override
  public TypeDescriptor getTypeDescriptor(@NotNull String name, XmlTag context) {
    return myDefaultNSDescriptor instanceof XmlNSTypeDescriptorProvider
           ? ((XmlNSTypeDescriptorProvider)myDefaultNSDescriptor).getTypeDescriptor(name, context) : null;
  }

  @Override
  public TypeDescriptor getTypeDescriptor(XmlTag descriptorTag) {
    return myDefaultNSDescriptor instanceof XmlNSTypeDescriptorProvider
           ? ((XmlNSTypeDescriptorProvider)myDefaultNSDescriptor).getTypeDescriptor(descriptorTag) : null;
  }

  @Override
  public XmlFile getDescriptorFile() {
    return myFile;
  }

  @Override
  public PsiElement getDeclaration() {
    return myFile;
  }

  @Override
  public String getName(PsiElement context) {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public void init(PsiElement element) {
  }

  @Override
  public Object @NotNull [] getDependencies() {
    return new Object[]{ProjectRootManager.getInstance(myModule.getProject())};
  }

  private static class ClassDescriptorCachedValueProvider implements CachedValueProvider<GwtUiComponentDescriptor> {
    private final PsiClass myPsiClass;
    private final String myNamespacePrefix;
    private final String myNamespacePackage;
    private final GwtUiComponentsNSDescriptor myNSDescriptor;

    ClassDescriptorCachedValueProvider(@NotNull GwtUiComponentsNSDescriptor nsDescriptor, @NotNull PsiClass aClass,
                                       @NotNull String namespacePackage, @NotNull String namespacePrefix) {
      myPsiClass = aClass;
      myNamespacePrefix = namespacePrefix;
      myNamespacePackage = namespacePackage;
      myNSDescriptor = nsDescriptor;
    }

    @Override
    public Result<GwtUiComponentDescriptor> compute() {
      return Result.create(new GwtUiComponentDescriptor(myPsiClass, myNSDescriptor, myNamespacePackage, myNamespacePrefix),
                           PsiModificationTracker.MODIFICATION_COUNT);
    }
  }

  private static class AllDescriptorsCachedValueProvider implements CachedValueProvider<GwtUiComponentDescriptor[]> {
    private final String myPrefix;
    private final GwtUiComponentsNSDescriptor myNSDescriptor;
    private final Module myModule;
    private final String myPackageName;

    AllDescriptorsCachedValueProvider(GwtUiComponentsNSDescriptor nsDescriptor, Module module, String packageName,
                                             String prefix) {
      myPrefix = prefix;
      myNSDescriptor = nsDescriptor;
      myModule = module;
      myPackageName = packageName;
    }

    @Override
    public Result<GwtUiComponentDescriptor[]> compute() {
      List<GwtUiComponentDescriptor> descriptors = new ArrayList<>();
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myModule.getProject());
      final PsiPackage aPackage = psiFacade.findPackage(myPackageName);
      if (aPackage != null) {
        final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);
        final PsiClass baseClass = psiFacade.findClass(UiBinderUtil.WIDGET_BASE_CLASS, scope);
        if (baseClass != null) {
          ClassInheritorsSearch.search(baseClass, packageScope(aPackage, true), true).asIterable().forEach(psiClass -> {
              descriptors.add(myNSDescriptor.getOrCreateClassDescriptor(psiClass, myPackageName, myPrefix));
          });
        }
      }
      return Result.create(descriptors.toArray(new GwtUiComponentDescriptor[0]), PsiModificationTracker.MODIFICATION_COUNT);
    }
  }
}

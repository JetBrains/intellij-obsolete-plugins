// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.resolve.taglib;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.psi.impl.light.LightIdentifier;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrImplicitVariableImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrRenamableLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("SynchronizeOnThis")
public class TagLibNamespaceDescriptor {
  
  public static final String GSP_TAG_METHOD_MARKER = "Grails:GSP_TAG";
  
  private final String myNamespacePrefix;

  private final PsiManager myManager;

  private final List<PsiClass> myClasses = new ArrayList<>();

  private Map<String, PsiMethod> myVariableCache;
  private volatile Map<String, PsiMethod> myFinalCalculatedVariables;

  private volatile PsiVariable myDummyClassVariable;

  private final Module myModule;

  TagLibNamespaceDescriptor(String namespacePrefix, Module module) {
    this.myNamespacePrefix = namespacePrefix;
    myModule = module;
    this.myManager = PsiManager.getInstance(module.getProject());
  }

  void addClass(PsiClass aClass) {
    myClasses.add(aClass);
  }

  public @NotNull String getNamespacePrefix() {
    return myNamespacePrefix;
  }

  public List<PsiClass> getClasses() {
    return myClasses;
  }

  public Collection<PsiMethod> getAllTags() {
    if (myFinalCalculatedVariables != null) return myFinalCalculatedVariables.values();

    Map<String, PsiMethod> variableMap = new HashMap<>();

    Set<String> exclude = GspTagLibUtil.getExcludedTags(myNamespacePrefix);

    for (Map.Entry<String, PsiMethod> entry : GrailsUtils.collectClosureProperties(myClasses).entrySet()) {
      String name = entry.getKey();

      if (!exclude.contains(name)) {
        final PsiMember element = GrailsUtils.toField(entry.getValue());
        variableMap.put(name, new GspTagMethod(myManager, name, element));
      }
    }

    myFinalCalculatedVariables = variableMap;

    return variableMap.values();
  }

  public @Nullable PsiMethod getTag(final @NotNull String name) {
    if (myFinalCalculatedVariables != null) return myFinalCalculatedVariables.get(name);

    if (GspTagLibUtil.getExcludedTags(myNamespacePrefix).contains(name)) return null;

    synchronized (this) {
      if (myVariableCache != null && myVariableCache.containsKey(name)) {
        return myVariableCache.get(name);
      }
    }

    RecursionGuard.StackStamp stamp = RecursionManager.markStack();
    PsiMethod[] method = RecursionManager.doPreventingRecursion(name, true, () -> new PsiMethod[]{GrailsUtils.getClosureProperty(myClasses, name)});
    if (method == null) {
      return null;
    }

    PsiMethod res = method[0] == null ? null : new GspTagMethod(myManager, name, GrailsUtils.toField(method[0]));

    if (stamp.mayCacheNow()) {
      synchronized (this) {
        if (myVariableCache == null) {
          myVariableCache = new HashMap<>();
        }
        myVariableCache.put(name, res);
      }
    }

    return res;
  }

  public boolean processTags(@NotNull PsiScopeProcessor processor,
                             @NotNull ResolveState state,
                             @Nullable String name) {
    if (name == null) {
      for (PsiMethod variable : getAllTags()) {
        if (!processor.execute(variable, state)) return false;
      }
    }
    else {
      PsiMethod variable = getTag(name);
      if (variable != null) {
        if (!processor.execute(variable, state)) return false;
      }
    }

    return true;
  }

  public @Nullable PsiVariable getDummyClassVariable() {
    PsiVariable dummyClassVariable = myDummyClassVariable;
    if (dummyClassVariable == null) {
      JavaPsiFacade facade = JavaPsiFacade.getInstance(myManager.getProject());

      PsiClass objectClass = facade.findClass(CommonClassNames.JAVA_LANG_OBJECT, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule));
      if (objectClass == null) return null;

      PsiClass dummyClass = new DummyClass(objectClass);

      PsiElementFactory factory = facade.getElementFactory();

      PsiType type = factory.createType(dummyClass);

      dummyClassVariable = new GrImplicitVariableImpl(myManager, new LightIdentifier(myManager, myNamespacePrefix), type, false, null) {
        @Override
        public Icon getIcon(int flags) {
          return GroovyMvcIcons.Grails;
        }
      };

      myDummyClassVariable =dummyClassVariable;
    }

    return dummyClassVariable;
  }

  public class DummyClass extends LightClass {

    private final String myClassName;

    public DummyClass(PsiClass delegate) {
      super(delegate, GroovyLanguage.INSTANCE);
      myClassName = "_namespace_" + myNamespacePrefix;
    }

    @Override
    public String getName() {
      return myClassName;
    }

    @Override
    public String getQualifiedName() {
      return myClassName;
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                       @NotNull ResolveState state,
                                       PsiElement lastParent,
                                       @NotNull PsiElement place) {
      if (ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) {
        if (!processTags(processor, state, ResolveUtil.getNameHint(processor))) return false;
      }

      //return super.processDeclarations(processor, state, lastParent, place);
      return true;
    }
  }

  public class GspTagMethod extends GrRenamableLightMethodBuilder {
    private final @NotNull PsiType myReturnType;

    public GspTagMethod(PsiManager manager,
                        @NotNull String name,
                        @NotNull PsiMember navigationElement) {
      super(manager, name);
      setNavigationElement(navigationElement);
      addOptionalParameter("attr", CommonClassNames.JAVA_UTIL_MAP);
      addOptionalParameter("body", CommonClassNames.JAVA_LANG_OBJECT);

      setContainingClass(navigationElement.getContainingClass());

      setMethodKind(GSP_TAG_METHOD_MARKER);

      myReturnType = PsiElementFactory.getInstance(manager.getProject())
        .createTypeByFQClassName("org.codehaus.groovy.grails.web.util.StreamCharBuffer", navigationElement.getResolveScope());
    }

    @Override
    public Icon getIcon(int flags) {
      return GroovyMvcIcons.Grails;
    }

    @Override
    public @NotNull PsiType getReturnType() {
      return myReturnType;
    }

    public String getTagName() {
      return myNamespacePrefix + ':' + getName();
    }
    
    public String getNamespacePrefix() {
      return myNamespacePrefix;
    }

    @Override
    protected void onRename(@NotNull String newName) {
      PsiElement element = getNavigationElement();
      ((PsiNamedElement)element).setName(newName);
    }

    @Override
    public @NotNull SearchScope getUseScope() {
      return GlobalSearchScope
        .getScopeRestrictedByFileTypes(GlobalSearchScope.moduleWithDependentsScope(myModule), GroovyFileType.GROOVY_FILE_TYPE,
                                       GspFileType.GSP_FILE_TYPE);
    }
  }

}

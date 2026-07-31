/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.gwt.jsinject;

import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.codeInsight.completion.util.MethodParenthesesHandler;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

public class GwtClassMemberReference extends PsiReferenceBase.Poly<JSGwtReferenceExpressionImpl> {
  private static final Logger LOG = Logger.getInstance(GwtClassMemberReference.class);
  private static final Key<Map<String, CachedValue<MultiMap<String, PsiMember>>>> CACHED_MEMBER_MAP_KEY_FOR_SDK = Key.create("GWT_cached_member_signatures");
  private static final @NonNls String NEW_EXPRESSION = "new";
  private static final @NonNls Map<PsiType, String> ourPrimitiveTypes = new HashMap<>();
  static {
    ourPrimitiveTypes.put(PsiTypes.byteType(), "B");
    ourPrimitiveTypes.put(PsiTypes.charType(), "C");
    ourPrimitiveTypes.put(PsiTypes.doubleType(), "D");
    ourPrimitiveTypes.put(PsiTypes.floatType(), "F");
    ourPrimitiveTypes.put(PsiTypes.intType(), "I");
    ourPrimitiveTypes.put(PsiTypes.longType(), "J");
    ourPrimitiveTypes.put(PsiTypes.shortType(), "S");
    ourPrimitiveTypes.put(PsiTypes.booleanType(), "Z");
  }

  private final PsiReference myClassReference;

  public GwtClassMemberReference(final JSGwtReferenceExpressionImpl element, final @Nullable PsiReference classReference, final TextRange range) {
    super(element, range, false);
    myClassReference = classReference;
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    PsiClass psiClass = resolveQualifier();
    if (psiClass == null) return ResolveResult.EMPTY_ARRAY;

    MultiMap<String, PsiMember> map = getMembersMap(psiClass);
    if (map == null) return ResolveResult.EMPTY_ARRAY;
    return ContainerUtil.map2Array(map.get(getValue()), ResolveResult.class, PsiElementResolveResult::new);
  }

  public @Nullable PsiClass resolveQualifier() {
    if (myClassReference == null) return null;

    PsiElement element = myClassReference.resolve();
    if (!(element instanceof PsiClass psiClass)) {
      return null;
    }
    final GwtFacet facet = GwtFacet.findFacetByPsiElement(myElement);
    if (facet != null) {
      GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(myElement.getProject());
      VirtualFile virtualFile = myElement.getContainingFile().getVirtualFile();
      List<GwtModule> gwtModules = virtualFile == null ? emptyList() : gwtModulesManager.findGwtModulesByClientSourceFile(virtualFile);
      final PsiClass emulationClass = facet.getConfiguration().getSdk().findJreEmulationClass(gwtModules, psiClass);
      if (emulationClass != null) {
        return emulationClass;
      }
    }
    return psiClass;
  }

  private @Nullable MultiMap<String, PsiMember> getMembersMap(final @NotNull PsiClass aClass) {
    final GwtFacet gwtFacet = GwtFacet.findFacetByPsiElement(myElement);
    final GwtSdk sdk = gwtFacet != null ? gwtFacet.getConfiguration().getSdk() : null;
    String sdkUrl = sdk != null ? sdk.getHomeDirectoryUrl() : null;

    Map<String, CachedValue<MultiMap<String, PsiMember>>> map = aClass.getUserData(CACHED_MEMBER_MAP_KEY_FOR_SDK);
    if (map == null) {
      map = new HashMap<>();
      aClass.putUserData(CACHED_MEMBER_MAP_KEY_FOR_SDK, map);
    }

    CachedValue<MultiMap<String, PsiMember>> value = map.get(sdkUrl);
    if (value == null) {
      value = CachedValuesManager.getManager(aClass.getProject()).createCachedValue(() -> {
        GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(aClass.getProject());
        VirtualFile virtualFile = aClass.getContainingFile().getVirtualFile();
        List<GwtModule> gwtModules = virtualFile == null ? emptyList() : gwtModulesManager.findGwtModulesByClientSourceFile(virtualFile);
        MultiMap<String, PsiMember> map1 = buildMembersMap(gwtModules, aClass, sdk);
        return new CachedValueProvider.Result<>(map1, PsiModificationTracker.MODIFICATION_COUNT);
      }, false);
      map.put(sdkUrl, value);
    }

    return value.getValue();
  }

  private static MultiMap<String, PsiMember> buildMembersMap(List<GwtModule> gwtModules, @NotNull PsiClass aClass, @Nullable GwtSdk sdk) {
    @NonNls MultiMap<String, PsiMember> map = new MultiMap<>();

    collectMethods(gwtModules, aClass, map, new HashSet<>(), sdk);

    if (sdk == null || sdk.getVersion().isNewExpressionInJavaScriptSupported()) {
      for (PsiMethod constructor : aClass.getConstructors()) {
        StringBuilder signature = new StringBuilder(NEW_EXPRESSION);
        signature.append('(');
        if (!appendEnclosingClassType(signature, constructor.getContainingClass())) continue;
        if (!appendParameterTypes(signature, constructor)) continue;
        signature.append(')');
        map.putValue(signature.toString(), constructor);
      }
      if (aClass.getConstructors().length == 0) {
        StringBuilder signature = new StringBuilder(NEW_EXPRESSION);
        signature.append('(');
        if (appendEnclosingClassType(signature, aClass)) {
          signature.append(')');
          map.putValue(signature.toString(), aClass);
        }
      }
    }

    if (aClass.isEnum()) {
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory();
      try {
        final PsiMethod valuesMethod = elementFactory.createMethodFromText("public static " + aClass.getName() + "[] values() {}", aClass);
        map.putValue("values()", valuesMethod);
        final PsiMethod valueOfMethod = elementFactory.createMethodFromText("public static " + aClass.getName() + " valueOf(String name) {}", aClass);
        map.putValue("valueOf(Ljava/lang/String;)", valueOfMethod);
      }
      catch (IncorrectOperationException e) {
        LOG.info(e);
      }
    }

    for (PsiField psiField : aClass.getFields()) {
      map.putValue(psiField.getName(), psiField);
    }

    return map;
  }

  private static void collectMethods(List<GwtModule> gwtModules, @NotNull PsiClass aClass, MultiMap<String, ? super PsiMethod> result,
                                     Set<PsiClass> visited, @Nullable GwtSdk sdk) {
    boolean wildcardsSupported = sdk == null || sdk.getVersion().isWildcardMethodReferencesInJavaScriptSupported();
    if (sdk != null) {
      final PsiClass emulationClass = sdk.findJreEmulationClass(gwtModules, aClass);
      if (emulationClass != null) {
        aClass = emulationClass;
      }
    }

    if (!visited.add(aClass)) return;

    for (PsiMethod psiMethod : aClass.getMethods()) {
      if (psiMethod.isConstructor()) continue;

      StringBuilder signature = new StringBuilder(psiMethod.getName());
      signature.append('(');
      if (!appendParameterTypes(signature, psiMethod)) continue;
      signature.append(')');
      final String key = signature.toString();
      if (!result.containsKey(key)) {
        result.putValue(key, psiMethod);
        if (wildcardsSupported) {
          result.putValue(psiMethod.getName() + "(*)", psiMethod);
        }
      }
    }

    final PsiClass superClass = aClass.getSuperClass();
    if (superClass != null) {
      collectMethods(gwtModules, superClass, result, visited, sdk);
    }
    for (PsiClass psiClass : aClass.getInterfaces()) {
      collectMethods(gwtModules, psiClass, result, visited, sdk);
    }
  }

  private static boolean appendEnclosingClassType(final @NotNull StringBuilder signature, final @NotNull PsiClass psiClass) {
    PsiClass containingClass = psiClass.getContainingClass();
    PsiModifierList modifierList = psiClass.getModifierList();
    if (containingClass != null && (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.STATIC))) {
      String type = getClassTypeSignature(containingClass);
      if (type == null) return false;
      signature.append(type);
    }
    return true;
  }

  private static boolean appendParameterTypes(final StringBuilder signature, final PsiMethod psiMethod) {
    for (PsiParameter psiParameter : psiMethod.getParameterList().getParameters()) {
      String type = getTypeSignature(psiParameter.getType());
      if (type == null) return false;
      signature.append(type);
    }
    return true;
  }

  private static @Nullable @NonNls String getTypeSignature(PsiType type) {
    type = TypeConversionUtil.erasure(type);
    if (type instanceof PsiArrayType) {
      return "[" + getTypeSignature(((PsiArrayType)type).getComponentType());
    }
    if (type instanceof PsiPrimitiveType) {
      return ourPrimitiveTypes.get(type);
    }
    if (type instanceof PsiClassType) {
      PsiClass psiClass = ((PsiClassType)type).resolve();
      if (psiClass == null) return null;
      return getClassTypeSignature(psiClass);
    }
    return null;
  }

  private static @Nullable @NonNls String getClassTypeSignature(final PsiClass psiClass) {
    String name = getJvmClassName(psiClass);
    if (name == null) return null;
    return "L" + name + ";";
  }

  private static @Nullable String getJvmClassName(final @NotNull PsiClass psiClass) {
    PsiClass parent = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class, true);
    if (parent != null) {
      return getJvmClassName(parent) + "$" + psiClass.getName();
    }
    String qualifiedName = psiClass.getQualifiedName();
    return qualifiedName != null ? qualifiedName.replace('.', '/') : null;
  }

  @Override
  public Object @NotNull [] getVariants() {
    PsiClass psiClass = resolveQualifier();
    if (psiClass == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

    MultiMap<String, PsiMember> map = getMembersMap(psiClass);
    if (map == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

    Map<PsiMember, LookupElement> lookupItems = new HashMap<>();
    for (Map.Entry<String, Collection<PsiMember>> entry : map.entrySet()) {
      Collection<PsiMember> members = entry.getValue();
      PsiMember member = ContainerUtil.getFirstItem(members);
      if (member == null || members.size() > 1) continue;

      String lookupString = entry.getKey();
      LookupElement oldItem = lookupItems.get(member);
      if (oldItem != null && (oldItem.getLookupString().length() < lookupString.length()
                              || oldItem.getLookupString().length() == lookupString.length() && !lookupString.endsWith("(*)"))) continue;

      LookupElementBuilder builder;
      if (member instanceof PsiMethod method) {
        builder = JavaLookupElementBuilder.forMethod(method, lookupString, PsiSubstitutor.EMPTY, psiClass)
                                       .withInsertHandler(new MethodParenthesesHandler(method, true));
        if (method.isConstructor()) {
          builder = builder.withPresentableText(NEW_EXPRESSION);
        }
      }
      else if (member instanceof PsiField) {
        builder = JavaLookupElementBuilder.forField((PsiField)member, lookupString, psiClass);
      }
      else if (member instanceof PsiClass) {
        //default no-arg constructor
        builder = JavaLookupElementBuilder.forClass((PsiClass)member, lookupString)
                     .withInsertHandler(ParenthesesInsertHandler.NO_PARAMETERS);
      }
      else {
        LOG.error("unexpected member: " + member);
        continue;
      }
      lookupItems.put(member, builder);
    }
    for (Map.Entry<String, Collection<PsiMember>> entry : map.entrySet()) {
      if (entry.getValue().size() > 1) {
        for (PsiMember member : entry.getValue()) {
          LookupElement element = lookupItems.get(member);
          if (element != null) {
            element.putUserData(JavaCompletionUtil.FORCE_SHOW_SIGNATURE_ATTR, Boolean.TRUE);
          }
        }
      }
    }
    return lookupItems.values().toArray();
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    String oldText = myElement.getText();
    String oldElementName = getRangeInElement().substring(oldText);
    int i = oldElementName.indexOf('(');
    if (i != -1) {
      newElementName += oldElementName.substring(i);
    }
    String newText = getRangeInElement().replace(oldText, newElementName);
    ASTNode newNode = JSChangeUtil.createExpressionFromText(myElement.getProject(), newText, GwtLanguageDialect.GWT_DIALECT);
    return myElement.replace((JSExpression)newNode.getPsi());
  }

  @Override
  public PsiElement bindToElement(final @NotNull PsiElement element) throws IncorrectOperationException {
    if (element instanceof PsiMember) {
      return handleElementRename(((PsiMember)element).getName());
    }
    return super.bindToElement(element);
  }
}

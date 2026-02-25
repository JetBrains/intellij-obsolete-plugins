// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspClass;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspDeclarationHolder;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspRunMethod;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.resolve.CollectClassMembersUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GrGspClassImpl extends GroovyPsiElementImpl implements GrGspClass {

  // @see org.codehaus.groovy.grails.web.pages.GroovyPageWritable#writeTo
  // and org.codehaus.groovy.grails.web.pages.GroovyPageRequestBinding#lazyRequestBasedValuesMap #CHECK#
  @Language("JAVA")
  private static final String DYNAMIC_FIELDS_SOURCE = "class GspProperties {" +
                                                      //"  private final org.codehaus.groovy.grails.web.util.GrailsPrintWriter codecOut;" +
                                                      "  private final String actionName;" +
                                                      "  private final String controllerName;" +
                                                      "  private final javax.servlet.ServletContext application;" +
                                                      "  private final org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext applicationContext;" +
                                                      "  private final org.codehaus.groovy.grails.web.servlet.FlashScope flash;" +
                                                      "  private final org.codehaus.groovy.grails.commons.GrailsApplication grailsApplication;" +
                                                      "  private final org.codehaus.groovy.grails.web.util.GrailsPrintWriter out;" +
                                                      "  private final org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap params;" +
                                                      "  private final javax.servlet.http.HttpServletRequest request;" +
                                                      "  private final javax.servlet.http.HttpServletResponse response;" +
                                                      "  private final javax.servlet.http.HttpSession session;" +
                                                      "  private final groovy.lang.Binding pageScope;" +
                                                      "  private final org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest webRequest;" +
                                                      "  private final org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException exception;" +
                                                      "}";

  public GrGspClassImpl(ASTNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return "GspClass";
  }

  //copy-paste from Grails (GroovyPageCompiler)
  private static String generateJavaName(String str) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    boolean nextMustBeStartChar = true;
    while (i < str.length()) {
      char ch = str.charAt(i++);
      if (ch == '/') {
        nextMustBeStartChar = true;
        sb.append(ch);
      }
      else {
        // package or class name cannot start with a number
        if (nextMustBeStartChar && !Character.isJavaIdentifierStart(ch)) {
          sb.append('_');
        }
        nextMustBeStartChar = false;
        sb.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
      }
    }
    return sb.toString();
  }


  @Override
  public @Nullable @NonNls String getQualifiedName() {
    return "gsp.default." + getName();
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAnnotationType() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public @Nullable PsiReferenceList getExtendsList() {
    return null;
  }

  @Override
  public @Nullable PsiReferenceList getImplementsList() {
    return null;
  }

  @Override
  public PsiClassType @NotNull [] getExtendsListTypes() {
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public PsiClassType @NotNull [] getImplementsListTypes() {
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public @Nullable PsiClass getSuperClass() {
    return JavaPsiFacade.getInstance(getProject()).findClass("org.codehaus.groovy.grails.web.pages.GroovyPage", getResolveScope());
  }

  @Override
  public PsiClass @NotNull [] getInterfaces() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClass @NotNull [] getSupers() {
    final PsiClass superClass = getSuperClass();
    return superClass != null ? new PsiClass[]{superClass} : PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClassType @NotNull [] getSuperTypes() {
    PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    return new PsiClassType[]{factory.createTypeByFQClassName("org.codehaus.groovy.grails.web.pages.GroovyPage", getResolveScope())};
  }

  @Override
  public GrField @NotNull [] getFields() {
    GrGspDeclarationHolder[] holders = getRunMethod().getRunBlock().getDeclarationHolders();
    ArrayList<GrField> fields = new ArrayList<>();
    for (GrGspDeclarationHolder holder : holders) {
      ContainerUtil.addAll(fields, holder.getFields());
    }
    return fields.toArray(GrField.EMPTY_ARRAY);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {

    String name = ResolveUtil.getNameHint(processor);
    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

    GspGroovyFile groovyFile = (GspGroovyFile)getContainingFile().getOriginalFile();

    GspFile gspFile = groovyFile.getGspLanguageRoot();

    if (ResolveUtil.shouldProcessProperties(classHint)) {
      // Process variables defined in tags like g:each
      if (!processGspVariables(processor, name, groovyFile, state, place)) return false;

      // Process variables returned from model.
      if (name == null) {
        GspModelVariableModel variableModel = GspModelVariableModel.getInstance(gspFile);
        for (String argumenName : variableModel.getArgumentNames()) {
          PsiVariable variable = variableModel.getVariable(argumenName);
          if (variable != null) {
            if (!processor.execute(variable, state)) return false;
          }
        }
      }
      else {
        PsiVariable variable = GspModelVariableModel.getInstance(gspFile).getVariable(name);
        if (variable != null) {
          if (!processor.execute(variable, state)) return false;
        }
      }

      // Process builtin gsp properties. (e.g. request, responce, ...)
      for (PsiField field : DynamicMemberUtils.getMembers(place.getProject(), DYNAMIC_FIELDS_SOURCE).getFields(name)) {
        if (!processor.execute(field, state)) return false;
      }
    }

    if (!GspTagLibUtil.processGrailsTags(processor, place, state, name, classHint)) return false;

    PsiClass superClass = getSuperClass();
    if (superClass != null) {
      if (!ResolveUtil.processClassDeclarations(superClass, processor, state, lastParent, place)) return false;
    }

    return true;
  }

  private static boolean processGspVariables(final @NotNull PsiScopeProcessor processor,
                                             @Nullable String name,
                                             @NotNull GspGroovyFile file,
                                             @NotNull ResolveState state,
                                             @NotNull PsiElement place) {
    if (place.getContainingFile().getOriginalFile() != file) return true;
    if (!file.getSkeleton().processElementAtOffset(place.getTextOffset(), processor, name, state)) return false;

    return true;
  }

  @Override
  public GrMethod @NotNull [] getMethods() {
    GrGspDeclarationHolder[] holders = getRunMethod().getRunBlock().getDeclarationHolders();
    ArrayList<GrMethod> methods = new ArrayList<>();
    for (GrGspDeclarationHolder holder : holders) {
      ContainerUtil.addAll(methods, holder.getMethods());
    }
    return methods.toArray(GrMethod.EMPTY_ARRAY);
  }

  @Override
  public PsiMethod @NotNull [] getConstructors() {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiClass @NotNull [] getInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClassInitializer @NotNull [] getInitializers() {
    return PsiClassInitializer.EMPTY_ARRAY;
  }

  @Override
  public PsiField @NotNull [] getAllFields() {
    return PsiField.EMPTY_ARRAY;
  }

  @Override
  public PsiMethod @NotNull [] getAllMethods() {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiClass @NotNull [] getAllInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public @Nullable PsiField findFieldByName(String name, boolean checkBases) {
    if (!checkBases) {
      for (GrField field : getFields()) {
        if (name.equals(field.getName())) return field;
      }
      return null;
    }
    Map<String, CandidateInfo> fieldsMap = CollectClassMembersUtil.getAllFields(this);
    final CandidateInfo info = fieldsMap.get(name);
    return info == null ? null : (PsiField) info.getElement();
  }

  @Override
  public @Nullable PsiMethod findMethodBySignature(@NotNull PsiMethod patternMethod, boolean checkBases) {
    return null;
  }

  @Override
  public PsiMethod @NotNull [] findMethodsBySignature(@NotNull PsiMethod patternMethod, boolean checkBases) {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiMethod @NotNull [] findMethodsByName(@NonNls String name, boolean checkBases) {
    if (!checkBases) {
      List<PsiMethod> result = new ArrayList<>();
      for (GrMethod method : getMethods()) {
        if (name.equals(method.getName())) result.add(method);
      }

      return result.toArray(PsiMethod.EMPTY_ARRAY);
    }

    Map<String, List<CandidateInfo>> methodsMap = CollectClassMembersUtil.getAllMethods(this, true);
    return PsiImplUtil.mapToMethods(methodsMap.get(name));
  }

  @Override
  public @NotNull List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NotNull String name, boolean checkBases) {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    return Collections.emptyList();
  }

  @Override
  public @Nullable PsiClass findInnerClassByName(String name, boolean checkBases) {
    return null;
  }

  @Override
  public @Nullable PsiJavaToken getLBrace() {
    return null;
  }

  @Override
  public @Nullable PsiJavaToken getRBrace() {
    return null;
  }

  @Override
  public @Nullable PsiIdentifier getNameIdentifier() {
    return null;
  }

  @Override
  public PsiElement getScope() {
    return null;
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
  }

  @Override
  public boolean isInheritorDeep(@NotNull PsiClass baseClass, @Nullable PsiClass classToByPass) {
    return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
  }

  @Override
  public @Nullable PsiClass getContainingClass() {
    return null;
  }

  @Override
  public @NotNull Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull GrGspRunMethod getRunMethod() {
    GrGspRunMethod method = findChildByClass(GrGspRunMethod.class);
    assert method != null;
    return method;
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("There is no way to set up gsp class name");
  }

  @Override
  public @Nullable PsiModifierList getModifierList() {
    return null;
  }

  @Override
  public boolean hasModifierProperty(@NonNls @NotNull String name) {
    return false;
  }

  @Override
  public @Nullable PsiDocComment getDocComment() {
    return null;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean hasTypeParameters() {
    return false;
  }

  @Override
  public @Nullable PsiTypeParameterList getTypeParameterList() {
    return null;
  }

  @Override
  public PsiTypeParameter @NotNull [] getTypeParameters() {
    return PsiTypeParameter.EMPTY_ARRAY;
  }

  @Override
  public String getName() {
    final VirtualFile file = getContainingFile().getOriginalFile().getVirtualFile();
    assert file != null;
    String name = file.getName();

    final VirtualFile views = GrailsUtils.findViewsDirectory(this);
    if (views != null) {
      final String path = VfsUtilCore.getRelativePath(file, views, '/');
      if (path != null) {
        name = path;
      }
    }

    return generateJavaName(name);
  }

}

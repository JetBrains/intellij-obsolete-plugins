// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.tests;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.pluginSupport.buildTestData.GrailsBuildTestDataMemberProvider;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotation;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GrailsTestUtils {

  public static final String TEST_FOR = "grails.test.mixin.TestFor";
  public static final String TEST_MIXIN = "grails.test.mixin.TestMixin";
  public static final String MOCK = "grails.test.mixin.Mock";

  public static final Collection<String> TEST_ANNOTATIONS =
    List.of(TEST_FOR, TEST_MIXIN, MOCK, GrailsBuildTestDataMemberProvider.BUILD_ANNOTATION);

  private static final String[] TEST_SUFFIXES = {
    "Test", "Tests", "Spec", "Specification",
    "UnitTest", "UnitTests", "UnitSpec", "UnitSpecification",
    "IntegrationTest", "IntegrationTests", "IntegrationSpec", "IntegrationSpecification",
  };

  private GrailsTestUtils() { }

  public static Collection<PsiClass> getTestsForArtifact(@NotNull PsiClass psiClass, boolean searchTestForAnnotation) {
    final GrailsApplication application = GrailsApplicationManager.findApplication(psiClass);
    if (application == null) return Collections.emptyList();

    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0)) {
      Collection<PsiClass> result = new SmartList<>();
      GrailsTestUtilsKt.getTestsForArtifact(application, psiClass, result);
      return result;
    }

    final VirtualFile testDir = application.getRoot().findChild(GrailsUtils.TEST_DIR);
    if (testDir == null) return Collections.emptyList();

    GlobalSearchScope testDirScope = GlobalSearchScopesCore.directoryScope(application.getProject(), testDir, true);

    PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(application.getProject());

    final String className = psiClass.getName();

    List<PsiClass> res = new ArrayList<>();

    for (String suffix : TEST_SUFFIXES) {
      String testClassName = className + suffix;
      for (PsiClass testClass : shortNamesCache.getClassesByName(testClassName, testDirScope)) {
        if (AnnotationUtil.isAnnotated(testClass, TEST_FOR, 0)) continue;

        if (!(testClassName + ".groovy").equals(testClass.getContainingFile().getOriginalFile().getName())) continue;

        res.add(testClass);
      }
    }

    if (searchTestForAnnotation) {
      PsiClass annotationClass = JavaPsiFacade.getInstance(application.getProject()).findClass(TEST_FOR, psiClass.getResolveScope());
      if (annotationClass != null) {
        for (PsiReference reference : ReferencesSearch.search(annotationClass, testDirScope).findAll()) {
          PsiElement element = reference.getElement();
          if (!(element instanceof GrCodeReferenceElement)) continue;
          PsiElement annotation = element.getParent();
          if (!(annotation instanceof GrAnnotation)) continue;

          PsiAnnotationMemberValue value = ((GrAnnotation)annotation).findDeclaredAttributeValue("value");
          if (!(value instanceof GrReferenceExpression)) continue;

          PsiElement resolve = ((GrReferenceExpression)value).resolve();
          if (resolve != psiClass) continue;

          PsiAnnotationOwner annotationOwner = ((PsiAnnotation)annotation).getOwner();
          if (!(annotationOwner instanceof PsiModifierList)) continue;

          PsiElement annotatedClass = ((PsiModifierList)annotationOwner).getParent();

          if (!(annotatedClass instanceof GrClassDefinition)) continue;

          res.add((PsiClass)annotatedClass);
        }
      }
    }

    return res;
  }

  public static @Nullable PsiClass getTestedClass(@NotNull PsiClass testClass) {
    if (!(testClass instanceof GrClassDefinition)) return null;

    if (!GrailsUtils.isInGrailsTests(testClass)) return null;

    PsiModifierList modifierList = testClass.getModifierList();
    if (modifierList == null) return null;

    PsiAnnotation annotation = modifierList.findAnnotation(TEST_FOR);
    if (annotation != null) {
      PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
      if (!(value instanceof GrReferenceExpression)) return null;

      PsiElement resolve = ((GrReferenceExpression)value).resolve();
      if (!(resolve instanceof PsiClass aClass)) return null;

      if (GrailsArtifact.getType(aClass) == null) return null;

      return aClass;
    }

    Module module = ModuleUtilCore.findModuleForPsiElement(testClass);
    if (module == null) return null;

    VirtualFile appDir = GrailsFramework.getInstance().findAppDirectory(module);
    if (appDir == null) return null;

    GlobalSearchScope appDirScope = GlobalSearchScopesCore.directoryScope(module.getProject(), appDir, true);

    PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(module.getProject());

    String testClassName = testClass.getName();

    for (String suffix : TEST_SUFFIXES) {
      if (testClassName.endsWith(suffix)) {
        String artifactName = StringUtil.trimEnd(testClassName, suffix);
        for (PsiClass artifact : shortNamesCache.getClassesByName(artifactName, appDirScope)) {
          if (GrailsArtifact.getType(artifact) != null) {
            return artifact;
          }
        }
      }
    }

    return null;
  }

  public static @Nullable String getTestType(@NotNull PsiClass testClass) {
    VirtualFile virtualFile = testClass.getContainingFile().getOriginalFile().getVirtualFile();
    if (virtualFile == null) return null;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(testClass.getProject()).getFileIndex();
    VirtualFile sourceRoot = fileIndex.getSourceRootForFile(virtualFile);
    if (sourceRoot == null) return null;

    VirtualFile parent = sourceRoot.getParent();
    if (parent == null || !parent.getName().equals("test")) {
      return null;
    }

    return sourceRoot.getName();
  }

  public static PsiType getTestedClassClass(GrReferenceExpression ref) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(ref.getProject());
    PsiClass classClass = facade.findClass(CommonClassNames.JAVA_LANG_CLASS, ref.getResolveScope());
    if (classClass == null) return null;

    PsiType testedClass = getTestedClass(ref);
    if (testedClass != null) {
      return facade.getElementFactory().createType(classClass, testedClass);
    }

    return facade.getElementFactory().createType(classClass);
  }

  public static @Nullable PsiType getTestedClass(GrReferenceExpression ref) {
    PsiClass testClass = PsiUtil.getContainingNotInnerClass(ref);
    if (testClass == null) return null;

    String testClassName = testClass.getQualifiedName();
    if (testClassName == null) return null;

    String testedClassName = getTestedClassName(testClassName);
    if (testedClassName == null) return null;

    JavaPsiFacade facade = JavaPsiFacade.getInstance(testClass.getProject());
    PsiClass testedClass = facade.findClass(testedClassName, testClass.getResolveScope());
    if (testedClass == null) return null;

    return facade.getElementFactory().createType(testedClass);
  }

  // See MvcSpec.findClassUnderTestConventiallyBySuffix()
  public static @Nullable String getTestedClassName(String testClassName) {
    int suffixIndex = Math.max(testClassName.lastIndexOf("Controller"), testClassName.lastIndexOf("TagLib"));
    if (suffixIndex == -1) return null;

    int testedClassNameLength = suffixIndex + (testClassName.charAt(suffixIndex) == 'C' ? "Controller".length() : "TagLib".length());

    if (testClassName.indexOf('.', testedClassNameLength) != -1) return null;

    return testClassName.substring(0, testedClassNameLength);
  }

}

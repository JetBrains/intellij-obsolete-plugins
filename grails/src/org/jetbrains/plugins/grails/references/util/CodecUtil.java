// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyTargetElementEvaluator;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class CodecUtil {
  private static final Map<String, Couple<Object>> standardCodecsTypeHint = new HashMap<>();

  static {
    PsiType byteArray = PsiTypes.byteType().createArrayType();
    standardCodecsTypeHint.put("Base64", Couple.of(CommonClassNames.JAVA_LANG_STRING, byteArray));
    standardCodecsTypeHint.put("Hex", Couple.of(CommonClassNames.JAVA_LANG_STRING, byteArray));
    standardCodecsTypeHint.put("HTML", Couple.of(CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING));
    standardCodecsTypeHint.put("JavaScript", Couple.of(CommonClassNames.JAVA_LANG_STRING, null));
    standardCodecsTypeHint.put("MD5Bytes", Couple.of(byteArray, null));
    standardCodecsTypeHint.put("MD5", Couple.of(CommonClassNames.JAVA_LANG_STRING, null));
    standardCodecsTypeHint.put("SHA1Bytes", Couple.of(byteArray, null));
    standardCodecsTypeHint.put("SHA1", Couple.of(CommonClassNames.JAVA_LANG_STRING, null));
    standardCodecsTypeHint.put("SHA256Bytes", Couple.of(byteArray, null));
    standardCodecsTypeHint.put("SHA256", Couple.of(CommonClassNames.JAVA_LANG_STRING, null));
    standardCodecsTypeHint.put("URL", Couple.of(CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING));
  }

  private CodecUtil() {

  }

  public static Map<String, Couple<PsiMethod>> getCodecMap(final @NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(
      module,
      () -> CachedValueProvider.Result.create(
        calculateCodecsMap(module),
        MvcModuleStructureSynchronizer.getInstance(module.getProject()).getFileAndRootsModificationTracker())
    );
  }

  private static Map<String, Couple<PsiMethod>> calculateCodecsMap(Module module) {
    Map<String, Couple<PsiMethod>> res = new HashMap<>();

    PsiPackage aPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage("org.codehaus.groovy.grails.plugins.codecs");
    if (aPackage != null) {
      parseCodecs(res, Arrays.asList(aPackage.getClasses(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false))));
      parseCodecs(res, GrailsArtifact.CODEC.getInstances(module).values());
    }

    return res;
  }

  private static void parseCodecs(Map<String, Couple<PsiMethod>> result, Collection<? extends PsiClass> codecClasses) {
    for (PsiClass codecClass : codecClasses) {
      String name = codecClass.getName();
      if (name != null && name.endsWith("Codec")) {
        String codecName = name.substring(0, name.length() - "Codec".length());

        PsiMethod encodeMethod = createMethod(codecClass, true, codecName);
        PsiMethod decodeMethod = createMethod(codecClass, false, codecName);

        result.put(codecName, Couple.of(encodeMethod, decodeMethod));
      }
    }
  }

  private static @Nullable PsiMethod createMethod(final PsiClass codecClass, boolean isEncode, String codecName) {
    String elementName;
    String methodName;
    if (isEncode) {
      elementName = "encode";
      methodName = "encodeAs" + codecName;
    }
    else {
      elementName = "decode";
      methodName = "decode" + codecName;
    }

    Supplier<PsiType> returnType = null;
    PsiElement navigationElement = null;

    Couple<Object> pair = standardCodecsTypeHint.get(codecName);
    if (pair != null) {
      Object obj = isEncode ? pair.first : pair.second;
      if (obj == null) return null;
      if (obj instanceof PsiType) {
        returnType = () -> (PsiType)obj;
      }
      else {
        PsiType type = JavaPsiFacade.getElementFactory(codecClass.getProject()).createTypeByFQClassName((String)obj,
                                                                                                        codecClass.getResolveScope());
        returnType = () -> type;
      }
    }

    PsiField field = codecClass.findFieldByName(elementName, false);
    if (field != null && field.hasModifierProperty(PsiModifier.STATIC)) {
      navigationElement = field;

      if (returnType == null) {
        if (field instanceof GrField) {
          final GrExpression initializer = ((GrField)field).getInitializerGroovy();
          if (initializer instanceof GrClosableBlock) {
            returnType = NotNullLazyValue.createValue(() -> {
              PsiType result = ((GrClosableBlock)initializer).getReturnType();
              if (result != null) {
                return result;
              }
              return PsiType.getJavaLangObject(codecClass.getManager(), codecClass.getResolveScope());
            });
          }
        }

        if (returnType == null) {
          PsiClassType v = PsiType.getJavaLangObject(codecClass.getManager(), codecClass.getResolveScope());
          returnType = () -> v;
        }
      }
    }
    else {
      for (final PsiMethod psiMethod : codecClass.findMethodsByName(elementName, false)) {
        if (psiMethod.hasModifierProperty(PsiModifier.STATIC) && psiMethod.getParameterList().getParametersCount() == 1) {
          navigationElement = psiMethod;

          if (returnType == null) {
            returnType = NotNullLazyValue.createValue(() -> {
              PsiType res;
              if (psiMethod instanceof GrMethod) {
                res = ((GrMethod)psiMethod).getInferredReturnType();
              }
              else {
                res = psiMethod.getReturnType();
              }

              if (res == null) {
                return PsiType.getJavaLangObject(codecClass.getManager(), codecClass.getResolveScope());
              }

              return res;
            });
          }

          break;
        }
      }
    }

    if (navigationElement != null) {
      LightMethodBuilder res = new LightMethodBuilder(codecClass.getManager(), GroovyLanguage.INSTANCE, methodName);
      res.putUserData(GroovyTargetElementEvaluator.NAVIGATION_ELEMENT_IS_NOT_TARGET, Boolean.TRUE);
      res.setMethodReturnType(returnType);

      res.setContainingClass(codecClass);
      res.setModifiers(PsiModifier.PUBLIC);
      res.setNavigationElement(navigationElement);
      return res;
    }

    return null;
  }
}

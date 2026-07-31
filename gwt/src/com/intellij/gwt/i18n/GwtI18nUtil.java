/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.gwt.i18n;

import com.intellij.gwt.codeInsight.GwtMethodGenerationUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.lang.properties.IProperty;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.List;

public final class GwtI18nUtil {
  private static final Logger LOG = Logger.getInstance(GwtI18nUtil.class);
  public static final @NonNls String CONSTANTS_INTERFACE_NAME = "com.google.gwt.i18n.client.Constants";
  public static final @NonNls String MESSAGES_INTERFACE_NAME = "com.google.gwt.i18n.client.Messages";
  public static final @NonNls String LOCALIZABLE_INTERFACE_NAME = "com.google.gwt.i18n.client.LocalizableResource";
  private static final @NonNls String GWT_PROPERTY_KEY_JAVADOC = "/**\n*@gwt.key {0}\n*/";
  public static final @NonNls String KEY_ANNOTATION_CLASS = "com.google.gwt.i18n.client.LocalizableResource.Key";
  public static final @NonNls String GWT_KEY_TAG = "gwt.key";


  private GwtI18nUtil() {
  }

  public static void navigateToProperty(@NotNull IProperty property) {
    property.navigate(true);
  }

  public static String suggestPropertyKey(@NotNull String value, final PsiNameHelper nameHelper, final LanguageLevel languageLevel) {
    List<String> words = StringUtil.getWordsIn(value);
    StringBuilder key = new StringBuilder();
    for (String word : words) {
      if (!key.isEmpty()) {
        word = StringUtil.capitalize(word);
      }
      else {
        word = StringUtil.decapitalize(word);
      }
      key.append(word);
    }
    return GwtMethodGenerationUtil.addPrefixIfNeeded(key.toString(), "property", nameHelper, languageLevel);
  }

  public static String convertPropertyName2MethodName(String propertyName, final PsiNameHelper nameHelper, final LanguageLevel languageLevel) {
    return GwtMethodGenerationUtil.convertStringToMethodName(propertyName, nameHelper, languageLevel, "getProperty");
  }

  public static void addMethod(PsiClass aClass, String propertyName, @Nullable String propertyValue, final GwtVersion gwtVersion) {
    try {
      PsiMethod method = addMethod(aClass, propertyName, gwtVersion);
      PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();
      int parametersCount = getParametersCount(propertyValue);
      PsiClassType javaLangString = PsiType.getJavaLangString(method.getManager(), GlobalSearchScope.allScope(method.getProject()));
      for (int i = 0; i < parametersCount; i++) {
        final PsiParameter psiParameter = psiElementFactory.createParameter("p" + i, javaLangString);
        if (aClass.isInterface()) {
          PsiUtil.setModifierProperty(psiParameter, PsiModifier.FINAL, false);
        }
        method.getParameterList().add(psiParameter);
      }
      CodeStyleManager.getInstance(aClass.getProject()).reformat(method);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  public static int getParametersCount(final @Nullable String propertyValue) {
    if (propertyValue == null) return 0;

    int maxParameter = -1;
    int i = propertyValue.indexOf('{');
    while (i != -1) {
      int end = propertyValue.indexOf('}', i);
      if (end == -1) break;

      int comma = propertyValue.indexOf(',', i);
      if (comma != -1 && comma < end) {
        end = comma;
      }

      try {
        int parameter = Integer.parseInt(propertyValue.substring(i+1, end));
        maxParameter = Math.max(maxParameter, parameter);
      }
      catch (NumberFormatException ignored) {
      }
      i = propertyValue.indexOf('{', end);
    }
    return maxParameter + 1;
  }

  public static PsiMethod addMethod(PsiClass aClass, String propertyName, final GwtVersion gwtVersion) throws IncorrectOperationException {
    String methodName = convertPropertyName2MethodName(propertyName, PsiNameHelper.getInstance(aClass.getProject()),
                                                       PsiUtil.getLanguageLevel(aClass));

    final PsiMethod addedMethod = GwtMethodGenerationUtil.addStringMethod(aClass, methodName);

    if (!propertyName.equals(methodName)) {
      addKeyAnnotationOrJavaDoc(addedMethod, propertyName, gwtVersion,
                                JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory());
    }
    return addedMethod;
  }

  public static void addKeyAnnotationOrJavaDoc(@NotNull PsiMethod method, @NotNull String propertyName, @NotNull GwtVersion gwtVersion,
                                               @NotNull PsiElementFactory elementFactory) {
    if (gwtVersion.isGenericsSupported()) {
      addKeyAnnotation(propertyName, method, elementFactory);
    }
    else {
      final String commentText = MessageFormat.format(GWT_PROPERTY_KEY_JAVADOC, propertyName);
      PsiComment comment = elementFactory.createCommentFromText(commentText, method.getContainingClass());
      method.addBefore(comment, method.getFirstChild());
    }
  }

  public static void addKeyAnnotation(final String propertyName, final PsiMethod method, final PsiElementFactory elementFactory)
      throws IncorrectOperationException {
    final String annotationText = "@" + KEY_ANNOTATION_CLASS + "(\"" + propertyName + "\")";
    PsiAnnotation annotation = elementFactory.createAnnotationFromText(annotationText, method);
    method.getModifierList().add(annotation);
  }

  public static String getPropertyName(final PsiMethod method) {
    PsiAnnotation annotation = method.getModifierList().findAnnotation(KEY_ANNOTATION_CLASS);
    if (annotation != null) {
      PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
      if (attributes.length == 1) {
        String key = JamCommonUtil.getObjectValue(attributes[0].getValue(), String.class);
        if (key != null) {
          return key;
        }
      }
    }

    final PsiDocComment docComment = method.getDocComment();
    if (docComment != null) {
      final PsiDocTag tag = docComment.findTagByName(GWT_KEY_TAG);
      if (tag != null) {
        final PsiDocTagValue psiDocTagValue = tag.getValueElement();
        if (psiDocTagValue != null) {
          final String text = psiDocTagValue.getText();
          if (text != null) {
            return text;
          }
        }
      }
    }
    return method.getName();
  }
}

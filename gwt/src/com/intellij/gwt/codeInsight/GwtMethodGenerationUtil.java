package com.intellij.gwt.codeInsight;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class GwtMethodGenerationUtil {
  private static final Logger LOG = Logger.getInstance(GwtMethodGenerationUtil.class);

  private GwtMethodGenerationUtil() {
  }

  public static String addPrefixIfNeeded(String id, final @NonNls String prefix, final PsiNameHelper nameHelper, final LanguageLevel languageLevel) {
    if (!nameHelper.isIdentifier(id, languageLevel)) {
      id = prefix + StringUtil.capitalize(id);
      if (!nameHelper.isIdentifier(id, languageLevel)) {
        id = prefix;
      }
    }
    return id;
  }

  public static String convertStringToMethodName(String string, final PsiNameHelper nameHelper, final LanguageLevel languageLevel,
                                                 final String prefix) {
    if (nameHelper.isIdentifier(string, languageLevel)) {
      return string;
    }

    final String[] words = string.split("[\\.-]");
    StringBuilder builder = new StringBuilder();
    for (String word : words) {
      String id = convert2Id(word);
      if (!id.isEmpty()) {
        if (!builder.isEmpty()) {
          id = StringUtil.capitalize(id);
        }
        builder.append(id);
      }
    }

    return addPrefixIfNeeded(builder.toString(), prefix, nameHelper, languageLevel);
  }

  private static String convert2Id(final String word) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < word.length(); i++) {
      char c = word.charAt(i);
      if (builder.isEmpty() && Character.isJavaIdentifierStart(c)
          || !builder.isEmpty() && Character.isJavaIdentifierPart(c)) {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  public static PsiMethod addStringMethod(@NotNull PsiClass aClass, @NotNull String methodName) {
    final PsiManager psiManager = aClass.getManager();
    final PsiClassType javaLangString = PsiType.getJavaLangString(psiManager, aClass.getResolveScope());
    final PsiMethod method = JavaPsiFacade.getInstance(psiManager.getProject()).getElementFactory().createMethod(methodName, javaLangString);
    PsiUtil.setModifierProperty(method, PsiModifier.PUBLIC, false);
    final PsiCodeBlock body = method.getBody();
    LOG.assertTrue(body != null);
    body.delete();
    return (PsiMethod)aClass.add(method);
  }
}

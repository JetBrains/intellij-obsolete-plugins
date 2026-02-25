// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspExprInjection;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterGroovyElement;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.lexer.TokenSets;
import org.jetbrains.plugins.groovy.lang.psi.GroovyTokenSets;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrConditionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightParameter;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrMethodWrapper;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Collection;

public final class GrailsPsiUtil {

  private GrailsPsiUtil() {
  }

  public static boolean processLogVariable(@NotNull PsiScopeProcessor processor, @NotNull PsiClass aClass, @Nullable String nameHint) {
    if (nameHint == null || nameHint.equals("log")) {
      if (ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) {
        if (aClass.findFieldByName("log", false) == null && !aClass.isInterface()) {
          GrLightField logField = new GrLightField(aClass, "log", "org.apache.commons.logging.Log");
          logField.getModifierList().setModifiers(GrModifierFlags.PRIVATE_MASK + GrModifierFlags.STATIC_MASK + GrModifierFlags.FINAL_MASK);
          return processor.execute(logField, ResolveState.initial());
        }
      }
    }

    return true;
  }

  public static boolean enhance(@NotNull PsiScopeProcessor processor, @NotNull PsiClass apiClass, @NotNull PsiType objectType) {
    return enhance(processor, apiClass, objectType, null);
  }

  public static boolean enhance(@NotNull PsiScopeProcessor processor,
                                @NotNull PsiClass apiClass,
                                @NotNull PsiType objectType,
                                @Nullable Object methodKind) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return true;
    String nameHint = ResolveUtil.getNameHint(processor);

    PsiMethod[] methods;

    if (nameHint == null) {
      methods = apiClass.getAllMethods();
    }
    else {
      methods = apiClass.findMethodsByName(nameHint, true);
    }

    for (PsiMethod method : methods) {
      if (!isCandidateMethod(method, objectType)) continue;

      GrMethodWrapper builder = GrMethodWrapper.wrap(method);

      if (!method.hasModifierProperty(PsiModifier.STATIC)) {
        builder.getParameterList().removeParameter(0);
      }

      builder.addModifier(PsiModifier.PUBLIC);

      builder.setMethodKind(methodKind);

      if (!processor.execute(builder, ResolveState.initial())) return false;
    }

    return true;
  }

  // See BaseApiProvider.isNotExcluded()
  private static boolean isCandidateMethod(PsiMethod method, @NotNull PsiType objectType) {
    if (!method.hasModifierProperty(PsiModifier.PUBLIC)
        || method.hasModifierProperty(PsiModifier.ABSTRACT)
        || method.getName().indexOf('$') != -1
        || method.isConstructor()
      ) {
      return false;
    }

    if (!method.hasModifierProperty(PsiModifier.STATIC)) {
      PsiParameterList parameterList = method.getParameterList();
      PsiParameter[] parameters = parameterList.getParameters();
      if (parameters.length == 0) return false;
      if (!TypesUtil.isAssignableByMethodCallConversion(parameters[0].getType(), objectType, method)) return false;
    }

    return true;
  }

  public static GrLightMethodBuilder substitute(PsiMethod method, PsiSubstitutor substitutor) {
    GrLightMethodBuilder res = GrMethodWrapper.wrap(method);
    res.setReturnType(substitutor.substitute(res.getReturnType()));

    res.getParameterList().clear();
    for (PsiParameter parameter : method.getParameterList().getParameters()) {
      GrLightParameter p = new GrLightParameter(parameter.getName(), substitutor.substitute(parameter.getType()), res);

      if (parameter instanceof GrParameter) {
        p.setOptional(((GrParameter)parameter).isOptional());
      }

      res.addParameter(p);
    }

    return res;
  }

  public static boolean process(String className, PsiScopeProcessor processor, PsiElement place, ResolveState state) {
    PsiClass aClass = JavaPsiFacade.getInstance(place.getProject()).findClass(className, place.getResolveScope());

    if (aClass != null) {
      return ResolveUtil.processClassDeclarations(aClass, processor, state, null, place);
    }

    return true;
  }

  public static @Nullable String getPlainLabelName(GrNamedArgument namedArgument) {
    GrArgumentLabel label = namedArgument.getLabel();
    if (label == null) return null;

    PsiElement labelElement = label.getNameElement();

    if (!(labelElement instanceof LeafElement)) return null;

    IElementType type = ((LeafElement)labelElement).getElementType();
    if (GroovyTokenTypes.mIDENT == type || TokenSets.KEYWORDS.contains(type)) {
      return labelElement.getText();
    }

    if (GroovyTokenSets.STRING_LITERALS.contains(type)) {
      return (String)GrLiteralImpl.getLiteralValue(labelElement);
    }

    return null;
  }

  public static boolean isSimpleAttribute(@NotNull XmlAttributeValue value) {
    PsiElement leftQuote = value.getFirstChild();

    if (!PsiImplUtil.isLeafElementOfType(leftQuote, GspTokenTypes.GSP_ATTR_VALUE_START_DELIMITER)) {
      return false;
    }

    PsiElement textToken = leftQuote.getNextSibling();

    if (!PsiImplUtil.isLeafElementOfType(textToken, XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)) {
      return false;
    }

    PsiElement rightQuote = textToken.getNextSibling();

    if (!(rightQuote instanceof XmlToken) || ((XmlToken)rightQuote).getTokenType() != GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER) {
      return false;
    }

    return rightQuote.getNextSibling() == null;
  }

  public static @Nullable XmlAttributeValue getAttributeValue(@NotNull XmlTag tag, String attributeName) {
    XmlAttribute attribute = tag.getAttribute(attributeName);
    if (attribute == null) return null;
    XmlAttributeValue value = attribute.getValueElement();
    if (value == null) return null;

    // Return null if value.getValue().trim().length() == 0
    PsiElement firstChild = value.getFirstChild();

    if (PsiImplUtil.isLeafElementOfType(firstChild, GspTokenTypes.GSP_ATTR_VALUE_START_DELIMITER)) {
      PsiElement secondChild = firstChild.getNextSibling();
      if (PsiImplUtil.isLeafElementOfType(firstChild, GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER)) return null;

      if (PsiImplUtil.isLeafElementOfType(secondChild, XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)) {
        if (PsiImplUtil.isLeafElementOfType(secondChild.getNextSibling(), GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER)) {
          if (secondChild.getText().trim().isEmpty()) {
            return null;
          }
        }
      }
    }

    return value;
  }

  public static @Nullable PsiType getAttributeExpressionType(@NotNull XmlAttributeValue value) {
    PsiElement leftQuote = value.getFirstChild();

    if (!(leftQuote instanceof XmlToken) || ((XmlToken)leftQuote).getTokenType() != GspTokenTypes.GSP_ATTR_VALUE_START_DELIMITER) {
      return null;
    }

    PsiElement firstElement = leftQuote.getNextSibling();
    if (!(firstElement instanceof LeafPsiElement)) return null;
    IElementType firstElementType = ((LeafPsiElement)firstElement).getElementType();

    if (firstElementType == GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER) return null;
    if (firstElementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || firstElementType == GspTokenTypes.GSTRING_DOLLAR) {
      return PsiType.getJavaLangString(value.getManager(), value.getResolveScope());
    }

    if (firstElement instanceof GspOuterGroovyElement) {
      PsiElement valueEndDelimiter = firstElement.getNextSibling();
      if (valueEndDelimiter instanceof XmlToken && ((XmlToken)valueEndDelimiter).getTokenType() ==
                                                   XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
        return getTypeByOuterElement((GspOuterGroovyElement)firstElement);
      }

      return PsiType.getJavaLangString(value.getManager(), value.getResolveScope());
    }

    if (firstElementType == GspTokenTypes.GEXPR_BEGIN) {
      PsiElement groovyCodeElement = firstElement.getNextSibling();
      if (!(groovyCodeElement instanceof GspOuterGroovyElement)) return null;
      PsiElement closeBracket = groovyCodeElement.getNextSibling();
      if (!PsiImplUtil.isLeafElementOfType(closeBracket, GspTokenTypes.GEXPR_END)) return null;
      assert closeBracket != null;

      PsiElement valueEndDelimiter = closeBracket.getNextSibling();
      if (valueEndDelimiter instanceof XmlToken && ((XmlToken)valueEndDelimiter).getTokenType() ==
                                                   XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
        return getTypeByOuterElement((GspOuterGroovyElement)groovyCodeElement);
      }

      return PsiType.getJavaLangString(value.getManager(), value.getResolveScope());
    }

    return null;
  }

  public static @Nullable PsiType getTypeByOuterElement(GspOuterGroovyElement outerElement) {
    PsiElement grInLeft = outerElement.getContainingFile().getViewProvider().findElementAt(outerElement.getTextOffset() - 1,
                                                                                           GroovyLanguage.INSTANCE);
    if (!(grInLeft instanceof LeafPsiElement)) return null;

    IElementType elementType = ((LeafPsiElement)grInLeft).getElementType();

    if (elementType == GspTokenTypes.GSTRING_DOLLAR) return PsiType.getJavaLangString(grInLeft.getManager(), grInLeft.getResolveScope());

    if (!(grInLeft instanceof OuterLanguageElement) && elementType != GspTokenTypes.GEXPR_BEGIN) return null;

    PsiElement grIn = grInLeft.getNextSibling();
    if (!(grIn instanceof GrGspExprInjection)) return null;

    GrExpression expression = ((GrGspExprInjection)grIn).getExpression();
    if (expression == null) return null;

    if (expression instanceof GrConditionalExpression cExpr) {
      if (cExpr.getThenBranch() == null && cExpr.getElseBranch() == null) {
        return cExpr.getCondition().getType(); // For case '<g:each in="${collection?}" >' see IDEA-66507
      }
    }

    return expression.getType();
  }

  public static @NotNull PsiType getElementTypeByCollectionType(@NotNull PsiType psiType, Project project, @NotNull GlobalSearchScope scope) {
    if (psiType instanceof PsiArrayType) {
      return ((PsiArrayType)psiType).getComponentType();
    }

    PsiType res = PsiUtil.extractIterableTypeParameter(psiType, true);
    if (res != null) return res;

    if (!(psiType instanceof PsiClassType)) return psiType;

    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    PsiElementFactory factory = facade.getElementFactory();

    PsiType typeMap = factory.createTypeByFQClassName(CommonClassNames.JAVA_UTIL_MAP, scope);

    if (typeMap.isAssignableFrom(psiType)) {
      return factory.createTypeByFQClassName(CommonClassNames.JAVA_UTIL_MAP_ENTRY, scope);
    }

    return psiType;
  }

  public static void removeValuesFromList(Collection<String> c, GrListOrMap listOrMap) {
    for (GrExpression expression : listOrMap.getInitializers()) {
      if (expression instanceof GrLiteralImpl) {
        Object value = ((GrLiteralImpl)expression).getValue();
        if (value instanceof String sValue) {
          c.remove(sValue);
        }
      }
    }
  }
}

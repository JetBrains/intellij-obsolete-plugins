/*
 * Copyright 2014 The authors
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

package com.intellij.lang.ognl.psi;

import com.intellij.lang.ognl.OgnlLanguage;
import org.intellij.lang.annotations.Language;

/**
 * {@link OgnlNewExpression}.
 *
 * @author Yann C&eacute;bron
 */
public class NewExpressionPsiTest extends PsiTestCase {

  public void testClassnameWithNoParameters() {
    assertConstructorExpression("new Something()", "Something");
  }

  public void testQualifiedClassnameWithNoParameters() {
    final OgnlNewExpression newExpression = assertConstructorExpression("new java.util.ArrayList()", "java.util.ArrayList");

    assertNotNull(newExpression.getParameterList());
  }

  public void testClassnameWithOneParameter() {
    final OgnlNewExpression newExpression = assertConstructorExpression("new Integer(1)", "Integer");

    final OgnlParameterList parameterList = newExpression.getParameterList();
    assertNotNull(parameterList);
    assertSize(1, parameterList.getParametersList());
    assertEquals(1, parameterList.getParameterCount());
  }

  public void testClassnameWithMultipleParameters() {
    assertConstructorExpression("new Something(1, 2)", "Something");
  }

  private OgnlNewExpression assertConstructorExpression(
    @Language(value = OgnlLanguage.ID,
      prefix = OgnlLanguage.EXPRESSION_PREFIX,
      suffix = OgnlLanguage.EXPRESSION_SUFFIX)
    final String expression,
    final String objectTypeText) {
    final OgnlNewExpression newExpression = parse(expression);

    final OgnlExpression objectTypeExpression = newExpression.getObjectType();
    assertNotNull(objectTypeExpression);
    assertEquals(objectTypeText, objectTypeExpression.getText());

    return newExpression;
  }

  private OgnlNewExpression parse(@Language(value = OgnlLanguage.ID,
    prefix = OgnlLanguage.EXPRESSION_PREFIX,
    suffix = OgnlLanguage.EXPRESSION_SUFFIX) final String expression) {
    return (OgnlNewExpression)parseSingleExpression(expression);
  }
}
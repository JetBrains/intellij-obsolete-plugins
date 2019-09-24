/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.plugins.ruby.motion;

import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiReference;
import junit.framework.TestCase;
import org.jetbrains.plugins.ruby.motion.symbols.FunctionSymbol;
import org.jetbrains.plugins.ruby.motion.symbols.MotionClassSymbol;
import org.jetbrains.plugins.ruby.motion.symbols.MotionSymbolUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolFilterFactory;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RSymbolType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType;
import org.jetbrains.plugins.ruby.ruby.testCases.RubyTestUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class RubyMotionCodeInsightTest extends RubyMotionLightFixtureTestCase {
  @Override
  protected String getTestDataRelativePath() {
    return "testApp";
  }

  public void testClassNameResolve() {
    defaultConfigure();
    checkResolveToObjC("UI<caret>View", "UIView");
  }

  public void testFunctionResolve() {
    defaultConfigure();
    checkResolveToObjC("CGAffineTransformFrom<caret>String", "CGAffineTransformFromString");
  }

  public void testSelectorResolve() {
    defaultConfigure();
    checkResolveToObjC("UIView.al<caret>loc", "NSObject.alloc");
  }

  public void testSelectorChainResolve() {
    defaultConfigure();
    checkResolveToObjC("UIView.alloc.initWith<caret>Frame", "UIView.initWithFrame");
  }

  public void testFunctionType() {
    defaultConfigure();
    assertTypeBirthClass(findReferenceBySignature("tra<caret>nsform"), "CGAffineTransform");
  }

  public void testStructType() {
    defaultConfigure();
    final RType type = getExpressionType("tra<caret>nsform");
    assertInstanceOf(type, RSymbolType.class);
    assertNotNull(type.getMemberForName("tx", SymbolFilterFactory.EMPTY_FILTER, null));
    assertNotNull(type.getMemberForName("tx=", SymbolFilterFactory.EMPTY_FILTER, null));
  }

  public void testSelectorType() {
    defaultConfigure();

    assertTypeBirthClass(findReferenceBySignature("UIView.al<caret>loc"), "UIView");
  }

  public void testSelectorChainType() {
    defaultConfigure();

    assertTypeBirthClass(findReferenceBySignature("UIView.alloc.initWith<caret>Frame"), "UIView");
  }

  public void testCompletion() throws Throwable {
    defaultConfigure();
    assertInCompletionList("<caret>UIView", "animationDidStart", "animationDidStop:finished:");
  }

  public void testStricterCompletion() {
    List<String> variants = myFixture.getCompletionVariants("app/app_delegate_completion.rb");
    assertContainsElements(variants, "KCFDateFormatterVeryShortMonthSymbols", "KCFGregorianUnitsHours");
    // "UIBarButtonItem", "CFNetServiceClientContext"  // filtered out by too many elements
  }

  public void testRakefileCompletion() throws Throwable {
    myFixture.configureByFiles("Rakefile", "app/app_delegate.rb");
    assertInCompletionList("app.<caret>name", "status_bar_style=", "codesign_certificate");
  }

  public void testContextCompletion() throws Throwable {
    myFixture.configureByFiles("app/context.rb", "Rakefile");
    assertNotInCompletionList(").<caret>test", "imageNamed");
  }

  public void testParameterType() {
    myFixture.configureByFiles("app/inheritance_arg.rb", "Rakefile");
    checkResolveToObjC("dequeueReusable<caret>CellWithIdentifier", "UITableView.dequeueReusableCellWithIdentifier");
  }

  public void testSelectorShorthands() {
    final List<String> names = new ArrayList<>();
    final List<Trinity<String, String, Integer>> examples = new ArrayList<>();
    examples.add(Trinity.create("foo?", "isFoo", 0));
    examples.add(Trinity.create("foo=", "setFoo:", 1));
    examples.add(Trinity.create("[]", "objectForKey:", 1));
    examples.add(Trinity.create("[]=", "setObject:forKey:", 2));
    for (Trinity<String, String, Integer> data : examples) {
      names.clear();
      MotionSymbolUtil.getSelectorNames(names, data.second, data.third);
      assertContainsElements(names, data.first);
    }
  }

  protected void checkResolveToObjC(final String signature, final String fqn) {
    final PsiReference ref = findReferenceBySignature(signature);
    final Symbol symbol = RubyTestUtil.resolveToSymbol(ref);
    TestCase.assertTrue(String.valueOf(symbol), symbol instanceof MotionClassSymbol || symbol instanceof FunctionSymbol);
    TestCase.assertEquals(fqn, symbol.getFQNWithNesting().getFullPath());
  }
}

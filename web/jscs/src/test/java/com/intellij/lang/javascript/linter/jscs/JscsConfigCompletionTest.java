package com.intellij.lang.javascript.linter.jscs;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.javascript.linter.jscs.config.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.*;

/**
 * @author Irina.Chernushina on 10/9/2014.
 */
public class JscsConfigCompletionTest extends BasePlatformTestCase {

  @Override
  protected String getTestDataPath() {
    return getBasePath();
  }

  protected String getBasePath() {
    return "src/test/testData/config/completion";
  }

  private void doTest(String nameSuffix, String ext) {
    String[] files = {getTestName(false) + nameSuffix + "." + ext};
    myFixture.configureByFiles(files);
    myFixture.completeBasic();
  }

  public void testHighLevelPropertyName() {
    doTest("", "jscsrc");
    final List<String> variants = new ArrayList<>();
    for (JscsOption option : JscsOption.values()) {
      variants.add(StringUtil.wrapWithDoubleQuote(option.name()));
    }
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  public void testHighLevelPropertyName2() {
    doTest("", "jscsrc");
    final List<String> variants = new ArrayList<>();
    for (JscsOption option : JscsOption.values()) {
      variants.add(option.name());
    }
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  public void testHighLevelPropertyStringValue() {
    doTest("", "jscsrc");
    final List<String> variants = new ArrayList<>();
    final JscsOptionDescriptor.ValuesCollection description =
      (JscsOptionDescriptor.ValuesCollection) JscsOption.preset.getDescriptor().getTypes().get(ValueType.str);
    for (String s : description.getArrValues()) {
      variants.add(StringUtil.wrapWithDoubleQuote(s));
    }
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  public void testHighLevelPropertyArrayValue() {
    doTest("", "jscsrc");
    final List<String> variants = new ArrayList<>();
    final JscsOptionDescriptor.ValuesCollection description =
      (JscsOptionDescriptor.ValuesCollection) JscsOption.requireCurlyBraces.getDescriptor().getTypes().get(ValueType.array);
    Collections.addAll(variants, description.getArrValues());
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  public void testInnerPropertyName() {
    doTest("", "jscs.json");
    final JscsOptionDescriptor.ValuesObject description =
      (JscsOptionDescriptor.ValuesObject) JscsOption.maximumLineLength.getDescriptor().getTypes().get(ValueType.obj);
    final List<String> variants = new ArrayList<>(description.getFields().keySet());
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  public void testInnerJSDocCompletion() {
    doTest("", "jscs.json");
    final JscsOptionDescriptor.ValuesObject description =
      (JscsOptionDescriptor.ValuesObject) JscsOption.jsDoc.getDescriptor().getTypes().get(ValueType.obj);
    final Set<String> fields =
      ((JscsOptionDescriptor.ValuesObject)description.getFields().get("checkAnnotations").getTypes().get(ValueType.obj)).getFields().keySet();
    final List<String> variants = new ArrayList<>();
    for (String field : fields) {
      variants.add("\"" + field + "\"");
    }
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  public void testInnerJSDocCompletionValue() {
    doTest("", "jscs.json");
    final JscsOptionDescriptor.ValuesObject description =
      (JscsOptionDescriptor.ValuesObject) JscsOption.jsDoc.getDescriptor().getTypes().get(ValueType.obj);
    final JscsOptionDescriptor presetDescriptor =
      ((JscsOptionDescriptor.ValuesObject)description.getFields().get("checkAnnotations").getTypes().get(ValueType.obj)).getFields().get("preset");
    final JscsOptionDescriptor.ValueDescription valueDescription = presetDescriptor.getTypes().get(ValueType.str);
    Assert.assertTrue(valueDescription instanceof JscsOptionDescriptor.ValuesCollection);
    final List<String> variants = new ArrayList<>();
    for (String s : ((JscsOptionDescriptor.ValuesCollection)valueDescription).getArrValues()) {
      variants.add("\"" + s + "\"");
    }
    Collections.sort(variants);
    assertCompletionVariants(variants);
  }

  private void assertCompletionVariants(@NotNull List<String> expectedVariants) {
    String[] expectedVariantsArray = ArrayUtilRt.toStringArray(expectedVariants);
    boolean[] status = completionContains(myFixture.getLookupElements(), expectedVariantsArray);
    for (int i = 0; i < status.length; i++) {
      assertTrue("Completion variant '" + expectedVariantsArray[i] + "' must exist.", status[i]);
    }
    assertEquals(expectedVariantsArray.length, myFixture.getLookupElements().length);
  }

  protected static boolean[] completionContains(LookupElement[] items, String[] variants) {
    boolean [] status = new boolean[variants.length];
    List<String> variantsToCheck = new ArrayList<>(Arrays.asList(variants));

    for(LookupElement e:items) {
      for(String v:variantsToCheck) {
        if (e.getLookupString().equals(v)) {
          variantsToCheck.remove(v);
          for(int i = 0; i < variants.length; ++i) {
            if (variants[i].equals(v)) {
              status[i]= true;
              break;
            }
          }
          break;
        }
      }
    }
    return status;
  }
}

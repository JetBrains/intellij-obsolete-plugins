package com.intellij.lang.javascript.linter.jscs;

import com.intellij.lang.javascript.linter.jscs.config.JscsDocumentationReader;
import com.intellij.lang.javascript.linter.jscs.config.JscsOption;
import com.intellij.openapi.util.text.StringUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Irina.Chernushina on 10/14/2014.
 */
public class JscsDocumentationTest {
  @Test
  public void testAllRulesParsed() {
    final JscsDocumentationReader reader = JscsDocumentationReader.getInstance();

    for (JscsOption option : JscsOption.values()) {
      Assert.assertNotNull(option.name(), reader.getDescription(option));
    }
  }

  @Test
  public void testAllRulesHaveShortDescription() {
    for (JscsOption option : JscsOption.values()) {
      Assert.assertTrue(option.name(), ! StringUtil.isEmptyOrSpaces(option.getDescription()));
    }
  }
}

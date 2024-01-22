package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.util.xml.DomNameStrategy;
import com.intellij.util.xml.JavaNameStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public final class AsIsNameStrategy extends DomNameStrategy {
  /**
   * @param propertyName property name, i.e. method name without first 'get', 'set' or 'is'
   * @return XML element name
   */
  @NotNull
  @Override
  public String convertName(@NotNull String propertyName) {
    return propertyName;
  }

  /**
   * Is used to get presentable DOM elements in UI
   *
   * @param xmlElementName XML element name
   * @return Presentable DOM element name
   */
  @Override
  public String splitIntoWords(String xmlElementName) {
    final String[] strings = NameUtil.nameToWords(xmlElementName);
    ArrayList<String> lst = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    for (String s : strings) {
      if (s.length() == 1) {
        sb.append(s);
      }
      else {
        //flush
        if (sb.length() > 0) {
          lst.add(sb.toString());
          sb.setLength(0);
        }
        lst.add(s);
      }
    }
    //flush
    if (sb.length() > 0) {
      lst.add(sb.toString());
      sb.setLength(0);
    }
    return StringUtil.join(lst, JavaNameStrategy.DECAPITALIZE_FUNCTION, " ");
  }
}

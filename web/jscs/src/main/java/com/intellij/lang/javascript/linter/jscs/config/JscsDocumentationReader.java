package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.lang.javascript.linter.jscs.JscsConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.xmlb.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Irina.Chernushina on 10/14/2014.
 */
public class JscsDocumentationReader {
  private static final Logger LOG = Logger.getInstance(JscsConfiguration.LOG_CATEGORY);

  private final Map<JscsOption, String> myDocMap;
  private final Map<String, String> myInnerOptionsData;

  private JscsDocumentationReader(@NotNull final Map<JscsOption, String> map, Map<String, String> data) {
    myDocMap = map;
    myInnerOptionsData = data;
  }

  public static JscsDocumentationReader getInstance() {
    return Holder.INSTANCE;
  }

  @Nullable
  public String getDescription(@NotNull final JscsOption option) {
    return myDocMap.get(option);
  }

  @Nullable
  public String getInnerDescription(@NotNull final String name) {
    return myInnerOptionsData.get(name);
  }

  @NotNull
  private static JscsDocumentationReader parseFromXml() {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final String content = loadXmlContent();
      final InputSource source = new InputSource(new StringReader(content));
      final Document document = db.parse(source);
      return fromDocument(document);
    } catch (Exception e) {
      LOG.error("Can't parse jscs documentation :(", e);
      return DEFAULT;
    }
  }

  private static JscsDocumentationReader fromDocument(Document document) {
    final NodeList options = document.getChildNodes();
    if (options.getLength() != 1 || ! "options".equals(options.item(0).getNodeName())) return DEFAULT;
    final Node optionsElement = options.item(0);

    final NodeList optionsList = optionsElement.getChildNodes();
    final Map<JscsOption, String> map = new HashMap<>();
    final Map<String, String> inner = new HashMap<>();

    for (int i = 0; i < optionsList.getLength(); i ++) {
      final Node item = optionsList.item(i);
      final Element childElement = ObjectUtils.tryCast(item, Element.class);
      if (childElement == null || ! Constants.OPTION.equals(childElement.getTagName())) continue;

      final String key = childElement.getAttribute("key");
      if (key == null) continue;

      final JscsOption option = JscsOption.safeValueOf(key);

      final NodeList dList = childElement.getElementsByTagName("description");
      if (dList.getLength() == 1) {
        final Node child = dList.item(0);
        final Element descElement = ObjectUtils.tryCast(child, Element.class);
        if (descElement != null) {
          if (option != null) {
            map.put(option, descElement.getTextContent());
          } else {
            inner.put(key, descElement.getTextContent());
          }
        }
      }
    }
    return map.isEmpty() ? DEFAULT : new JscsDocumentationReader(map, inner);
  }

  @NotNull
  private static String loadXmlContent() throws IOException {
    InputStream in = JscsDocumentationReader.class.getResourceAsStream("jscs-documentation.xml");
    Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
    return FileUtil.loadTextAndClose(reader);
  }

  private final static JscsDocumentationReader DEFAULT = new JscsDocumentationReader(Collections.emptyMap(),
                                                                                     Collections.emptyMap());
  private static class Holder {
    private final static JscsDocumentationReader INSTANCE = parseFromXml();
  }
}

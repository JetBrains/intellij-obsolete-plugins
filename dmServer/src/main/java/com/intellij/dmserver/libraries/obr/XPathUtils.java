package com.intellij.dmserver.libraries.obr;

import org.jetbrains.annotations.NonNls;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.regex.Pattern;

public final class XPathUtils {

  @NonNls
  private static final Pattern JS_PATTERN = Pattern.compile("<script.*?>.*?</script>", Pattern.DOTALL);

  private static final XPathUtils ourInstance = new XPathUtils();

  public static XPathUtils getInstance() {
    return ourInstance;
  }

  private final XPath myXPath = XPathFactory.newInstance().newXPath();

  private XPathUtils() {

  }

  public XPath getXPath() {
    return myXPath;
  }

  public Object evaluateXPath(@NonNls String xPath, Object item, QName returnType) throws XPathExpressionException {
    return getXPath().evaluate(xPath, item, returnType);
  }

  public String evaluateXPath(@NonNls String xPath, Object item) throws XPathExpressionException {
    return getXPath().evaluate(xPath, item);
  }

  public Node createHtmlRoot(@NonNls String source) throws XPathExpressionException {
    //		myXPath.setNamespaceContext(new NamespaceContext() {
    //
    //			private final Collection<String> myPrefixes = Collections.singletonList("html");
    //
    //			@Override
    //			public String getNamespaceURI(String prefix) {
    //				return "http://www.w3.org/1999/xhtml";
    //			}
    //
    //			@Override
    //			public String getPrefix(String namespaceURI) {
    //				return "html";
    //			}
    //
    //			@Override
    //			public Iterator<?> getPrefixes(String namespaceURI) {
    //				return myPrefixes.iterator();
    //			}
    //
    //		});

    source =
      source.replace("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">", //
                     "");
    //      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"temp/xhtml1-strict.dtd\">");
    source = source.replace("&nbsp;", " ");
    source = source.replace("&copy;", " ");
    source = JS_PATTERN.matcher(source).replaceAll("");
    source = source.replace("<html xmlns=\"http://www.w3.org/1999/xhtml\">", "<html>");

    InputSource inputSource = new InputSource(new StringReader(source));
    @NonNls String html = "/html";
    return (Node)getXPath().evaluate(html, inputSource, XPathConstants.NODE);
  }

}

package css;

import com.intellij.psi.PsiReference;
import com.intellij.psi.css.CssDeclaration;
import com.intellij.psi.css.inspections.CssInvalidElementInspection;
import com.intellij.psi.css.inspections.CssUnknownPropertyInspection;
import com.intellij.psi.css.inspections.invalid.CssUnknownTargetInspection;
import com.intellij.psi.css.resolve.CssResolveManager;
import com.intellij.psi.css.resolve.CssResolver;
import com.intellij.psi.css.resolve.HtmlCssClassOrIdReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsTest;
import com.intellij.util.containers.ContainerUtil;

import java.io.IOException;

public class CssInStrutsProjectResolveAndHighlightingTest extends StrutsTest {
  private static CssResolver getResolver() {
    final CssResolver resolver = CssResolveManager.getInstance().getNewResolver();
    resolver.setUseAgentStylesheet(false);
    return resolver;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new CssUnknownTargetInspection(),
                                new CssInvalidElementInspection(),
                                new CssUnknownPropertyInspection());
  }

  public void testStrutsResolve() throws IOException {
    doStrutsTestWithGivenTldName("StrutsResolve","struts_html.tld");
  }

  public void testStrutsResolve2() throws IOException {
    doStrutsTestWithGivenTldName("StrutsResolve","struts_html2.tld");
  }

  public void testStrutsResolve3() throws IOException {
    doStrutsTestWithGivenTldName("StrutsResolve","struts_html3.tld");
  }

  public void testStrutsResolve4() throws IOException {
    doStrutsTestWithGivenTldName("StrutsResolve","struts_html4.tld");
  }

  private void doStrutsTestWithGivenTldName(String testName, final String name) throws IOException {
    myFixture.configureByFiles(testName + ".jsp", testName + ".css", "WEB-INF/" + name);
    myFixture.checkHighlighting(true, false, false);

    XmlTag tag = getTagAtCaret();
    final CssDeclaration[] cssDeclarations = getResolver().resolve(tag);

    // do not pick up the style content from jsp include
    assertEquals( "css style ref in jsp", 3, cssDeclarations.length);

    for(String attrName : new String[] {"styleClass", "styleId"}) {
      final XmlAttribute attribute = tag.getAttribute(attrName, null);
      assertNotNull(attribute);

      final XmlAttributeValue valueElement = attribute.getValueElement();
      final PsiReference[] references = valueElement.getReferences();
      final HtmlCssClassOrIdReference ref = ContainerUtil.findInstance(references, HtmlCssClassOrIdReference.class);
      assertNotNull(ref);
      assertTrue( ref.multiResolve(false).length > 0 );
    }

  }

  private XmlTag getTagAtCaret() {
    return PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset()), XmlTag.class);
  }

  @Override
  protected String getBasePath() {
    return "/css/";
  }
}

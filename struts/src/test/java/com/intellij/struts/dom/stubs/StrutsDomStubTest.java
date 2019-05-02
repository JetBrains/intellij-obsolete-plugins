package com.intellij.struts.dom.stubs;

import com.intellij.struts.StrutsTest;
import com.intellij.struts.dom.DomStubTest;

import java.io.File;

/**
 * @author Yann C&eacute;bron
 */
public class StrutsDomStubTest extends DomStubTest {

  public void testStrutsConfigXml() {
    doBuilderTest("struts-config.xml",
                  "File:struts-config\n" +
                  "  Element:struts-config\n" +
                  "    Element:form-beans\n" +
                  "      Element:form-bean\n" +
                  "        Attribute:name:wrongType\n" +
                  "        Attribute:type:x\n" +
                  "      Element:form-bean\n" +
                  "        Attribute:name:dynaBean\n" +
                  "        Attribute:type:org.apache.struts.action.DynaActionForm\n" +
                  "        Element:form-property\n" +
                  "          Attribute:name:formProperty\n" +
                  "          Attribute:type:formType\n" +
                  "    Element:global-forwards\n" +
                  "      Element:forward\n" +
                  "        Attribute:name:servletForward\n" +
                  "        Attribute:path:/mapping\n" +
                  "    Element:action-mappings\n" +
                  "      Element:action\n" +
                  "        Attribute:path:/login\n" +
                  "        Attribute:type:x\n" +
                  "        Attribute:name:loginForm\n" +
                  "        Attribute:parameter:parameter\n" +
                  "        Element:forward\n" +
                  "          Attribute:name:forwardName\n" +
                  "          Attribute:path:forwardPath\n" +
                  "    Element:controller\n" +
                  "      Attribute:inputForward:true\n" +
                  "      Element:set-property\n" +
                  "        Attribute:property:myProperty\n" +
                  "        Attribute:value:myValue\n" +
                  "    Element:plug-in\n" +
                  "      Attribute:className:org.apache.struts.tiles.TilesPlugin\n" +
                  "      Element:set-property\n" +
                  "        Attribute:property:definitions-config\n" +
                  "        Attribute:value:/WEB-INF/tiles-defs.xml\n");
  }

  @Override
  protected String getTestDataPath() {
    return new File(StrutsTest.getTestDataRoot(), "stubs/struts").getPath();
  }
}

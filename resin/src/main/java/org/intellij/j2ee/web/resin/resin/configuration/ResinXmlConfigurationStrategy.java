package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.j2ee.web.resin.ResinModel;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class ResinXmlConfigurationStrategy extends Resin31ConfigurationStrategy {


  private static final Logger LOG = Logger.getInstance(ResinXmlConfigurationStrategy.class);

  @NonNls
  public static final String IMPORT_SINGLE_PATH_ATTRIBUTE = "path";
  @NonNls
  private static final String[] IMPORT_ATTRIBUTE_NAMES = new String[]{IMPORT_SINGLE_PATH_ATTRIBUTE, "fileset"};
  @NonNls
  private static final String CONF_FOLDER_VAR = "${__DIR__}";

  @NonNls
  protected static final String RESIN_CONF = "resin32.xml";

  private List<ResinConfigImport> myImports;

  ResinXmlConfigurationStrategy(ResinInstallation resinInstallation) {
    super(resinInstallation);
  }

  @Override
  public void init(ResinModel serverModel, Element element) throws ExecutionException {
    super.init(serverModel, element);
    myImports = new ArrayList<>();
    resolveImports(element, myImports);
  }

  @Override
  public InputStream getDefaultResinConfContent() {
    return this.getClass().getResourceAsStream(RESIN_CONF);
  }

  /**
   * This method will resolve every import turning ${__DIR__} into &lt;resin_home&gt;/conf
   */
  protected void resolveImports(Element rootElement, @Nullable List<ResinConfigImport> imports) {
    try {
      File confFolder = new File(getInstallation().getResinHome(), "conf");
      String confFolderPath = FileUtil.toSystemIndependentName(confFolder.getAbsolutePath());

      for (String importAttrName : IMPORT_ATTRIBUTE_NAMES) {
        XPath xpath = XPath.newInstance(".//*[name()='resin:import' or name()='resin:properties'][@" + importAttrName + "]");
        List<Element> elements = (List<Element>)xpath.selectNodes(rootElement);

        for (Element element : elements) {
          String path = element.getAttributeValue(importAttrName);
          element.setAttribute(importAttrName, StringUtil.replace(path, CONF_FOLDER_VAR, confFolderPath));
          if (imports != null && StringUtil.equals(IMPORT_SINGLE_PATH_ATTRIBUTE, importAttrName)) {
            imports.add(new ResinConfigImport(element));
          }
        }
      }
    }
    catch (JDOMException ex) {
      LOG.info(ex);
    }
  }

  protected final List<ResinConfigImport> getImports() {
    return myImports;
  }

  @Override
  public void save() throws ExecutionException {
    for (ResinConfigImport configImport : getImports()) {
      configImport.save();
    }
  }
}

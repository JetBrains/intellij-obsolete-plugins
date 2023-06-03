package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.NullableLazyValue;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;

public class ResinConfigImport {

  private static final Logger LOG = Logger.getInstance(ResinConfigImport.class);

  private final Element myRoot;

  private final NullableLazyValue<Element> myImportDoc = new NullableLazyValue<>() {
    @Override
    protected Element compute() {
      String path = myRoot.getAttributeValue(ResinXmlConfigurationStrategy.IMPORT_SINGLE_PATH_ATTRIBUTE);
      try {
        return JDOMUtil.load(new File(path));
      }
      catch (JDOMException | IOException e) {
        LOG.debug(e);
        return null;
      }
    }
  };

  private ResinGeneratedConfig myCopy;

  private ExecutionException myCopyException;

  public ResinConfigImport(Element root) {
    myRoot = root;
  }

  public Element getImportDoc() {
    return myImportDoc.getValue();
  }

  public void copy() {
    if (myCopy == null && myCopyException == null) {
      try {
        myCopy = new ResinGeneratedConfig(getImportDoc(), "resin-import");
        myRoot.setAttribute(ResinXmlConfigurationStrategy.IMPORT_SINGLE_PATH_ATTRIBUTE, myCopy.getFile().getAbsolutePath());
      }
      catch (ExecutionException e) {
        myCopyException = e;
      }
    }
  }

  public void save() throws ExecutionException {
    if (myCopyException != null) {
      throw myCopyException;
    }
    if (myCopy != null) {
      myCopy.save();
    }
  }
}

package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class ResinGeneratedConfig {
  private final Element myElement;
  private final File myFile;

  public ResinGeneratedConfig(@NotNull Element element, @NonNls String prefix) throws ExecutionException {
    myElement = element;
    try {
      myFile = FileUtil.createTempFile(prefix, ".conf");
      myFile.deleteOnExit();
    }
    catch (IOException e) {
      throw new ExecutionException(ResinBundle.message("message.error.resin.conf.cant.create", e));
    }
  }

  public File getFile() {
    return myFile;
  }

  public void save() throws ExecutionException {
    try {
      JDOMUtil.write(myElement, myFile.toPath());
    }
    catch (IOException e) {
      throw new ExecutionException(ResinBundle.message("message.error.resin.conf.update"), e);
    }
  }
}

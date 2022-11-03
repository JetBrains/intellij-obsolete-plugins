package com.intellij.dmserver.install.impl;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class PropertiesUtil {

  public static Properties loadProperties(VirtualFile file) throws IOException {
    Properties result = new Properties();
    try (InputStream is = file.getInputStream()) {
      result.load(is);
      return result;
    }
    catch (IOException e) {
      throw new IOException(".properties file has unknown format", e);  // TODO: not the only possible cause
    }
  }

  public static void saveProperties(VirtualFile file, Properties properties) throws IOException {
    try (OutputStream os = file.getOutputStream(PropertiesUtil.class)) {
      properties.store(os, null);
    }
    catch (IOException e) {
      throw new IOException(".properties file has unknown format", e);  // TODO: not the correct msg
    }
  }
}

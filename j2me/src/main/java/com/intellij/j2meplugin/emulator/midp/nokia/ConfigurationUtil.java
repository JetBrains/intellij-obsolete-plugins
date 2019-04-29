/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.midp.nokia;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileFilters;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Properties;

public class ConfigurationUtil {
  @Nullable
  private static Element getEmulatorProperties(String homeDir) {
    final File home = new File(homeDir);
    if (!home.exists() || !home.isDirectory()) return null;
    final FileFilter xmlFilter = FileFilters.filesWithExtension("xml");
    File[] xml = home.listFiles(xmlFilter);
    if (xml == null) return null;
    if (xml.length != 1) {
      xml = home.listFiles(pathname -> {
        if (xmlFilter.accept(pathname)) {
          final String homeName = home.getName();
          final int _index = homeName.indexOf('_');
          if (_index > 0 && pathname.getName().startsWith(homeName.substring(0, _index))) {
            return true;
          }
        }
        return false;
      });
    }
    if (xml == null || xml.length != 1) return null; //can't choose corresponding file
    try {
      return JDOMUtil.load(new BufferedInputStream(new FileInputStream(xml[0])));
    }
    catch (IOException ignored) {
      return null;
    }
    catch (JDOMException ignored) {
      return null;
    }
  }

  @Nullable
  @SuppressWarnings({"HardCodedStringLiteral"})
  public static String getPreferences(String homeDir) {
    Element pref = getByNameGroup(homeDir, "preferences");
    if (pref == null) return null;
    for (Element property : pref.getChildren("property")) {
      final String name = property.getAttributeValue("name");
      if (Comparing.equal(name, "prefs")) {
        String preferencePath = property.getAttributeValue("value");
        if (preferencePath == null) return null;
        final int separatorIndex = preferencePath.indexOf(File.separatorChar);
        return separatorIndex > -1 ? preferencePath.substring(separatorIndex) : null;
      }
    }
    return null;
  }

  @Nullable
  private static Element getByNameGroup(String homeDir, String groupName) {
    final Element emulatorProperties = getEmulatorProperties(homeDir);
    if (emulatorProperties == null) return null;
    for (Element group : emulatorProperties.getChildren("group")) {
      final String name = group.getAttributeValue("name");
      if (Comparing.equal(name, groupName)) {
        return group;
      }
    }
    return null;
  }

  @Nullable
  public static Properties getProperties(String homePath) {
    Properties properties = new Properties();
    final File home = new File(homePath);
    if (!home.exists() || !home.isDirectory()) return null;
    final FileFilter propertiesFilter = FileFilters.filesWithExtension("properties");
    File[] props = home.listFiles(propertiesFilter);
    if (props == null) return properties;
    if (props.length != 1) {
      props = home.listFiles(pathname -> pathname.getName().equals(home.getName() + ".properties"));
    }
    if (props == null || props.length != 1) return properties;
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(props[0]));
      properties.load(is);
      is.close();
      return properties;
    }
    catch (IOException ignored) {
      return null;
    }
  }

}

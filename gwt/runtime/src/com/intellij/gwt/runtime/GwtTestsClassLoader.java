package com.intellij.gwt.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GwtTestsClassLoader extends ClassLoader {
  private final Set myIgnoredUrls;

  public GwtTestsClassLoader(ClassLoader parent) {
    super(parent);
    String ignoredUrlsProperty = System.getProperty("idea.gwt.ignored.resource.urls");
    if (ignoredUrlsProperty == null || ignoredUrlsProperty.isEmpty()) {
      myIgnoredUrls = Collections.EMPTY_SET;
    }
    else {
      myIgnoredUrls = new HashSet();
      String[] ignoredUrls = ignoredUrlsProperty.split("\n");
      for (int i = 0, len = ignoredUrls.length; i < len; i++) {
        myIgnoredUrls.add(ignoredUrls[i]);
      }
    }
  }

  @Override
  public Enumeration getResources(String name) throws IOException {
    final Enumeration resources = super.getResources(name);
    if ("META-INF/jdoconfig.xml".equals(name)) {
      //to fix the problem with duplicated jdoconfig.xml in classpath (from sources and from output)
      final List list = Collections.list(resources);
      if (list.size() > 1) {
        final Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
          final URL url = (URL)iterator.next();
          if (myIgnoredUrls.contains(url.toString())) {
            iterator.remove();
          }
        }
        return Collections.enumeration(list);
      }
    }
    return resources;
  }
}

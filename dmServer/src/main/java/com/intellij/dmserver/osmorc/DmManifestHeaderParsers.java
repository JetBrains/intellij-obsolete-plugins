package com.intellij.dmserver.osmorc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.header.HeaderParser;
import org.jetbrains.lang.manifest.header.HeaderParserProvider;
import org.osmorc.manifest.lang.header.OsgiHeaderParser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author michael.golubev
 */
public class DmManifestHeaderParsers implements HeaderParserProvider {
  private final Map<String, HeaderParser> myParsers;

  public DmManifestHeaderParsers() {
    myParsers = new HashMap<>();
    myParsers.put("Application-Name", OsgiHeaderParser.INSTANCE);
    myParsers.put("Application-Description", OsgiHeaderParser.INSTANCE);
    myParsers.put("Application-Version", OsgiHeaderParser.INSTANCE);
    myParsers.put("Application-SymbolicName", OsgiHeaderParser.INSTANCE);
    myParsers.put("Application-TraceLevels", OsgiHeaderParser.INSTANCE);
    myParsers.put("Import-Bundle", OsgiHeaderParser.INSTANCE);
    myParsers.put("Import-Library", OsgiHeaderParser.INSTANCE);
    myParsers.put("Library-SymbolicName", OsgiHeaderParser.INSTANCE);
    myParsers.put("Library-Version", OsgiHeaderParser.INSTANCE);
    myParsers.put("Library-Name", OsgiHeaderParser.INSTANCE);
    myParsers.put("Library-Description", OsgiHeaderParser.INSTANCE);
    myParsers.put("Module-Type", OsgiHeaderParser.INSTANCE);
    myParsers.put("Spring-Context", OsgiHeaderParser.INSTANCE);
    myParsers.put("SpringExtender-Version", OsgiHeaderParser.INSTANCE);
    myParsers.put("Web-ContextPath", OsgiHeaderParser.INSTANCE);
    myParsers.put("Web-DispatcherServletUrlPatterns", OsgiHeaderParser.INSTANCE);
  }

  @NotNull
  @Override
  public Map<String, HeaderParser> getHeaderParsers() {
    return myParsers;
  }
}

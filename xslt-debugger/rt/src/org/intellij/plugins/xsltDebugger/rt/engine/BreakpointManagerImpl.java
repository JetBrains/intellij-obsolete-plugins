// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.rt.engine;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BreakpointManagerImpl implements BreakpointManager {
  private final Map<Integer, Map<String, Breakpoint>> myBreakpoints = new HashMap<>();

  @Override
  public Breakpoint setBreakpoint(File file, int line) {
    return setBreakpoint(file.toURI().toASCIIString(), line);
  }

  @Override
  public synchronized void removeBreakpoint(String uri, int line) {
    final Map<String, Breakpoint> s = myBreakpoints.get(line);
    if (s != null) {
      s.remove(normalizeUri(uri));
    }
  }

  @Override
  public synchronized List<Breakpoint> getBreakpoints() {
    List<Breakpoint> breakpoints = new ArrayList<>();
    for (Map<String, Breakpoint> map : myBreakpoints.values()) {
      breakpoints.addAll(map.values());
    }
    return breakpoints;
  }

  @Override
  public void removeBreakpoint(Breakpoint bp) {
    removeBreakpoint(bp.getUri(), bp.getLine());
  }

  @Override
  public synchronized Breakpoint setBreakpoint(String uri, int line) {
    assert line > 0 : "No line number for breakpoint in file " + uri;

    uri = normalizeUri(uri);
    final Map<String, Breakpoint> s = myBreakpoints.get(line);
    final BreakpointImpl bp = new BreakpointImpl(uri, line);
    if (s == null) {
      final HashMap<String, Breakpoint> map = new HashMap<>();
      map.put(uri, bp);
      myBreakpoints.put(line, map);
    } else {
      s.put(uri, bp);
    }
    return bp;
  }

  private static String normalizeUri(String uri) {
    // hmm, this code sucks, but seems to be a good guess to ensure the same format of
    // strings (file:/C:/... vs, file:///C:/...) on both sides...
    try {
      try {
        uri = uri.replaceAll(" ", "%20");
        return new File(new URI(uri)).toURI().toASCIIString();
      } catch (IllegalArgumentException e) {
        return new URI(uri).normalize().toASCIIString();
      }
    } catch (URISyntaxException e) {
      System.err.println("Failed to parse <" + uri + ">: " + e);
      return uri;
    }
  }

  public synchronized boolean isBreakpoint(String uri, int lineNumber) {
    final Breakpoint breakpoint = getBreakpoint(uri, lineNumber);
    return breakpoint != null && breakpoint.isEnabled();
  }

  @Override
  public synchronized Breakpoint getBreakpoint(String uri, int lineNumber) {
    final Map<String, Breakpoint> s = myBreakpoints.get(lineNumber);
    if (s != null) {
      return s.get(normalizeUri(uri));
    }
    return null;
  }
}

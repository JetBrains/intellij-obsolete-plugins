// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public interface Value extends Serializable {
  interface Type extends Serializable {
    String getName();
  }

  enum XPathType implements Type {
    BOOLEAN, NUMBER, STRING, NODESET, OBJECT, UNKNOWN;

    @Override
    public String getName() {
      return name().toLowerCase(Locale.ENGLISH);
    }
  }

  final class ObjectType implements Type {
    private final String myName;

    public ObjectType(String name) {
      myName = name;
    }

    @Override
    public String getName() {
      return myName;
    }
  }

  Object getValue();

  Type getType();

  class NodeSet implements Serializable {
    public final String myStringValue;
    private final List<Node> myNodes;

    public NodeSet(String stringValue, List<Node> nodes) {
      myStringValue = stringValue;
      myNodes = nodes;
    }

    public List<Node> getNodes() {
      return myNodes;
    }

    @Override
    public String toString() {
      return myStringValue;
    }
  }

  class Node implements Serializable, Debugger.Locatable {
    public final String myURI;
    public final int myLineNumber;
    public final String myXPath;
    public final String myStringValue;

    public Node(String URI, int lineNumber, String XPath, String stringValue) {
      myURI = URI;
      myLineNumber = lineNumber;
      myXPath = XPath;
      myStringValue = stringValue;
    }

    @Override
    public String getURI() {
      return myURI;
    }

    @Override
    public int getLineNumber() {
      return myLineNumber;
    }

    @Override
    public String toString() {
      return "Node{" +
             "myURI='" + myURI + '\'' +
             ", myLineNumber=" + myLineNumber +
             ", myXPath='" + myXPath + '\'' +
             ", myStringValue='" + myStringValue + '\'' +
             '}';
    }
  }
}

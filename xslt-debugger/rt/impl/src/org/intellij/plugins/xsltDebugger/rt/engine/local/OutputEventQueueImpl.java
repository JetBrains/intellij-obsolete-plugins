// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.local;

import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;
import org.intellij.plugins.xsltDebugger.rt.engine.OutputEventQueue;

import java.util.ArrayList;
import java.util.List;

public class OutputEventQueueImpl implements OutputEventQueue {
  private final Debugger myDebugger;

  private final List<NodeEvent> myEvents = new ArrayList<>();

  private boolean myEnabled = true;

  public OutputEventQueueImpl(Debugger debugger) {
    myDebugger = debugger;
  }

  public void startDocument() {
    if (myEnabled) {
      myEvents.add(new NodeEvent(START_DOCUMENT, null, null));
    }
  }

  public void endDocument() {
    if (myEnabled) {
      myEvents.add(new NodeEvent(END_DOCUMENT, null, null));
    }
  }

  public void startElement(String prefix, String localName, String uri) {
    addEvent(new NodeEvent(OutputEventQueue.START_ELEMENT, new NodeEvent.QName(prefix, localName, uri), null));
  }

  public void attribute(String prefix, String localName, String uri, String value) {
    addEvent(new NodeEvent(ATTRIBUTE, new NodeEvent.QName(prefix, localName, uri), value));
  }

  public void endElement() {
    addEvent(new NodeEvent(END_ELEMENT, null, null));
  }

  public void characters(String s) {
    addEvent(new NodeEvent(CHARACTERS, null, s));
  }

  public void comment(String s) {
    addEvent(new NodeEvent(COMMENT, null, s));
  }

  public void pi(String target, String data) {
    addEvent(new NodeEvent(PI, new NodeEvent.QName(target), data));
  }

  public void trace(String text) {
    addEvent(new NodeEvent(TRACE_POINT, null, text));
  }

  @Override
  public void setEnabled(boolean b) {
    myEnabled = b;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  private void addEvent(NodeEvent event) {
    if (myEnabled) {
      final Debugger.StyleFrame frame = myDebugger.getCurrentFrame();
      if (frame != null) {
        event.setLocation(frame.getURI(), frame.getLineNumber());
      }
      myEvents.add(event);
    }
  }

  @Override
  public List<NodeEvent> getEvents() {
    try {
      return new ArrayList<>(myEvents);
    } finally {
      myEvents.clear();
    }
  }
}

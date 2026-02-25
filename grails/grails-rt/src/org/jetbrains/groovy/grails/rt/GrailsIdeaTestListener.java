// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.groovy.grails.rt;

import grails.build.GrailsBuildListener;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Maxim.Medvedev
 * @noinspection UseOfSystemOutOrSystemErr,UnusedDeclaration
 */
public class GrailsIdeaTestListener implements GrailsBuildListener {
  private final Map<String, Long> myProperties = new HashMap<>();
  private String myClassName;

  private PrintStream out;

  @Override
  public void receiveGrailsBuildEvent(String s, Object[] objects) {
    if (out == null) {
      out = System.out; // Save System.out. They change it during test running.
    }

    if ("TestCaseEnd".equals(s)) {
      out.println(objects[1]);
      testSuiteFinished((String)objects[0]);
    }

    else if ("TestCaseStart".equals(s)) {
      myClassName = (String)objects[0];
      testSuiteStarted(myClassName);
    }

    else if ("TestStart".equals(s)) {
      testStarted((String)objects[0]);
    }

    else if ("TestEnd".equals(s)) {
      testFinished((String)objects[0]);
    }

    else if ("TestFailure".equals(s)) {
      Object failure = objects[1];
      String message = failure instanceof Throwable ?
                       replaceEscapedSymbols(((Throwable)failure).getMessage()) :
                       String.valueOf(failure);
      String details = failure instanceof Throwable ? getStackTrace((Throwable)failure) : "none";
      String error = failure instanceof AssertionError || failure == null ? "error='true'" : "";
      out.println("\n##teamcity[testFailed name='" + replaceEscapedSymbols(((String)objects[0])) +
                  "' message='" + message +
                  "' details='" + details +
                  "' " + error + "]");
    }
  }

  private static String replaceEscapedSymbols(String s) {
    if (s == null) return null;
    return s.replaceAll("[\\|'\\[\\]]", "\\|$0").
      replaceAll("\n", "|n").
      replaceAll("\r", "|r");
  }

  static String getStackTrace(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    return replaceEscapedSymbols(writer.getBuffer().toString());
  }

  private void testFinished(String testName) {
    long duration = System.currentTimeMillis() - myProperties.get(testName).longValue();
    out.println("\n##teamcity[testFinished name='" + replaceEscapedSymbols(testName) + "' duration='" + duration + "']");
  }

  private void testStarted(String testName) {
    String testLocation = replaceEscapedSymbols(myClassName + '.' + testName);
    out.println("\n##teamcity[testStarted name='" +
                replaceEscapedSymbols(testName) +
                "' captureStandardOutput='false' locationHint='grails://methodName::" +
                testLocation +
                "']");
    myProperties.put(testName, Long.valueOf(System.currentTimeMillis()));
  }

  private void testSuiteStarted(String name) {
    out.println("\n##teamcity[testSuiteStarted name='" + name + "' locationHint='grails://className::" + name + "']");
    myProperties.put(name, Long.valueOf(System.currentTimeMillis()));
  }

  private void testSuiteFinished(String name) {
    long duration = System.currentTimeMillis() - myProperties.get(name).longValue();
    out.println("\n##teamcity[testSuiteFinished name='" + name + "' duration='" + duration + "']");
  }
}

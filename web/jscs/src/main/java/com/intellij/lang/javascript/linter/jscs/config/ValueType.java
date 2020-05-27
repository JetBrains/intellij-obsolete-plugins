package com.intellij.lang.javascript.linter.jscs.config;

/**
 * @author Irina.Chernushina on 4/30/2015.
 */
public enum ValueType {
  bool("boolean"),
  true_or_false("boolean"),
  obj("object"),
  str("string"),
  array("array"),
  num_int("integer");

  private final String myName;

  ValueType(final String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public String getNameOrFixedValue() {
    if (bool.equals(this)) return "true";
    return getName();
  }
}

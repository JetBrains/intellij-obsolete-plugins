// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for converting between different name types,
 * for example from class names -> property names and vice-versa. The
 * key aspect of this class is that it has no dependencies outside the JDK!
 */
public final class GrailsNameUtils {

  private static final String PROPERTY_SET_PREFIX = "set";

  private GrailsNameUtils() {
  }

  /**
   * Retrieves the name of a setter for the specified property name
   *
   * @param propertyName The property name
   * @return The setter equivalent
   */
  public static String getSetterName(String propertyName) {
    return PROPERTY_SET_PREFIX + StringUtil.toUpperCase(propertyName.substring(0, 1)) + propertyName.substring(1);
  }

  /**
   * Calculate the name for a getter method to retrieve the specified property
   *
   * @return The name for the getter method for this property, if it were to exist, i.e. getConstraints
   */
  public static String getGetterName(String propertyName) {
    return "get" + Character.toUpperCase(propertyName.charAt(0))
           + propertyName.substring(1);
  }

  /**
   * Returns the class name for the given logical name and trailing name. For example "person" and "Controller" would evaluate to "PersonController"
   *
   * @param logicalName  The logical name
   * @param trailingName The trailing name
   * @return The class name
   */
  public static String getClassName(String logicalName, String trailingName) {
    if (isBlank(logicalName)) {
      throw new IllegalArgumentException("Argument [logicalName] cannot be null or blank");
    }

    String className = StringUtil.toUpperCase(logicalName.substring(0, 1)) + logicalName.substring(1);
    if (trailingName != null) {
      className += trailingName;
    }
    return className;
  }

  /**
   * Returns the class name representation of the given name
   *
   * @param name The name to convert
   * @return The property name representation
   */
  public static String getClassNameRepresentation(String name) {

    StringBuilder buf = new StringBuilder();
    if (name != null && !name.isEmpty()) {
      String[] tokens = name.split("[^\\w\\d]");
      for (String token1 : tokens) {
        String token = token1.trim();
        buf.append(StringUtil.toUpperCase(token.substring(0, 1)))
          .append(token.substring(1));
      }
    }

    return buf.toString();
  }

  /**
   * Converts foo-bar into FooBar. Empty and null strings are returned as-is.
   *
   * @param name The lower case hyphen separated name
   * @return The class name equivalent.
   */
  private static String getClassNameForLowerCaseHyphenSeparatedName(String name) {
    // Handle null and empty strings.
    if (isBlank(name)) return name;

    if (name.indexOf('-') > -1) {
      StringBuilder buf = new StringBuilder();
      String[] tokens = name.split("-");
      for (String token : tokens) {
        if (token == null || token.isEmpty()) continue;
        buf.append(StringUtil.toUpperCase(token.substring(0, 1)))
          .append(token.substring(1));
      }
      return buf.toString();
    }

    return StringUtil.toUpperCase(name.substring(0, 1)) + name.substring(1);
  }

  /**
   * Retrieves the logical class name of a Grails artifact given the Grails class
   * and a specified trailing name
   *
   * @param clazz        The class
   * @param trailingName The trailing name such as "Controller" or "TagLib"
   * @return The logical class name
   */
  public static String getLogicalName(Class<?> clazz, String trailingName) {
    return getLogicalName(clazz.getName(), trailingName);
  }

  /**
   * Retrieves the logical name of the class without the trailing name
   *
   * @param name         The name of the class
   * @param trailingName The trailing name
   * @return The logical name
   */
  public static String getLogicalName(String name, String trailingName) {
    if (!isBlank(trailingName)) {
      String shortName = getShortName(name);
      if (shortName.contains(trailingName)) {
        return shortName.substring(0, shortName.length() - trailingName.length());
      }
    }
    return name;
  }

  public static String getLogicalPropertyName(String className, String trailingName) {
    if (!isBlank(className) && !isBlank(trailingName)) {
      if (className.length() == trailingName.length() + 1 && className.endsWith(trailingName)) {
        return StringUtil.toLowerCase(className.substring(0, 1));
      }
    }
    return getLogicalName(getPropertyName(className), trailingName);
  }

  /**
   * Shorter version of getPropertyNameRepresentation.
   *
   * @param name The name to convert
   * @return The property name version
   */
  public static String getPropertyName(String name) {
    return getPropertyNameRepresentation(name);
  }

  /**
   * Shorter version of getPropertyNameRepresentation.
   *
   * @param clazz The clazz to convert
   * @return The property name version
   */
  public static String getPropertyName(Class<?> clazz) {
    return getPropertyNameRepresentation(clazz);
  }

  /**
   * Returns the property name equivalent for the specified class.
   *
   * @param targetClass The class to get the property name for
   * @return A property name reperesentation of the class name (eg. MyClass becomes myClass)
   */
  public static String getPropertyNameRepresentation(Class<?> targetClass) {
    String shortName = getShortName(targetClass);
    return getPropertyNameRepresentation(shortName);
  }

  /**
   * Returns the property name representation of the given name.
   *
   * @param name The name to convert
   * @return The property name representation
   */
  public static String getPropertyNameRepresentation(String name) {
    // Strip any package from the name.
    int pos = name.lastIndexOf('.');
    if (pos != -1) {
      name = name.substring(pos + 1);
    }

    if (name.isEmpty()) return name;

    // Check whether the name begins with two upper case letters.
    if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
      return name;
    }

    String propertyName = StringUtil.toLowerCase(name.substring(0, 1)) + name.substring(1);
    if (propertyName.indexOf(' ') > -1) {
      propertyName = propertyName.replaceAll("\\s", "");
    }
    return propertyName;
  }

  /**
   * Converts foo-bar into fooBar.
   *
   * @param name The lower case hyphen separated name
   * @return The property name equivalent
   */
  public static String getPropertyNameForLowerCaseHyphenSeparatedName(String name) {
    return getPropertyName(getClassNameForLowerCaseHyphenSeparatedName(name));
  }

  public static String toPluginName(@NotNull String pluginNameInUnknownRepresentation) {
    if (pluginNameInUnknownRepresentation.indexOf('-') > -1) {
        return getPropertyNameForLowerCaseHyphenSeparatedName(pluginNameInUnknownRepresentation);
    }

    return pluginNameInUnknownRepresentation;
  }

  /**
   * Returns the class name without the package prefix.
   *
   * @param targetClass The class to get a short name for
   * @return The short name of the class
   */
  public static String getShortName(Class<?> targetClass) {
    String className = targetClass.getName();
    return getShortName(className);
  }

  /**
   * Returns the class name without the package prefix.
   *
   * @param className The class name to get a short name for
   * @return The short name of the class
   */
  public static String getShortName(String className) {
    int i = className.lastIndexOf(".");
    if (i > -1) {
      className = className.substring(i + 1);
    }
    return className;
  }

  /**
   * Retrieves the script name representation of the supplied class. For example
   * MyFunkyGrailsScript would be my-funky-grails-script.
   *
   * @param clazz The class to convert
   * @return The script name representation
   */
  public static String getScriptName(Class<?> clazz) {
    if (clazz == null) {
      return null;
    }
    return getScriptName(clazz.getName());
  }

  /**
   * Retrieves the script name representation of the given class name.
   * For example MyFunkyGrailsScript would be my-funky-grails-script.
   *
   * @param name The class name to convert.
   * @return The script name representation.
   */
  public static String getScriptName(String name) {
    if (name == null) {
      return null;
    }

    name = StringUtil.trimEnd(name, ".groovy");
    String naturalName = getNaturalName(name);
    return StringUtil.toLowerCase(naturalName.replaceAll("\\s", "-"));
  }

  /**
   * Calculates the class name from a script name in the form my-funk-grails-script.
   *
   * @param scriptName The script name
   * @return A class name
   */
  public static String getNameFromScript(String scriptName) {
    return getClassNameForLowerCaseHyphenSeparatedName(scriptName);
  }

  /**
   * Returns the name of a plugin given the name of the *GrailsPlugin.groovy
   * descriptor file. For example, "DbUtilsGrailsPlugin.groovy" gives
   * "db-utils".
   *
   * @param descriptorName The simple name of the plugin descriptor.
   * @return The plugin name for the descriptor, or {@code null}
   *         if <i>descriptorName</i> is {@code null}, or an empty string
   *         if <i>descriptorName</i> is an empty string.
   * @throws IllegalArgumentException if the given descriptor name is
   *                                  not valid, i.e. if it doesn't end with "GrailsPlugin.groovy".
   */
  public static String getPluginName(String descriptorName) {
    if (descriptorName == null || descriptorName.isEmpty()) {
      return descriptorName;
    }

    if (!descriptorName.endsWith("GrailsPlugin.groovy")) {
      throw new IllegalArgumentException("Plugin descriptor name is not valid: " + descriptorName);
    }

    int pos = descriptorName.indexOf("GrailsPlugin.groovy");
    return getScriptName(descriptorName.substring(0, pos));
  }

  /**
   * Converts a property name into its natural language equivalent eg ('firstName' becomes 'First Name')
   *
   * @param name The property name to convert
   * @return The converted property name
   */
  public static String getNaturalName(String name) {
    name = getShortName(name);
    List<String> words = new ArrayList<>();
    int i = 0;
    char[] chars = name.toCharArray();
    for (char c : chars) {
      String w;
      if (i >= words.size()) {
        w = "";
        words.add(i, w);
      }
      else {
        w = words.get(i);
      }

      if (Character.isLowerCase(c) || Character.isDigit(c)) {
        if (Character.isLowerCase(c) && w.isEmpty()) {
          c = Character.toUpperCase(c);
        }
        else if (w.length() > 1 && Character.isUpperCase(w.charAt(w.length() - 1))) {
          w = "";
          words.add(++i, w);
        }

        words.set(i, w + c);
      }
      else if (Character.isUpperCase(c)) {
        if ((i == 0 && w.isEmpty()) || Character.isUpperCase(w.charAt(w.length() - 1))) {
          words.set(i, w + c);
        }
        else {
          words.add(++i, String.valueOf(c));
        }
      }
    }

    StringBuilder buf = new StringBuilder();
    for (Iterator<String> j = words.iterator(); j.hasNext();) {
      String word = j.next();
      buf.append(word);
      if (j.hasNext()) {
        buf.append(' ');
      }
    }
    return buf.toString();
  }

  /**
   * <p>Determines whether a given string is {@code null}, empty,
   * or only contains whitespace. If it contains anything other than
   * whitespace then the string is not considered to be blank and the
   * method returns {@code false}.</p>
   * <p>We could use Commons Lang for this, but we don't want GrailsNameUtils
   * to have a dependency on any external library to minimise the number of
   * dependencies required to bootstrap Grails.</p>
   *
   * @param str The string to test.
   * @return {@code true} if the string is {@code null}, or
   *         blank.
   */
  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }
}

package com.intellij.gwt.uiBinder.references;

import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.UiBinderUtil.URN_IMPORT_PREFIX;
import static com.intellij.openapi.util.text.StringUtil.countChars;
import static com.intellij.openapi.util.text.StringUtil.split;
import static com.intellij.openapi.util.text.StringUtil.trimStart;
import static com.intellij.util.containers.ContainerUtil.getLastItem;
import static com.intellij.util.text.UniqueNameGenerator.generateUniqueName;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;

public final class UiXmlTagNameRefactoringUtil {

  public static String handleMove(XmlTag xmlTag, String newPackage, String entityName) {
    String newPrefix = xmlTag.getPrefixByNamespace(URN_IMPORT_PREFIX + newPackage);
    if (newPrefix != null) { // exact namespace match
      return newPrefix + ":" + entityName;
    }

    String oldTagName = trimStart(xmlTag.getName(), xmlTag.getNamespacePrefix() + ":");
    String newQualifiedClassName = newPackage + "." + entityName;

    int dotsInEntityName = countChars(entityName, '.');
    int oldNestingLevel = countChars(oldTagName, '.') - dotsInEntityName;
    if (oldNestingLevel > 0) {
      String closestParentPackageCandidate = stream(xmlTag.knownNamespaces())
        .filter(ns -> ns.startsWith(URN_IMPORT_PREFIX))
        .map(ns -> trimStart(ns, URN_IMPORT_PREFIX))
        .filter(ns -> newPackage.startsWith(ns) && newPackage.charAt(ns.length()) == '.') // there's no exact matching namespace, so IndexOutOfBoundsException cannot be thrown
        .max(comparingInt(String::length)) // find package with maximal length
        .orElse("");

      if (!closestParentPackageCandidate.isEmpty()) {
        int newNestingLevel = countChars(newPackage, '.', closestParentPackageCandidate.length(), false) - dotsInEntityName;
        if (newNestingLevel <= oldNestingLevel) { // don't create new namespace if package is close enough to the existing namespace
          String newTagName = newQualifiedClassName.substring(closestParentPackageCandidate.length() + 1);
          String newNamespacePrefix = xmlTag.getPrefixByNamespace(URN_IMPORT_PREFIX + closestParentPackageCandidate);
          return newNamespacePrefix + ":" + newTagName;
        }
      }
    }

    return createNewNamespacePrefix(xmlTag, newPackage) + ":" + entityName;
  }

  private static @NotNull String createNewNamespacePrefix(XmlTag xmlTag, String newPackage) {
    String name = getLastItem(split(newPackage, "."));
    String nsPrefix = generateUniqueName(name, prefix -> xmlTag.getNamespaceByPrefix(prefix).isEmpty());

    XmlFile containingFile = ((XmlFile)xmlTag.getContainingFile());
    containingFile.getRootTag().setAttribute("xmlns:" + nsPrefix, URN_IMPORT_PREFIX + newPackage);
    return nsPrefix;
  }
}

package com.intellij.dmserver.libraries;

import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;
import org.jetbrains.lang.manifest.psi.ManifestToken;
import org.jetbrains.lang.manifest.psi.ManifestTokenType;
import org.osmorc.manifest.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

public class LibraryDefinition {

  private final VirtualFile myLibraryDefFile;
  private final String mySymbolicName;
  private final String myVersion;
  private final List<BundleDefinition> myBundleDefs;
  private final List<Clause> myUnparsableClauses;
  @NonNls
  private static final String HEADER_LIBRARY_SYMBOLIC_NAME = "Library-SymbolicName";
  @NonNls
  private static final String HEADER_LIBRARY_VERSION = "Library-Version";
  @NonNls
  private static final String HEADER_IMPORT_BUNDLE = "Import-Bundle";

  @Nullable
  public static LibraryDefinition load(Project project, VirtualFile libraryDef) {

    PsiFile libraryPsi = PsiManager.getInstance(project).findFile(libraryDef);

    String symbolicName = ManifestUtils.getInstance().getHeaderValue(libraryPsi, HEADER_LIBRARY_SYMBOLIC_NAME);
    if (symbolicName == null) {
      return null; // error
    }
    String version = ManifestUtils.getInstance().getHeaderValue(libraryPsi, HEADER_LIBRARY_VERSION);
    if (version == null) {
      return null; // error
    }
    Header importBundleHeader = ManifestUtils.getInstance().findHeader(libraryPsi, HEADER_IMPORT_BUNDLE);
    if (importBundleHeader == null) {
      return null; // error
    }
    ArrayList<BundleDefinition> bundleDefs = new ArrayList<>();
    ArrayList<Clause> unparsableClauses = new ArrayList<>();

    for (Clause bundleClause : PsiTreeUtil.getChildrenOfType(importBundleHeader, Clause.class)) {
      BundleDefinition bundleDefintion = parseBundleClause(bundleClause);
      if (bundleDefintion == null) {
        unparsableClauses.add(bundleClause);
      }
      else {
        bundleDefs.add(bundleDefintion);
      }
    }

    return new LibraryDefinition(libraryDef, symbolicName, version, bundleDefs, unparsableClauses);
  }


  public LibraryDefinition(VirtualFile libraryDefFile,
                           String symbolicName,
                           String version,
                           List<BundleDefinition> bundleDefs,
                           List<Clause> unparsableClauses) {
    myLibraryDefFile = libraryDefFile;
    mySymbolicName = symbolicName;
    myVersion = version;
    myBundleDefs = bundleDefs;
    myUnparsableClauses = unparsableClauses;
  }

  public VirtualFile getLibDefFile() {
    return myLibraryDefFile;
  }

  private static BundleDefinition parseBundleClause(Clause bundleClause) {
    HeaderValuePart bundleNamePart = PsiTreeUtil.getChildOfType(bundleClause, HeaderValuePart.class);
    if (bundleNamePart == null) {
      return null; // error
    }

    Attribute[] bundleAttributes = PsiTreeUtil.getChildrenOfType(bundleClause, Attribute.class);
    String bundleVersion = null;
    for (Attribute bundleAttribute : bundleAttributes) {
      if ("version".equals(bundleAttribute.getNameElement().getUnwrappedText())) {
        boolean hasOpeningBracket = false;
        boolean hasClosingBracket = false;
        String leftBound = null;
        String rightBound = null;

        for (ManifestToken token : PsiTreeUtil.getChildrenOfType(bundleAttribute.getValueElement(), ManifestToken.class)) {
          ManifestTokenType tokenType = token.getTokenType();
          if (tokenType == ManifestTokenType.OPENING_BRACKET_TOKEN) {
            if ("[".equals(token.getText()) && leftBound == null) {
              hasOpeningBracket = true;
            }
          }
          else if (tokenType == ManifestTokenType.CLOSING_BRACKET_TOKEN) {
            if ("]".equals(token.getText()) && rightBound != null) {
              hasClosingBracket = true;
            }
          }
          else if (tokenType == ManifestTokenType.HEADER_VALUE_PART) {
            if (leftBound == null) {
              leftBound = token.getText().trim();
              if (leftBound.startsWith("[")) {
                leftBound = leftBound.substring(1);
                hasOpeningBracket = true;
              }
            }
            else if (rightBound == null) {
              rightBound = token.getText().trim();
              if (rightBound.endsWith("]")) {
                rightBound = rightBound.substring(0, rightBound.length() - 1);
                hasClosingBracket = true;
              }
            }
            else {
              return null; // error
            }
          }
        }
        if (!hasOpeningBracket || !hasClosingBracket) {
          return null; // unsupported
        }
        if (leftBound == null || rightBound == null) {
          return null; // error
        }
        if (!leftBound.equals(rightBound)) {
          return null; // unsupported
        }
        bundleVersion = leftBound;

        break;
      }
    }
    if (bundleVersion == null) {
      return null; // error
    }

    return new BundleDefinition(bundleNamePart.getUnwrappedText(), bundleVersion);
  }

  public String getSymbolicName() {
    return mySymbolicName;
  }

  public String getVersion() {
    return myVersion;
  }

  public List<BundleDefinition> getBundleDefs() {
    return myBundleDefs;
  }

  public List<Clause> getUnparsableClauses() {
    return myUnparsableClauses;
  }
}

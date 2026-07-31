package com.intellij.gwt.uiBinder;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PathUtil;
import com.intellij.xml.XmlSchemaProvider;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GwtUiXmlSchemaProvider extends XmlSchemaProvider {
  private static final Key<XmlFile> UI_BINDER_PATCHED_SCHEMA_KEY = Key.create("GWT_UI_BINDER_PATCHED_SCHEMA");
  private static final Key<XmlFile> CLIENT_UI_PATCHED_SCHEMA_KEY = Key.create("GWT_CLIENT_UI_PATCHED_SCHEMA");
  private static final @NonNls Set<String> RESERVED_PREFIXES = Set.of("client", "g", "ui");
  private static final @NonNls Map<String, Couple<@NonNls String>> DEFAULT_SCHEMES = new HashMap<>();

  public static final @NonNls String CLIENT_UI_NAMESPACE = "urn:import:com.google.gwt.user.client.ui";

  static {
    DEFAULT_SCHEMES.put(UiBinderUtil.UI_BINDER_NAMESPACE, Couple.of("ui", "com/google/gwt/uibinder/resources/UiBinder.xsd"));
    DEFAULT_SCHEMES.put(CLIENT_UI_NAMESPACE,
                        Couple.of("g", "com/google/gwt/uibinder/resources/com.google.gwt.user.client.ui.xsd"));
  }

  @Override
  public XmlFile getSchema(final @NotNull @NonNls String url, final @Nullable Module module, @NotNull PsiFile baseFile) {
    if (module == null) {
      return null;
    }

    return findDefaultSchema(url, module);
  }

  public static boolean isDefaultSchema(String namespace) {
    return DEFAULT_SCHEMES.containsKey(namespace);
  }

  public static @Nullable XmlFile findDefaultSchema(String url, Module module) {
    final Couple<String> pair = DEFAULT_SCHEMES.get(url);
    if (pair != null) {
      final String schemaPath = pair.getSecond();
      final PsiPackage psiPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(PathUtil.getParentPath(schemaPath).replace('/', '.'));
      if (psiPackage != null) {
        final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
        final String fileName = PathUtil.getFileName(schemaPath);
        for (PsiDirectory directory : psiPackage.getDirectories(scope)) {
          final PsiFile file = directory.findFile(fileName);
          if (file instanceof XmlFile xmlFile) {
            if (url.equals(UiBinderUtil.UI_BINDER_NAMESPACE)) {
              return getOrCreatePatchedUiBinderCopy(xmlFile);
            }
            if (url.equals(CLIENT_UI_NAMESPACE)) {
              return getOrCreatePatchedClientUiCopy(xmlFile);
            }
            return xmlFile;
          }
        }
      }
    }

    return null;
  }

  private static XmlFile getOrCreatePatchedUiBinderCopy(XmlFile xmlFile) {
    XmlFile patched = xmlFile.getUserData(UI_BINDER_PATCHED_SCHEMA_KEY);
    if (patched == null) {
      patched = (XmlFile)xmlFile.copy();
      final XmlDocument document = patched.getDocument();
      if (document != null) {
        final XmlTag rootTag = document.getRootTag();
        if (rootTag != null) {
          for (XmlTag tag : rootTag.getSubTags()) {
            if (!"UiBinder".equals(tag.getAttributeValue("name", XmlUtil.XML_SCHEMA_URI))) {
              continue;
            }

            for (XmlTag typeTag : tag.getSubTags()) {
              if (!"complexType".equals(typeTag.getLocalName())) {
                continue;
              }

              boolean baseMessagesInterfaceFound = false;
              for (XmlTag attributeTag : typeTag.getSubTags()) {
                if ("attribute".equals(attributeTag.getLocalName())) {
                  attributeTag.setAttribute("form", "qualified");
                  if (!baseMessagesInterfaceFound && "baseMessagesInterface".equals(attributeTag.getAttributeValue("name"))) {
                    baseMessagesInterfaceFound = true;
                  }
                }
              }

              if (!baseMessagesInterfaceFound) {
                // attribute is present in official documentation but absent in UiBinder.xsd
                XmlTag baseMessagesInterface = typeTag.createChildTag("attribute", rootTag.getNamespace(), null, false);
                baseMessagesInterface.setAttribute("name", "baseMessagesInterface");
                baseMessagesInterface.setAttribute("type", rootTag.getNamespacePrefix() + ":string");
                baseMessagesInterface.setAttribute("form", "qualified");

                typeTag.addSubTag(baseMessagesInterface, false);
              }
            }
          }
        }
      }
      xmlFile.putUserData(UI_BINDER_PATCHED_SCHEMA_KEY, patched);
    }
    return patched;
  }

  private static XmlFile getOrCreatePatchedClientUiCopy(XmlFile xmlFile) {
    XmlFile patchedFile = xmlFile.getUserData(CLIENT_UI_PATCHED_SCHEMA_KEY);
    if (patchedFile == null) {
      patchedFile = xmlFile;

      XmlTag originalRootTag = patchedFile.getRootTag();
      if (originalRootTag != null) {
        String namespace = originalRootTag.getNamespace();
        String namespacePrefix = originalRootTag.getNamespacePrefix();

        boolean appendListBox = true;
        for (XmlTag subTag : originalRootTag.getSubTags()) {
          String nameAttributeValue = subTag.getAttributeValue("name", namespace);
          if ("ListBox".equals(nameAttributeValue)) {
            appendListBox = false;
          }
        }

        if (appendListBox) {
          patchedFile = (XmlFile)xmlFile.copy();
          XmlTag rootTag = patchedFile.getRootTag();
          if (rootTag != null) {
            XmlElementFactory elementFactory = XmlElementFactory.getInstance(xmlFile.getProject());
            @NonNls String template = "<{0}:element name=\"ListBox\">" +
                                      "  <{0}:complexType mixed=\"true\">" +
                                      "    <{0}:anyAttribute processContents=\"lax\"/>" +
                                      "    <{0}:choice minOccurs=\"0\" maxOccurs=\"unbounded\">" +
                                      "      <{0}:element name=\"item\">" +
                                      "        <{0}:complexType>" +
                                      "          <{0}:attribute name=\"value\" type=\"{0}:string\"/>" +
                                      "        </{0}:complexType>" +
                                      "      </{0}:element>" +
                                      "    </{0}:choice>" +
                                      "  </{0}:complexType>" +
                                      "</{0}:element>";
            XmlTag subTag = elementFactory.createTagFromText(MessageFormat.format(template, namespacePrefix));
            rootTag.addSubTag(subTag, false);
          }
        }
      }
      xmlFile.putUserData(CLIENT_UI_PATCHED_SCHEMA_KEY, patchedFile);
    }
    return patchedFile;
  }

  @Override
  public @NotNull Set<String> getAvailableNamespaces(@NotNull XmlFile file, @Nullable String tagName) {
    if (tagName == null) {
      return DEFAULT_SCHEMES.keySet();
    }
    for (Map.Entry<String, Couple<String>> entry : DEFAULT_SCHEMES.entrySet()) {
      if (tagName.equals(entry.getValue().getFirst())) {
        return Collections.singleton(entry.getKey());
      }
    }
    return Collections.emptySet();
  }

  @Override
  public String getDefaultPrefix(@NotNull @NonNls String namespace, @NotNull XmlFile context) {
    final Couple<String> pair = DEFAULT_SCHEMES.get(namespace);
    if (pair != null) {
      return pair.getFirst();
    }
    if (namespace.startsWith(UiBinderUtil.URN_IMPORT_PREFIX)) {
      return suggestPrefix(StringUtil.trimStart(namespace, UiBinderUtil.URN_IMPORT_PREFIX));
    }
    return null;
  }

  private static String suggestPrefix(String packageName) {
    final List<String> names = StringUtil.split(packageName, ".");
    for (int i = names.size() - 1; i >= 0; i--) {
      String name = names.get(i);
      if (!RESERVED_PREFIXES.contains(name)) {
        return name;
      }
    }
    return "x";
  }

  @Override
  public boolean isAvailable(@NotNull XmlFile file) {
    return UiBinderUtil.isUiXmlFile(file);
  }

}

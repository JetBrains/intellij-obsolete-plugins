// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.jsp.impl.TldDescriptor;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PathUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GspTagDescriptorService {

  private static final Map<String, TagDescriptor> tagMap = new LinkedHashMap<>();
  static {
    tagMap.put("actionSubmit", new TagDescriptor("input", "type", "name", "value"));
    tagMap.put("actionSubmitImage", new TagDescriptor("input", "type", "name", "value"));
    tagMap.put("applyLayout", null);
    tagMap.put("checkBox", new TagDescriptor("input", "type", "name", "value", "checked"));
    tagMap.put("collect", null);
    tagMap.put("cookie", null);
    tagMap.put("country", null);
    tagMap.put("countrySelect", new TagDescriptor("select", "name"));
    tagMap.put("createLink", null);
    tagMap.put("createLinkTo", null);
    tagMap.put("currencySelect", new TagDescriptor("select", "name"));
    tagMap.put("datePicker", null);
    tagMap.put("def", null);
    tagMap.put("each", null);
    tagMap.put("eachError", null);
    tagMap.put("else", null);
    tagMap.put("elseif", null);
    tagMap.put("encodeAs", null);
    tagMap.put("escapeJavascript", null);
    tagMap.put("external", null);
    tagMap.put("field", new TagDescriptor("input", "type"));
    tagMap.put("fieldError", null);
    tagMap.put("fieldValue", null);
    tagMap.put("findAll", null);
    tagMap.put("form", new TagDescriptor("form", "action", "method"));
    tagMap.put("formatBoolean", null);
    tagMap.put("formatDate", null);
    tagMap.put("formatNumber", null);
    tagMap.put("formRemote", new TagDescriptor("form", "action", "method", "onsubmit"));
    tagMap.put("grep", null);
    tagMap.put("hasErrors", null);
    tagMap.put("header", null);
    tagMap.put("hiddenField", new TagDescriptor("input", "type", "name"));
    tagMap.put("if", null);
    tagMap.put("ifPageProperty", null);
    tagMap.put("img", new TagDescriptor("img", "dir", "uri", "file", "plugin"));
    tagMap.put("include", null);
    tagMap.put("javascript", null);
    tagMap.put("join", null);
    tagMap.put("layoutBody", null);
    tagMap.put("layoutHead", null);
    tagMap.put("layoutTitle", null);
    tagMap.put("link", new TagDescriptor("a", "href"));
    tagMap.put("localeSelect", new TagDescriptor("select", "name"));
    tagMap.put("message", null);
    tagMap.put("meta", null);
    tagMap.put("pageProperty", null);
    tagMap.put("paginate", null);
    tagMap.put("passwordField", new TagDescriptor("input", "type", "name"));
    tagMap.put("radio", new TagDescriptor("input", "type", "name", "value", "checked"));
    tagMap.put("radioGroup", new TagDescriptor("input", "type", "name", "value", "checked"));
    tagMap.put("remoteField", new TagDescriptor("input", "type", "name", "value", "onkeyup"));
    tagMap.put("remoteFunction", null);
    tagMap.put("remoteLink", new TagDescriptor("a", "onclick"));
    tagMap.put("render", null);
    tagMap.put("renderException", null);
    tagMap.put("resource", null);
    tagMap.put("renderErrors", null);
    tagMap.put("renderInput", null);
    tagMap.put("select", new TagDescriptor("select", "name"));
    tagMap.put("set", null);
    tagMap.put("setProvider", null);
    tagMap.put("sortableColumn", null);
    tagMap.put("submitButton", new TagDescriptor("input", "type", "name", "value"));
    tagMap.put("submitToRemote", new TagDescriptor("input", "type", "name", "value"));
    tagMap.put("textArea", new TagDescriptor("textarea"));
    tagMap.put("textField", new TagDescriptor("input", "type", "name", "value"));
    tagMap.put("timeZoneSelect", new TagDescriptor("select", "name"));
    tagMap.put("uploadForm", new TagDescriptor("form", "action", "method", "enctype"));
    tagMap.put("unless", null);
    tagMap.put("validate", null);
    tagMap.put("withTag", null);
    tagMap.put("while", null);
  }

  private final Map<String, Pair<XmlAttributeDescriptor[], Map<String, XmlAttributeDescriptor>>> myTagDescriptors;

  public GspTagDescriptorService(Project project) {
    PsiFile gspFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.gsp", GspFileType.GSP_FILE_TYPE, "");

    Map<String, XmlAttributeDescriptor[]> htmlTagAttributes = getHtmlTagAttributes(project);

    Map<String, Pair<XmlAttributeDescriptor[], Map<String, XmlAttributeDescriptor>>> tagDescriptors = new HashMap<>();

    TldDescriptor descriptor = getTldDescriptor(project);
    if (descriptor != null) {
      XmlDocument document = (XmlDocument)gspFile.getFirstChild();
      assert document != null;
      XmlTag gspRootTag = (XmlTag)document.getFirstChild().getNextSibling();

      for (XmlElementDescriptor elementDescriptor : descriptor.getRootElementsDescriptors(document)) {
        String tagName = elementDescriptor.getName();

        Map<String, XmlAttributeDescriptor> attrMap = new LinkedHashMap<>();

        TagDescriptor tagDescriptor = tagMap.get(tagName);
        if (tagDescriptor != null) {
          XmlAttributeDescriptor[] htmlAttr = htmlTagAttributes.get(tagDescriptor.htmlTag);
          for (XmlAttributeDescriptor attrDescr : htmlAttr) {
            attrMap.put(attrDescr.getName(), attrDescr);
          }

          for (String excluded : tagDescriptor.excludedAttributes) {
            attrMap.remove(excluded);
          }
        }

        for (XmlAttributeDescriptor attrDescr : elementDescriptor.getAttributesDescriptors(gspRootTag)) {
          attrMap.put(attrDescr.getName(), attrDescr);
        }

        XmlAttributeDescriptor[] allAttributes = attrMap.values().toArray(XmlAttributeDescriptor.EMPTY);

        tagDescriptors.put(tagName, Pair.create(allAttributes, attrMap));
      }
    }

    myTagDescriptors = tagDescriptors;
  }

  private static Map<String, XmlAttributeDescriptor[]> getHtmlTagAttributes(Project project) {
    Map<String, XmlAttributeDescriptor[]> res = new HashMap<>();

    for (Map.Entry<String, TagDescriptor> entry : tagMap.entrySet()) {
      TagDescriptor tagDescriptor = entry.getValue();
      if (tagDescriptor != null) {
        res.put(tagDescriptor.htmlTag, null);
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("<html><body>");

    for (String htmlTag : res.keySet()) {
      sb.append('<').append(htmlTag).append("/>");
    }

    sb.append("</body></html>");

    PsiFile htmlFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.html", HTMLLanguage.INSTANCE, sb);

    XmlTag[] htmlTags = ((XmlTag)htmlFile.getFirstChild().getFirstChild().getNextSibling()).getSubTags()[0].getSubTags();

    for (XmlTag tag : htmlTags) {
      res.put(tag.getName(), tag.getDescriptor().getAttributesDescriptors(tag));
    }

    return res;
  }

  @TestOnly
  public static Set<String> getAllTags() {
    return tagMap.keySet();
  }

  private static final class TagDescriptor {
    public final String htmlTag;

    public final String[] excludedAttributes;

    private TagDescriptor(String htmlTag, String... excludedAttributes) {
      this.htmlTag = htmlTag;
      this.excludedAttributes = excludedAttributes;
    }
  }

  public static GspTagDescriptorService getInstance(Project project) {
    return project.getService(GspTagDescriptorService.class);
  }

  public XmlAttributeDescriptor[] getAttributesDescriptors(String tagName) {
    Pair<XmlAttributeDescriptor[], Map<String, XmlAttributeDescriptor>> pair = myTagDescriptors.get(tagName);
    return pair == null ? XmlAttributeDescriptor.EMPTY : pair.first;
  }

  public @Nullable XmlAttributeDescriptor getAttributesDescriptor(String tagName, String attributeName) {
    Pair<XmlAttributeDescriptor[], Map<String, XmlAttributeDescriptor>> pair = myTagDescriptors.get(tagName);
    if (pair == null) return null;

    return pair.second.get(attributeName);
  }

  public static @Nullable TldDescriptor getTldDescriptor(Project project) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(getTldFile());
    if (!(psiFile instanceof XmlFile)) return null;

    return GrailsUtils.getTldDescriptor((XmlFile)psiFile);
  }

  public static @NotNull VirtualFile getTldFile() {
    String path = PathUtil.getJarPathForClass(GspTagLibUtil.class);

    VirtualFile tldFile;

    if (path.endsWith(".jar")) {
      tldFile = JarFileSystem.getInstance().findFileByPath(path + "!/org/jetbrains/plugins/grails/lang/gsp/resolve/taglib/tld/grails.tld");
    }
    else {
      tldFile = LocalFileSystem.getInstance().findFileByPath(path + "/org/jetbrains/plugins/grails/lang/gsp/resolve/taglib/tld/grails.tld");
    }

    assert tldFile != null;

    return tldFile;
  }
}

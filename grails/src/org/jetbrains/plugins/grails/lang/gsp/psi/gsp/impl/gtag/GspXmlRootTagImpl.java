// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.jsp.JspManager;
import com.intellij.jsp.impl.TldDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.BidirectionalMap;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.GspXmlTagBaseImpl;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GspXmlRootTagImpl extends GspXmlTagBaseImpl implements GspXmlRootTag {

  private static final @NonNls String GSP_ROOT_TAG = "gsp:root";
  private static final @NonNls String GSP_ROOT_TAG_LOCAL = "root";

  public GspXmlRootTagImpl() {
    super(GspElementTypes.GSP_ROOT_TAG);
  }

  @Override
  public String toString() {
    return "Gsp root tag";
  }

  @Override
  public @NotNull String getName() {
    return GSP_ROOT_TAG;
  }

  @Override
  public @NotNull String getLocalName() {
    return GSP_ROOT_TAG_LOCAL;
  }

  @Override
  public @NotNull String getNamespace() {
    return GspTagLibUtil.DEFAULT_TAGLIB_PREFIX;
  }

  @Override
  public String getPrefixByNamespace(String namespace) {
    String ns = getNamespaceData().getPrefixByNamespace(namespace);
    return ns != null ? ns : super.getPrefixByNamespace(namespace);
  }

  @Override
  public @NotNull String getNamespaceByPrefix(String prefix) {
    if (!isValid()) {
      final PsiFile containingFile = SharedImplUtil.getContainingFile(getNode());
      String message;
      if (containingFile == null) {
        message = "no file";
      } else {
        final FileViewProvider provider = containingFile.getViewProvider();
        message = "VFile valid=" + provider.getVirtualFile().isValid() + "; ";
        final PsiFile newFile = provider.getPsi(containingFile.getLanguage());
        message += "newFile=" + newFile + "; equals=" + (newFile == containingFile);
      }
      throw new PsiInvalidElementAccessException(this, message);
    }
    String ns = getNamespaceData().getNamespaceByPrefix(prefix);
    return ns == null ? super.getNamespaceByPrefix(prefix) : ns;
  }

  private NamespaceData getNamespaceData() {
    return CachedValuesManager.getCachedValue(this, () -> Result.create(
      new NamespaceData(this), PsiModificationTracker.MODIFICATION_COUNT
    ));
  }

  @Override
  public String[] knownNamespaces() {
    return getNamespaceData().knownNamespaces();
  }

  @Override
  public XmlNSDescriptor getNSDescriptor(final String namespace, final boolean strict) {
    return getNamespaceData().getNSDescriptor(namespace);
  }

  @Override
  public XmlElementDescriptor getDescriptor() {
    return new GspRootElementDescriptor(this, GspNamespaceDescriptor.getDefaultNsDescriptor(this));
  }

  private static class GspRootElementDescriptor extends GspElementDescriptorBase {
    GspRootElementDescriptor(final GspXmlRootTagImpl tag, final GspNamespaceDescriptor nsDescriptor) {
      super(nsDescriptor, tag, "gsp root");
    }

    @Override
    public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
      return XmlAttributeDescriptor.EMPTY;
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
      return null;
    }

  }

  private static class NamespaceData {
    private final BidirectionalMap<String, String> myPrefix2Namespace = new BidirectionalMap<>();
    private final Map<String, XmlNSDescriptor> myUri2Descriptor = new HashMap<>();

    NamespaceData(final GspXmlRootTagImpl tag) {
      assert tag.isValid();
      final Project project = tag.getProject();
      for (final String prefix : GspTagLibUtil.getTagLibClasses(tag).keySet()) {
        myPrefix2Namespace.put(prefix, prefix);

        XmlNSDescriptor nsDescriptor;

        TagLibNamespaceDescriptor descriptor = GspTagLibUtil.getTagLibClasses(tag, prefix);
        if (descriptor != null && !descriptor.getClasses().isEmpty()) {
          nsDescriptor = new GspNamespaceDescriptor(descriptor);
        }
        else {
          if (GspTagLibUtil.DEFAULT_TAGLIB_PREFIX.equals(prefix)) {
            final TldDescriptor tldDescriptor = GspTagDescriptorService.getTldDescriptor(project);
            nsDescriptor = tldDescriptor != null ? tldDescriptor : GspNamespaceDescriptor.getDefaultNsDescriptor(tag);
          }
          else {
            nsDescriptor = null;
          }
        }

        myUri2Descriptor.put(prefix, nsDescriptor);
      }

      PsiFile containingFile = tag.getContainingFile();
      if (containingFile instanceof GspFile) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(tag);
        final JspManager jspManager = JspManager.getInstance(project);

        for (GspDirective directive : ((GspFile)containingFile).getDirectiveTags(GspDirectiveKind.TAGLIB, false)) {
          final String prefix = directive.getAttributeValue("prefix");
          if (StringUtil.isEmpty(prefix) || "g".equals(prefix)) continue;

          final String uri = directive.getAttributeValue("uri");
          if (StringUtil.isEmpty(uri)) continue;

          myPrefix2Namespace.put(prefix, uri);
          if (jspManager != null) {
            final XmlFile tldFile = jspManager.getTldFileByUri(uri, module, null);
            XmlNSDescriptor nsDescriptor = GrailsUtils.getTldDescriptor(tldFile);
            myUri2Descriptor.put(uri, nsDescriptor);
          }
        }
      }

      myPrefix2Namespace.put(GspTmplNamespaceDescriptor.NAMESPACE_TMPL, GspTmplNamespaceDescriptor.NAMESPACE_TMPL);
      myUri2Descriptor.put(GspTmplNamespaceDescriptor.NAMESPACE_TMPL, new GspTmplNamespaceDescriptor(tag));

      myPrefix2Namespace.put(GspLinkNamespaceDescriptor.NAMESPACE_LINK, GspLinkNamespaceDescriptor.NAMESPACE_LINK);
      myUri2Descriptor.put(GspLinkNamespaceDescriptor.NAMESPACE_LINK, GspLinkNamespaceDescriptor.INSTANCE);
    }

    public @Nullable XmlNSDescriptor getNSDescriptor(@NotNull String uri) {
      return myUri2Descriptor.get(uri);
    }

    public @Nullable String getNamespaceByPrefix(@NotNull String prefix) {
      return myPrefix2Namespace.get(prefix);
    }

    public @Nullable String getPrefixByNamespace(@NotNull String namespace) {
      final List<String> value = myPrefix2Namespace.getKeysByValue(namespace);
      return value == null || value.isEmpty() ? null : value.get(0);
    }

    public String[] knownNamespaces() {
      return ArrayUtilRt.toStringArray(myPrefix2Namespace.values());
    }
  }
}

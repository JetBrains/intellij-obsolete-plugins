// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.impl.source.xml.TagNameReference;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.impl.source.xml.XmlTokenImpl;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.XmlExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.jetbrains.plugins.grails.references.common.TemplateFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GspGrailsTagImpl extends XmlTagImpl implements GspGrailsTag {

  public GspGrailsTagImpl() {
    super(GspElementTypes.GRAILS_TAG);
  }

  @Override
  public String toString() {
    return "Grails tag";
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    super.accept(visitor);
  }

  @Override
  public boolean endsByError() {
    return endsByError(this);
  }

  @Override
  public PsiReference @NotNull [] getReferences(@NotNull PsiReferenceService.Hints hints) {
    final ASTNode startTagName = XmlChildRole.START_TAG_NAME_FINDER.findChild(this);
    if (startTagName == null || !StringUtil.startsWith(startTagName.getChars(), "tmpl:")) return super.getReferences(hints);

    List<PsiReference> refs = new ArrayList<>();

    final String controllerName;

    PsiFile containingFile = getContainingFile();
    VirtualFile file;
    if (containingFile != null &&
        containingFile.getViewProvider() instanceof GspFileViewProvider &&
        (file = containingFile.getOriginalFile().getVirtualFile()) != null) {
      controllerName = GrailsUtils.getExistingControllerNameDirByGsp(file, containingFile.getProject());
    }
    else {
      controllerName = null;
    }

    final String text = getLocalName();

    if (!text.startsWith("/") && controllerName == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    String trimedUrl = PathReference.trimPath(text);

    final FileReferenceSet set = new TemplateFileReferenceSet(controllerName, trimedUrl, this, "tmpl:".length() + 1, null, true, true, null) {
      @Override
      protected PsiElement doSetTextToElement(String text) {
        if (!text.matches("[\\w\\-/]+")) return getElement();

        PsiFile gspFile = PsiFileFactory.getInstance(getProject()).createFileFromText("a.gsp", "<tmpl:" + text + "></tmpl:" + text + '>');

        boolean isStartName = true;
        for (PsiElement e = GspGrailsTagImpl.this.getFirstChild(); e != null; e = e.getNextSibling()) {
          if (PsiImplUtil.isLeafElementOfType(e, XmlTokenType.XML_TAG_NAME)) {
            if (isStartName) {
              e = e.replace(gspFile.findElementAt(1));
              isStartName = false;
            }
            else {
              e = e.replace(gspFile.findElementAt(gspFile.getTextLength() - 2));
              break;
            }
          }
        }

        return GspGrailsTagImpl.this;
      }
    };

    Collections.addAll(refs, set.getAllReferences());

    final ASTNode endTagName = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(this);
    if (endTagName != null) {
      final XmlExtension extension = XmlExtension.getExtensionByElement(this);
      if (extension != null) {
        TagNameReference endTagRef = extension.createTagNameReference(endTagName, false);
        if (endTagRef != null) {
          refs.add(endTagRef);
        }
      }
    }

    return refs.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static boolean endsByError(PsiElement elem) {
    if (elem instanceof PsiErrorElement) return true;
    PsiElement lastChild = elem.getLastChild();
    return lastChild != null && endsByError(lastChild);
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    String oldName = getName();
    if (oldName.startsWith("tmpl:") && oldName.length() > 5 && oldName.charAt(5) != '_') {
      if (name.startsWith("tmpl:_")) {
        name = "tmpl:" + name.substring(6);
      }
    }
    return super.setName(name);
  }

  @Override
  public PsiElement getNameElement() {
    for (ASTNode e = getFirstChildNode(); e != null; e = e.getTreeNext()) {
      if (e instanceof XmlTokenImpl xmlToken) {

        if (xmlToken.getTokenType() == XmlTokenType.XML_TAG_NAME) return xmlToken;
      }
    }

    return null;
  }


}

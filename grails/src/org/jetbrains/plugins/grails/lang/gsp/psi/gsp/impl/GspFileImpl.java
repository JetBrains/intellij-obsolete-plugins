// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.javascript.psi.JSElement;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.ecmal4.XmlBackedJSClass;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind;
import org.jetbrains.plugins.grails.lang.gsp.parsing.gsp.GspParserDefinition;
import org.jetbrains.plugins.grails.lang.gsp.psi.GspPsiElementFactory;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GspFileImpl extends PsiFileImpl implements GspFile {

  private Map<GspDirectiveKind, List<GspDirective>> myDirectives;

  private volatile List<XmlTag> myJsTagCache;

  public GspFileImpl(FileViewProvider viewProvider) {
    super(GspParserDefinition.GSP_FILE, GspParserDefinition.GSP_FILE, viewProvider);
  }

  @Override
  public String toString() {
    return "Groovy Server Pages file";
  }

  @Override
  public GroovyFileBase getGroovyLanguageRoot() {
    PsiFile psiFile = getViewProvider().getPsi(GroovyLanguage.INSTANCE);
    return ((GroovyFileBase) psiFile);
  }

  @Override
  public void clearCaches() {
    super.clearCaches();
    myDirectives = null;
    myJsTagCache = null;
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    visitor.visitFile(this);
  }

  @Override
  public List<GspDirective> getDirectiveTags(final GspDirectiveKind directiveKind, final boolean searchInIncludes) {
    if (searchInIncludes) {
      //todo implement me!
    }

    if (myDirectives == null) {
      final Map<GspDirectiveKind, List<GspDirective>> directivesMap = new EnumMap<>(GspDirectiveKind.class);

      XmlUtil.processXmlElements(getRootTag(), new PsiElementProcessor<>() {
        @Override
        public boolean execute(final @NotNull PsiElement element) {
          if (element instanceof GspDirective directive) {
            final GspDirectiveKind directiveKindByTag = GspDirectiveKind.getKind(directive);
            if (directiveKindByTag != null) {
              List<GspDirective> directives = directivesMap.get(directiveKindByTag);
              if (directives == null) {
                directivesMap.put(directiveKindByTag, directives = new ArrayList<>());
              }
              directives.add(directive);
            }
          }
          return true;
        }
      }, true);

      myDirectives = directivesMap;
    }

    List<GspDirective> directives = myDirectives.get(directiveKind);
    return directives == null ? Collections.emptyList() : directives;
  }

  @Override
  public void addImportForClass(PsiClass aClass) throws IncorrectOperationException {
    addImport(aClass.getQualifiedName());
  }

  @Override
  public void addImportStatement(GrImportStatement statement) {
    //todo get import string
  }

  @Override
  public PsiElement createGroovyScriptletFromText(String text) throws IncorrectOperationException {
    GspPsiElementFactory factory = GspPsiElementFactory.getInstance(getProject());
    GspScriptletTag script = factory.createScriptletTagFromText(text);
    GspXmlRootTag rootTag = getRootTag();
    assert rootTag != null;
    PsiElement firstChild = rootTag.getFirstChild();
    if (firstChild != null) {
      rootTag.addBefore(script, firstChild);
    } else {
      rootTag.add(script);
    }
    return script;
  }

  @Override
  public GspXmlRootTag getRootTag() {
    XmlDocument document = getDocument();
    assert document != null;
    PsiElement child = document.getFirstChild();
    assert child != null;
    return ((GspXmlRootTag) child.getNextSibling());
  }

  @Override
  public @NotNull FileViewProvider getViewProvider() {
    return super.getViewProvider();
  }

  private void addImport(String importString) throws IncorrectOperationException {
    GspDirective directive = calculatePositionForImport();
    GspPsiElementFactory factory = GspPsiElementFactory.getInstance(getProject());
    if (directive != null) {
      XmlAttribute importAttribute = directive.getAttribute("import");
      if (importAttribute != null) {
        importString = importString + "; " + importAttribute.getValue();
      }

      directive.addOrReplaceAttribute(factory.createDirectiveAttribute("import", importString));
    } else {
      GspDirective newDirective = factory.createDirectiveByKind(GspDirectiveKind.PAGE);
      newDirective.addOrReplaceAttribute(factory.createDirectiveAttribute("import", importString));
      GspXmlRootTag rootTag = getRootTag();
      assert rootTag != null;
      PsiElement firstChild = rootTag.getFirstChild();
      if (firstChild != null) {
        rootTag.addBefore(newDirective, firstChild);
      } else {
        rootTag.add(newDirective);
      }
    }
  }

  private @Nullable GspDirective calculatePositionForImport() {
    List<GspDirective> directives = getDirectiveTags(GspDirectiveKind.PAGE, false);
    if (directives.isEmpty()) return null;
    for (GspDirective directive : directives) {
      if (directive.getAttribute("import") != null) return directive;
    }
    return directives.get(0);
  }

  @Override
  public @NotNull FileType getFileType() {
    return GspFileType.GSP_FILE_TYPE;
  }

  @Override
  public @Nullable XmlDocument getDocument() {
    CompositeElement treeElement = calcTreeElement();

    ASTNode firstNode = treeElement.getFirstChildNode();
    if (firstNode != null) {
      final PsiElement asPsiElement = firstNode.getPsi();
      if (asPsiElement instanceof XmlDocument) {
        return (XmlDocument) asPsiElement;
      }
    }
    return null;
  }

  @Override
  public boolean processJsInJavascriptTags(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @NotNull PsiElement place) {
    JSFile fileOfPlace = PsiTreeUtil.getParentOfType(place, JSFile.class);

    for (XmlTag xmlTag : getJsTags()) {
      ASTNode tagEnd = XmlChildRole.START_TAG_END_FINDER.findChild(xmlTag.getNode());
      if (tagEnd != null) {
        ASTNode content = tagEnd.getTreeNext();
        if (content != null && content.getPsi() instanceof OuterLanguageElement) {
          PsiElement injected = InjectedLanguageManager.getInstance(getProject()).findInjectedElementAt(this, content.getStartOffset());
          injected = PsiTreeUtil.getParentOfType(injected, JSFile.class);

          if (injected != null && injected != fileOfPlace) {
            ResolveState s = state.put(XmlBackedJSClass.PROCESS_XML_BACKED_CLASS_MEMBERS_HINT, Boolean.TRUE);
            if (!JSResolveUtil.processDeclarationsInScope((JSElement)injected, processor, s, null, place)) return false;
          }
        }
      }
    }

    return true;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
    if (!super.processDeclarations(processor, state, lastParent, place)) return false;

    // JavaScript support
    if (JavaScriptIntegrationUtil.isJSElement(place)) {
      GspHtmlFileImpl htmlFile = (GspHtmlFileImpl)getHtmlLanguageRoot().getOriginalFile();
      return htmlFile.processDeclarations(processor, state, lastParent, place);
    }

    return true;
  }

  private List<XmlTag> getJsTags() {
    List<XmlTag> res = myJsTagCache;
    if (res == null) {
      res = new ArrayList<>();

      XmlDocument document = (XmlDocument)getFirstChild();
      assert document != null;

      final List<XmlTag> finalRes = res;
      document.accept(new XmlRecursiveElementVisitor(){
        @Override
        public void visitXmlTag(@NotNull XmlTag tag) {
          if (JavaScriptIntegrationUtil.isJsInjectionTag(tag.getName())) {
            finalRes.add(tag);
          }
          else {
            super.visitXmlTag(tag);
          }
        }
      });

      myJsTagCache = res;
    }

    return res;
  }

  @Override
  public boolean processElements(PsiElementProcessor processor, PsiElement place) {
    final XmlDocument document = getDocument();
    return document == null || document.processElements(processor, place);
  }

  @Override
  public GspHtmlFileImpl getHtmlLanguageRoot() {
    return ((GspHtmlFileImpl)getViewProvider().getPsi(HTMLLanguage.INSTANCE));
  }

  @Override
  public @NotNull GlobalSearchScope getFileResolveScope() {
    Module module = ModuleUtilCore.findModuleForPsiElement(this);
    VirtualFile file = getVirtualFile();
    if (file != null && getOriginalFile() != this) {
      file = getOriginalFile().getVirtualFile();
    }
    if (module != null && file != null) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
      boolean includeTests = projectFileIndex.isInTestSourceContent(file) ||
                             !(FileTypeRegistry.getInstance().isFileOfType(file, JavaFileType.INSTANCE) && projectFileIndex.isInSourceContent(file));
      return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests);
    }

    return ProjectScope.getAllScope(getProject());
  }

  @Override
  public boolean ignoreReferencedElementAccessibility() {
    return false;
  }
}

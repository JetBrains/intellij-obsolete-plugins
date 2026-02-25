// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.gspIndex;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.text.StringSearcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterGroovyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GspIncludeIndex extends FileBasedIndexExtension<String, Collection<GspIncludeInfo>> implements DataIndexer<String, Collection<GspIncludeInfo>, FileContent>, DataExternalizer<Collection<GspIncludeInfo>>,
                                                                                                                  FileBasedIndex.FileTypeSpecificInputFilter {

  public static final ID<String, Collection<GspIncludeInfo>> NAME = ID.create("GspIncludeIndex");

  @Override
  public @NotNull ID<String, Collection<GspIncludeInfo>> getName() {
    return NAME;
  }

  @Override
  public @NotNull DataIndexer<String, Collection<GspIncludeInfo>, FileContent> getIndexer() {
    return this;
  }

  @Override
  public boolean acceptInput(@NotNull VirtualFile file) {
    return true;
  }

  @Override
  public void registerFileTypesUsedForIndexing(@NotNull Consumer<? super FileType> fileTypeSink) {
    fileTypeSink.consume(GspFileType.GSP_FILE_TYPE);
    fileTypeSink.consume(GroovyFileType.GROOVY_FILE_TYPE);
  }

  @Override
  public @NotNull KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public @NotNull DataExternalizer<Collection<GspIncludeInfo>> getValueExternalizer() {
    return this;
  }

  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    return this;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 4;
  }

  private static @Nullable GspOuterGroovyElement extractGroovyOuterElement(XmlAttributeValue value) {
    PsiElement leftQuote = value.getFirstChild();
    if (!PsiImplUtil.isLeafElementOfType(leftQuote, GspTokenTypes.GSP_ATTR_VALUE_START_DELIMITER)) return null;

    PsiElement element = leftQuote.getNextSibling();
    
    if (element instanceof GspOuterGroovyElement) {
      if (!PsiImplUtil.isLeafElementOfType(element.getNextSibling(), GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER)) return null;

      return (GspOuterGroovyElement)element;
    }

    if (PsiImplUtil.isLeafElementOfType(element, GspTokenTypes.GEXPR_BEGIN)) {
      PsiElement groovyCodeElement = element.getNextSibling();
      if (!(groovyCodeElement instanceof GspOuterGroovyElement)) return null;
      PsiElement closeBracket = groovyCodeElement.getNextSibling();
      if (!PsiImplUtil.isLeafElementOfType(closeBracket, GspTokenTypes.GEXPR_END)) return null;

      PsiElement valueEndDelimiter = closeBracket.getNextSibling();
      if (PsiImplUtil.isLeafElementOfType(valueEndDelimiter, GspTokenTypes.GSP_ATTR_VALUE_END_DELIMITER)) {
        return (GspOuterGroovyElement)groovyCodeElement;
      }
    }
    
    return null;
  }
  
  @Override
  public @NotNull Map<String, Collection<GspIncludeInfo>> map(@NotNull FileContent inputData) {
    final Map<String, Collection<GspIncludeInfo>> res = new HashMap<>();

    final PsiFile psiFile = inputData.getPsiFile();
    if (psiFile instanceof GspFile) {
      psiFile.accept(new XmlRecursiveElementVisitor() {
        @Override
        public void visitXmlTag(@NotNull XmlTag tag) {
          super.visitXmlTag(tag);

          if (!tag.getName().startsWith("tmpl:")) return;

          ASTNode startTagName = XmlChildRole.START_TAG_NAME_FINDER.findChild(tag.getNode());
          if (startTagName == null) return;

          String fileName = getFileNameByElementValue(startTagName.getText().substring("tmpl:".length()));
          if (fileName.isEmpty()) return;

          fileName = '_' + fileName;

          XmlAttribute[] attributes = tag.getAttributes();
          String[] attrNames = new String[attributes.length];

          for (int i = 0; i < attributes.length; i++) {
            attrNames[i] = attributes[i].getName();
          }

          add(res, fileName, new GspIncludeInfo(startTagName.getStartOffset(), attrNames));
        }

        @Override
        public void visitXmlAttribute(@NotNull XmlAttribute modelAttribute) {
          if (!"model".equals(modelAttribute.getName())) return;

          XmlTag tag = modelAttribute.getParent();

          if (!tag.getName().startsWith("g:")) return;

          boolean isTemplate = false;
          
          XmlAttributeValue viewValue = GrailsPsiUtil.getAttributeValue(tag, "view");
          if (viewValue == null) {
            viewValue = GrailsPsiUtil.getAttributeValue(tag, "template");
            isTemplate = true;
          }

          if (viewValue == null || !GrailsPsiUtil.isSimpleAttribute(viewValue)) return;
          
          String fileName = getFileNameByElementValue(viewValue.getValue());
          if (fileName.isEmpty()) return;
          
          if (isTemplate) {
            fileName = '_' + fileName;
          }

          GrListOrMap map = extractMap(modelAttribute);
          if (map == null) return;

          addModelMap(res, fileName, viewValue, map);
        }
      });

      findModels(((GspFile)psiFile).getGroovyLanguageRoot(), res);
    }
    else if (psiFile instanceof GroovyFileBase) {
      findModels((GroovyFileBase)psiFile, res);
    }

    return res;
  }

  private static void findModels(@NotNull GroovyFileBase file, @NotNull Map<String, Collection<GspIncludeInfo>> res) {
    String text = file.getText();
    new StringSearcher("model", true, true).processOccurrences(text, offset -> {
      PsiElement psi = file.findElementAt(offset);
      GrArgumentLabel label = PsiTreeUtil.getParentOfType(psi, GrArgumentLabel.class);
      if (label != null) {
        processModelCandidate(label, res);
      }
      return true;
    });
  }

  private static void processModelCandidate(@NotNull GrArgumentLabel label, @NotNull Map<String, Collection<GspIncludeInfo>> res) {
    if (!"model".equals(label.getName())) return;

    PsiElement namedArgument = label.getParent();
    if (!(namedArgument instanceof GrNamedArgument)) return;

    GrExpression modelMap = ((GrNamedArgument)namedArgument).getExpression();
    if (!(modelMap instanceof GrListOrMap)) return;

    PsiElement eNamedArgumentOwner = namedArgument.getParent();
    if (!(eNamedArgumentOwner instanceof GrNamedArgumentsOwner owner)) return;

    GrNamedArgument view;
    boolean isTemplate = false;

    view = owner.findNamedArgument("view");
    if (view == null) {
      view = owner.findNamedArgument("template");
      isTemplate = true;
    }

    if (view == null) return;
    GrExpression viewExpression = view.getExpression();
    if (!(viewExpression instanceof GrLiteralImpl)) return;

    Object value = ((GrLiteralImpl)viewExpression).getValue();
    if (!(value instanceof String)) return;

    String fileName = getFileNameByElementValue((String)value);
    if (fileName.isEmpty()) return;

    if (isTemplate) {
      fileName = '_' + fileName;
    }

    GrMethodCall methodCall = PsiUtil.getMethodCallByNamedParameter((GrNamedArgument)namedArgument);
    if (methodCall == null) return;

    GrExpression invokedExpression = methodCall.getInvokedExpression();
    if (!(invokedExpression instanceof GrReferenceExpression)) return;

    GrExpression qualifier = ((GrReferenceExpression)invokedExpression).getQualifierExpression();
    if (qualifier != null && "g".equals(qualifier.getText())) return;

    addModelMap(res, fileName, viewExpression, (GrListOrMap)modelMap);
  }

  private static String getFileNameByElementValue(String value) {
    String path = StringUtil.trimEnd(value, ".gsp");
    path = path.substring(path.lastIndexOf('/') + 1);
    
    return path.trim();
  }
  
  public static @Nullable GrListOrMap extractMap(@NotNull XmlAttribute attribute) {
    XmlAttributeValue value = attribute.getValueElement();
    if (value == null) return null;

    GspOuterGroovyElement outerGroovyElement = extractGroovyOuterElement(value);
    if (outerGroovyElement == null) return null;

    PsiElement groovyElement = attribute.getContainingFile().getViewProvider().findElementAt(outerGroovyElement.getTextOffset(),
                                                                                             GroovyLanguage.INSTANCE);
    if (groovyElement == null) return null;
    
    if (groovyElement instanceof PsiWhiteSpace) {
      groovyElement = groovyElement.getNextSibling();
    }
    else {
      groovyElement = groovyElement.getParent();
    }
    
    return groovyElement instanceof GrListOrMap ? (GrListOrMap)groovyElement : null;
  }
  
  private static void addModelMap(Map<String, Collection<GspIncludeInfo>> res, String fileName, PsiElement viewElement, @NotNull GrListOrMap modelMap) {
    GrNamedArgument[] namedArguments = modelMap.getNamedArguments();
    if (namedArguments.length == 0) return;

    List<String> arguments = new ArrayList<>(namedArguments.length);

    for (GrNamedArgument namedArgument : namedArguments) {
      String labelName = GrailsPsiUtil.getPlainLabelName(namedArgument);
      if (labelName != null) {
        arguments.add(labelName);
      }
    }

    arguments = ContainerUtil.getFirstItems(arguments, 255); // length of this list stores to 1byte, see save(DataOutput, Collection<GspIncludeInfo>)

    if (arguments.isEmpty()) return;

    GspIncludeInfo includeInfo = new GspIncludeInfo(viewElement.getTextOffset(), ArrayUtil.toStringArray(arguments));

    add(res, fileName, includeInfo);
  }
  
  private static void add(Map<String, Collection<GspIncludeInfo>> res, String fileName, GspIncludeInfo includeInfo) {
    List<GspIncludeInfo> list = (List<GspIncludeInfo>)res.computeIfAbsent(fileName, __ -> new ArrayList<>());

    if (list.size() < 255) { // length of this list stores to 1byte, see save(DataOutput, Collection<GspIncludeInfo>)
      list.add(includeInfo);
    }
  }
  
  @Override
  public void save(@NotNull DataOutput out, Collection<GspIncludeInfo> value) throws IOException {
    assert value.size() <= 255;
    out.writeByte(value.size());

    for (GspIncludeInfo aValue : value) {
      out.writeInt(aValue.getOffset());
      String[] argumentNames = aValue.getNamedArguments();
      assert argumentNames.length <= 255;
      out.writeByte(argumentNames.length);
      for (String s : argumentNames) {
        out.writeUTF(s);
      }
    }
  }

  @Override
  public Collection<GspIncludeInfo> read(@NotNull DataInput in) throws IOException {
    int length = in.readUnsignedByte();
    
    Collection<GspIncludeInfo> res = new ArrayList<>(length);
    
    for (int i = 0; i < length; i++) {
      int offset = in.readInt();

      int namedArgumentCount = in.readUnsignedByte();
      
      String[] namedArguments = new String[namedArgumentCount];
      for (int k = 0; k < namedArgumentCount; k++) {
        namedArguments[k] = in.readUTF();
      }

      res.add(new GspIncludeInfo(offset, namedArguments));
    }
    
    return res;
  }

}

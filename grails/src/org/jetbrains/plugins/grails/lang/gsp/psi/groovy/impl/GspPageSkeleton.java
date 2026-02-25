// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SSBasedInspection")
public final class GspPageSkeleton {
  private IntArrayList defOffsets;
  private List<DefTagDescription> defTags;

  private final IntArrayList offsetsEach = new IntArrayList();
  private final List<TagDescription> eachTag = new ArrayList<>();

  GspPageSkeleton(GspGroovyFile gspGroovyFile) {
    processTag(gspGroovyFile.getGspLanguageRoot().getRootTag(), 0, null);
  }

  public boolean processElementAtOffset(int offset, @NotNull PsiScopeProcessor processor,
                                     @Nullable String name, @NotNull ResolveState state) {
    // Process variables defined in each like tags.
    TagDescription tagDescription = getTagDescriptionByOffset(offset);

    for (; tagDescription != null; tagDescription = tagDescription.parent) {
      if (!tagDescription.process(processor, name, state)) return false;
    }

    // Process variables defined in g:set tag.
    if (defTags != null) {
      int index = IntArrays.binarySearch(defOffsets.elements(), 0, defOffsets.size(), offset);
      if (index < 0) {
        index = -(index + 1);
      }

      for (int i = index - 1; i >= 0; i--) {
        DefTagDescription description = defTags.get(i);
        if (name == null || name.equals(description.name)) {
          if (!processor.execute(description.getVariable(), state)) return false;
        }
      }
    }

    return true;
  }

  public @Nullable PsiVariable getVariableByDefTag(GspTag tag) {
    if (defTags == null) return null;

    for (DefTagDescription description : defTags) {
      if (description.tag == tag) {
        return description.getVariable();
      }
    }

    return null;
  }

  public @Nullable EachTagDescription getEachTagDescription(GspTag tag) {
    assert tag.getName().equals("g:each");

    for (TagDescription description : eachTag) {
      if (description != null && description.tag == tag) {
        return (EachTagDescription)description;
      }
    }

    return null;
  }

  private void processTag(GspTag tag, int offset, TagDescription parentTagDescription) {
    String tagName = tag.getName();

    PsiElement e = tag.getFirstChild();

    TagDescription descr = parentTagDescription;

    if (tagName.equals("g:findAll") || tagName.equals("g:collect")) {
      descr = new NonEachTagDescription(tag, parentTagDescription);

      for (; e != null; e = e.getNextSibling()) {
        if (e instanceof XmlAttribute) {
          if (((XmlAttribute)e).getName().equals("expr")) {
            int attrStart = offset + e.getStartOffsetInParent();
            offsetsEach.add(attrStart);
            eachTag.add(descr);

            offsetsEach.add(attrStart + e.getTextLength() - 1);
            eachTag.add(parentTagDescription);
          }
        }
        else if (e instanceof XmlToken && ((XmlToken)e).getTokenType() == XmlTokenType.XML_TAG_END) {
          offsetsEach.add(offset + e.getStartOffsetInParent());
          eachTag.add(descr);
          break;
        }
      }
    }
    else {
      boolean isEach;
      if ((isEach = tagName.equals("g:each")) || tagName.equals("g:grep")) {
        descr = isEach ? new EachTagDescription(tag, parentTagDescription) : new NonEachTagDescription(tag, parentTagDescription);

        for (; e != null; e = e.getNextSibling()) {
          if (e instanceof XmlToken && ((XmlToken)e).getTokenType() == XmlTokenType.XML_TAG_END) {
            offsetsEach.add(offset + e.getStartOffsetInParent());
            eachTag.add(descr);
            break;
          }
        }
      }
    }

    for (; e != null; e = e.getNextSibling()) {
      if (e instanceof GspTag) {
        processTag((GspTag)e, offset + e.getStartOffsetInParent(), descr);
      }
    }

    if (descr != parentTagDescription) {
      offsetsEach.add(offset + tag.getTextLength() - 1);
      eachTag.add(parentTagDescription);
    }

    if (tagName.equals("g:set")) {
      XmlAttributeValue varAttr = GrailsPsiUtil.getAttributeValue(tag, "var");
      if (varAttr != null) {
        if (GrailsPsiUtil.isSimpleAttribute(varAttr)) {
          String defName = varAttr.getValue().trim();
          if (StringUtil.isJavaIdentifier(defName)) {
            if (defTags == null) {
              defOffsets = new IntArrayList();
              defTags = new ArrayList<>();
            }

            defOffsets.add(offset + tag.getTextLength() - 1);
            defTags.add(new DefTagDescription(tag, defName));
          }
        }
      }
    }
  }

  private @Nullable TagDescription getTagDescriptionByOffset(int offset) {
    int index = IntArrays.binarySearch(offsetsEach.elements(), 0, offsetsEach.size(), offset);
    if (index >= 0) {
      return eachTag.get(index);
    }

    index = -(index + 1) - 1;
    if (index == -1) return null;
    return eachTag.get(index);
  }

  static class DefTagDescription {
    public final GspTag tag;
    public final String name;
    private PsiVariable variable;

    DefTagDescription(GspTag tag, String name) {
      this.tag = tag;
      this.name = name;
    }

    public PsiVariable getVariable() {
      if (variable == null) {
        XmlAttributeValue valueAttr = GrailsPsiUtil.getAttributeValue(tag, "value");

        PsiType type = null;

        if (valueAttr != null) {
          type = GrailsPsiUtil.getAttributeExpressionType(valueAttr);
        }

        if (type == null) {
          type = PsiType.getJavaLangString(tag.getManager(), tag.getResolveScope());
        }

        variable = new GrLightVariable(tag.getManager(), name, type, GrailsPsiUtil.getAttributeValue(tag, "var"));
      }

      return variable;
    }
  }

  public abstract static class TagDescription {
    public final GspTag tag;
    public final TagDescription parent;

    protected TagDescription(GspTag tag, TagDescription parent) {
      this.tag = tag;
      this.parent = parent;
    }

    public abstract boolean process(@NotNull PsiScopeProcessor processor,
                        @Nullable String name,
                        @NotNull ResolveState state);
  }

  public static class NonEachTagDescription extends TagDescription {

    protected volatile boolean isVariableInit;
    protected PsiVariable variable;

    protected NonEachTagDescription(GspTag tag, TagDescription parent) {
      super(tag, parent);
    }

    @Override
    public boolean process(@NotNull PsiScopeProcessor processor,
                           @Nullable String name,
                           @NotNull ResolveState state) {
      if (name == null || name.equals("it")) {
        if (!isVariableInit) {
          XmlAttributeValue attrIn = GrailsPsiUtil.getAttributeValue(tag, "in");
          if (attrIn != null) {
            PsiType typeElement = null;

            PsiType typeCollection = GrailsPsiUtil.getAttributeExpressionType(attrIn);
            if (typeCollection != null) {
              typeElement = GrailsPsiUtil.getElementTypeByCollectionType(typeCollection, tag.getProject(), tag.getResolveScope());
            }

            if (typeElement == null) typeElement = TypesUtil.getJavaLangObject(tag);

            variable = new GrLightVariable(tag.getManager(), "it", typeElement, attrIn);
          }

          isVariableInit = true;
        }

        if (variable != null) {
          if (!processor.execute(variable, state)) return false;
        }
      }

      return true;
    }
  }

  public static class EachTagDescription extends TagDescription {

    private PsiVariable varVariable;
    private PsiVariable statusVariable;

    private volatile boolean isNameInit;

    private PsiElement varNavigationElement;
    private String varName;

    protected EachTagDescription(GspTag tag, TagDescription parent) {
      super(tag, parent);
    }

    private void ensureInit() {
      if (!isNameInit) {
        XmlAttributeValue attrVar = GrailsPsiUtil.getAttributeValue(tag, "var");
        if (attrVar == null) {
          varName = "it";
        }
        else {
          if (GrailsPsiUtil.isSimpleAttribute(attrVar)) {
            String nameVar = attrVar.getValue().trim();
            if (StringUtil.isJavaIdentifier(nameVar)) {
              this.varName = nameVar;
              varNavigationElement = attrVar;
            }
          }
        }

        XmlAttributeValue statusAttr = GrailsPsiUtil.getAttributeValue(tag, "status");
        if (statusAttr != null) {
          if (GrailsPsiUtil.isSimpleAttribute(statusAttr)) {
            String nameStatus = statusAttr.getValue().trim();
            if (StringUtil.isJavaIdentifier(nameStatus)) {
              statusVariable = new GrLightVariable(tag.getManager(), nameStatus, CommonClassNames.JAVA_LANG_INTEGER, statusAttr);
            }
          }
        }

        isNameInit = true;
      }
    }

    @Override
    public boolean process(@NotNull PsiScopeProcessor processor, @Nullable String name, @NotNull ResolveState state) {
      ensureInit();

      if (varName != null) {
        if (name == null || name.equals(varName)) {
          PsiVariable varVariable = getVarVariable();

          if (varVariable != null) {
            if (!processor.execute(varVariable, state)) return false;
          }
        }
      }

      PsiVariable statusVariable = getStatusVariable();
      if (statusVariable != null) {
        if (name == null || name.equals(statusVariable.getName())) {
          if (!processor.execute(statusVariable, state)) return false;
        }
      }

      return true;
    }

    public @Nullable PsiVariable getVarVariable() {
      ensureInit();

      if (varVariable != null) return varVariable;

      if (varName == null) return null;

      PsiType typeElement = null;
      XmlAttributeValue attrIn = GrailsPsiUtil.getAttributeValue(tag, "in");
      if (attrIn != null) {
        PsiType typeCollection = GrailsPsiUtil.getAttributeExpressionType(attrIn);
        if (typeCollection != null) {
          typeElement = GrailsPsiUtil.getElementTypeByCollectionType(typeCollection, tag.getProject(), tag.getResolveScope());
        }
      }

      if (typeElement == null) {
        typeElement = TypesUtil.getJavaLangObject(tag);
      }

      varVariable = new GrLightVariable(tag.getManager(), varName, typeElement,
                                      varNavigationElement != null ? varNavigationElement : (attrIn == null ? tag : attrIn));
      return varVariable;
    }

    public PsiVariable getStatusVariable() {
      ensureInit();
      return statusVariable;
    }
  }

}

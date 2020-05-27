package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil;
import com.intellij.lang.javascript.linter.jshint.config.JSHintOptionCompletionObject;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.Convertor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Irina.Chernushina on 10/9/2014.
 */
public class JscsConfigFileCompletionContributor extends CompletionContributor {
  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    final PsiElement position = parameters.getPosition();
    final PsiFile containingFile = position.getContainingFile();
    if (containingFile == null) return;
    final PsiFile originalFile = containingFile.getOriginalFile();
    final VirtualFile vFile = originalFile.getViewProvider().getVirtualFile();
    if (FileTypeRegistry.getInstance().isFileOfType(vFile, JscsConfigFileType.INSTANCE)) {
      final PsiElement positionParent = position.getParent();
      final boolean insideStringLiteral = JSLinterConfigFileUtil.isStringLiteral(positionParent);
      final Convertor<String, String> valueConvertor = o -> {
        if (insideStringLiteral) {
          return o;
        } else {
          return StringUtil.wrapWithDoubleQuote(o);
        }
      };

      final JsonPosition jsonPosition = findPosition(position);
      final List<String> propChain = jsonPosition.getPropChain();
      if (propChain.isEmpty()) {
        return;
      }

      final JscsOptionDescriptor descriptor = findOptionDescriptor(jsonPosition, !jsonPosition.isValue());
      final boolean topLevel = jsonPosition.getPropChain().size() == 1;
      if (jsonPosition.isValue()) {
        if (topLevel) {
          final JscsOption jscsOption = JscsOption.safeValueOf(propChain.get(0));
          if (jscsOption == null) return;
          fillVariantsInsideValue(parameters, result, valueConvertor, jscsOption.getDescriptor());
        } else if (descriptor != null) {
          fillVariantsInsideValue(parameters, result, valueConvertor, descriptor);
        }
      } else {
        if (topLevel) {
          completePropertyNames(result, valueConvertor);
        } else if (descriptor != null) {
          final JscsOptionDescriptor.ValueDescription description = descriptor.getTypes().get(ValueType.obj);
          if (description instanceof JscsOptionDescriptor.ValuesObject) {
            final Set<Map.Entry<String, JscsOptionDescriptor>> entries =
              ((JscsOptionDescriptor.ValuesObject)description).getFields().entrySet();
            for (Map.Entry<String, JscsOptionDescriptor> entry : entries) {
              addPropertyNameCompletionVariant(result, valueConvertor, entry.getKey(), entry.getValue().getDescription());
            }
          }
        }
      }
    }
  }

  private static void fillVariantsInsideValue(CompletionParameters parameters, CompletionResultSet result,
                                              Convertor<String, String> valueConvertor, JscsOptionDescriptor descriptor) {
    final List<String> variants = new ArrayList<>();
    final PsiElement position = parameters.getPosition();
    final PsiElement positionParent = position.getParent();
    final boolean insideStringLiteral = JSLinterConfigFileUtil.isStringLiteral(positionParent);
    final boolean isInArray = JSLinterConfigFileUtil.isArray(positionParent.getParent()) ||
                              insideStringLiteral && JSLinterConfigFileUtil.isArray(positionParent.getParent().getParent());

    final JscsOptionDescriptor.ValueDescription strDescr = descriptor.getTypes().get(ValueType.str);
    final JscsOptionDescriptor.ValueDescription arrDescr = descriptor.getTypes().get(ValueType.array);
    if (strDescr instanceof JscsOptionDescriptor.ValuesCollection) {
      addCollectionsVariants(valueConvertor, variants, (JscsOptionDescriptor.ValuesCollection)strDescr);
    }
    if (arrDescr instanceof JscsOptionDescriptor.ValuesCollection) {
      addCollectionsVariants(valueConvertor, variants, (JscsOptionDescriptor.ValuesCollection)arrDescr);
    }
    if (! insideStringLiteral && ! isInArray && descriptor.getTypes().containsKey(ValueType.bool)) {
      variants.add(Boolean.TRUE.toString());
    }
    if (! insideStringLiteral && ! isInArray && descriptor.getTypes().containsKey(ValueType.true_or_false)) {
      variants.add(Boolean.TRUE.toString());
      variants.add(Boolean.FALSE.toString());
    }
    for (String variant : variants) {
      LookupElementBuilder builder = LookupElementBuilder.create(variant);
      result.addElement(builder);
    }
    JSLinterConfigFileUtil.skipOtherCompletionContributors(parameters, result);
  }

  private static void addCollectionsVariants(Convertor<String, String> valueConvertor,
                                             List<String> variants,
                                             JscsOptionDescriptor.ValuesCollection strDescr) {
    final String[] values = strDescr.getArrValues();
    if (values != null) {
      for (String value : values) {
        value = "\t".equals(value) ? "\\t" : value;
        variants.add(valueConvertor.convert(value));
      }
    }
  }

  private static void completePropertyNames(CompletionResultSet result, Convertor<String, String> valueConvertor) {
    for (JscsOption option : JscsOption.values()) {
      addPropertyNameCompletionVariant(result, valueConvertor, option.name(), option.getDescription());
    }
  }

  private static void addPropertyNameCompletionVariant(@NotNull CompletionResultSet result,
                                                       Convertor<String, String> valueConvertor, String variant,
                                                       @Nullable final String description) {
    final String lookupString = valueConvertor.convert(variant);
    LookupElementBuilder builder = LookupElementBuilder.create(new JSHintOptionCompletionObject(lookupString), lookupString);
    if (description != null) {
      builder = builder.withRenderer(new LookupElementRenderer<LookupElement>() {
        @Override
        public void renderElement(LookupElement element, LookupElementPresentation presentation) {
          presentation.setItemText(element.getLookupString());
          presentation.setTypeGrayed(true);
          presentation.setTypeText(description);
        }
      });
    }
    result.addElement(builder);
  }

  private static JscsOptionDescriptor findOptionDescriptor(final JsonPosition position, boolean forParent) {
    final List<String> chain = position.getPropChain();
    if (chain.isEmpty()) return null;
    if (chain.size() == 1) return null;

    final JscsOption option = JscsOption.safeValueOf(chain.get(0));
    if (option == null) return null;

    JscsOptionDescriptor descriptor = option.getDescriptor();
    JscsOptionDescriptor.ValueDescription description = option.getDescriptor().getTypes().get(ValueType.obj);
    int idx = 1;
    final int bound = forParent ? (chain.size() - 1) : chain.size();
    while (idx < bound) {
      if (! (description instanceof JscsOptionDescriptor.ValuesObject)) return null;
      descriptor = ((JscsOptionDescriptor.ValuesObject)description).getFields().get(chain.get(idx));
      if (descriptor == null) return null;
      description = descriptor.getTypes().get(ValueType.obj);
      ++ idx;
    }
    return descriptor;
  }

  private static JsonPosition findPosition(PsiElement element) {
    PsiElement position = element;
    final JsonStringLiteral value = PsiTreeUtil.getParentOfType(element, JsonStringLiteral.class, false);
    final JsonProperty firstProp = PsiTreeUtil.getParentOfType(element, JsonProperty.class, false);
    boolean isValue  = firstProp != null && (value == null && firstProp.getNameElement() != element.getParent() ||
                                             value != null && value != firstProp.getNameElement());
    final List<String> names = new ArrayList<>();
    while (position != null) {
      final JsonProperty property = JSLinterConfigFileUtil.getProperty(position.getParent());
      if (property == null) break;
      names.add(property.getName());
      position = property;
    }
    Collections.reverse(names);
    return new JsonPosition(names, isValue);
  }

  private static class JsonPosition {
    private final List<String> myPropChain;
    private final boolean myIsValue;

    JsonPosition(List<String> propChain, boolean isValue) {
      myPropChain = propChain;
      myIsValue = isValue;
    }

    public List<String> getPropChain() {
      return myPropChain;
    }

    public boolean isValue() {
      return myIsValue;
    }
  }
}

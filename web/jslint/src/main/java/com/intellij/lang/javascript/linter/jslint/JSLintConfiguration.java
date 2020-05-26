package com.intellij.lang.javascript.linter.jslint;

import com.intellij.lang.javascript.linter.JSLinterConfiguration;
import com.intellij.lang.javascript.linter.JSLinterInspection;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@State(name = "JSLintConfiguration", storages = @Storage("jsLinters/jslint.xml"))
public class JSLintConfiguration extends JSLinterConfiguration<JSLintState> {

  private static final String IS_JSON_VALIDATED_ATTRIBUTE_NAME = "json";
  private static final String ROOT_ELEMENT_NAME = "jslint";
  private static final String OPTION_ELEMENT_NAME = "option";
  private static final JSLintState DEFAULT_STATE = new JSLintState.Builder()
    .setOptionsState(
      new JSLintOptionsState.Builder()
        .put(JSLintOption.MAXERR, 50)
        .build()
    ).build();

  public JSLintConfiguration(@NotNull Project project) {
    super(project);
  }

  @Override
  protected void savePrivateSettings(@NotNull JSLintState state) {
  }

  @NotNull
  @Override
  protected JSLintState loadPrivateSettings(@NotNull JSLintState state) {
    return state;
  }

  @NotNull
  public static JSLintConfiguration getInstance(@NotNull Project project) {
    return getInstance(project, JSLintConfiguration.class);
  }

  @NotNull
  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return JSLintInspection.class;
  }

  @NotNull
  @Override
  protected Element toXml(@NotNull JSLintState state) {
    Element root = new Element(ROOT_ELEMENT_NAME);
    addBooleanAttribute(root, state.isValidateJson(), IS_JSON_VALIDATED_ATTRIBUTE_NAME);
    JSLintOptionsState optionsState = state.getOptionsState();
    for (JSLintOption option : optionsState.getOptions()) {
      Object value = optionsState.getValue(option);
      if (value != null) {
        Element child = new Element(OPTION_ELEMENT_NAME);
        child.setAttribute(option.getOptionName(), value.toString());
        root.addContent(child);
      }
    }
    return root;
  }

  @NotNull
  @Override
  protected JSLintState fromXml(@NotNull Element element) {
    JSLintState.Builder builder = new JSLintState.Builder();
    builder.setValidateJson(getBooleanAttrValue(element, IS_JSON_VALIDATED_ATTRIBUTE_NAME));
    JSLintOptionsState optionsState = loadOptionsValues(element.getChildren());
    builder.setOptionsState(optionsState);
    return builder.build();
  }

  @NotNull
  private static JSLintOptionsState loadOptionsValues(@NotNull List<Element> optionsElements) {
    JSLintOptionsState.Builder optionsValuesBuilder = new JSLintOptionsState.Builder();
    for (Element child : optionsElements) {
      if (OPTION_ELEMENT_NAME.equals(child.getName())) {
        List<Attribute> attributes = child.getAttributes();
        for (Attribute attribute : attributes) {
          String optionName = attribute.getName();
          String valueStr = StringUtil.notNullize(attribute.getValue());
          JSLintOption option = JSLintOption.findByName(optionName);
          if (option != null) {
            Object value = option.getType().createObject(valueStr);
            if (value != null) {
              optionsValuesBuilder.put(option, value);
            }
          }
        }
      }
    }
    return optionsValuesBuilder.build();
  }

  private static void addBooleanAttribute(@NotNull Element element, boolean value, @NotNull String key) {
    if (value) {
      element.setAttribute(key, Boolean.TRUE.toString());
    }
  }

  private static boolean getBooleanAttrValue(@NotNull Element element, @NotNull String attrName) {
    String value = element.getAttributeValue(attrName);
    return Boolean.parseBoolean(value);
  }

  @NotNull
  @Override
  protected JSLintState getDefaultState() {
    return DEFAULT_STATE;
  }

}

package ppp.client.parameters;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.uibinder.client.UiConstructor;

public class MyParametrizedWidget extends Widget {
  private String myProperty;

  @UiConstructor
  public MyParametrizedWidget(String i<caret>nitializer) {
    myProperty = initializer;
  }
}
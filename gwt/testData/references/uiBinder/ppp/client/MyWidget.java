package ppp.client;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.uibinder.client.UiConstructor;

public class MyWidget extends Widget {
  private String myProperty;
  private MyDirection myDirection;

  @UiConstructor
  public MyWidget(String initializer) {
    myProperty = initializer;
  }

  public MyWidget(String initializer2, int initializer3) {
    myProperty = initializer;
  }

  public void setMyProperty(String property) {
    myProperty = property;
  }

  public void setDirection(MyDirection direction) {
    myDirection = direction;
  }

  public void setIntProperty(Integer value) {}
}
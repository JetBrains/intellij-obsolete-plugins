package ppp.client.uiTemplate.rename;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.*;

public class MyComponentImpl {
  @UiTemplate("MyComponentXXX.ui.xml")
  interface MyUiBinder extends UiBinder<DivElement, MyComponentImpl> {
  }
  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  public MyDivObject() {
  }
}
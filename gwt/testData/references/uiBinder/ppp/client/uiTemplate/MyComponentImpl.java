package ppp.client.uiTemplate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.Button;

public class MyComponentImpl {
  @UiTemplate("MyComponent.ui.xml")
  interface MyUiBinder extends UiBinder<DivElement, MyComponentImpl> {
  }
  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);
  @UiField SpanElement nameSpan;
  @UiField Button myButton;

  public MyDivObject() {
  }

  @UiHandler("myBut<caret>t")
  public void handleClick() {
  }
}
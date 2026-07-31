package ppp.client.inheritance;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;

public class MyComponent extends MyBaseComponent {
  interface MyUiBinder extends UiBinder<DivElement, MyComponent> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  public MyDivObject() {
    myElement = myBinder.createAndBindUi(this);
    nameSpan.setInnerText("World");
  }

  @UiHandler("myBut<caret>tonField")
  public void handleClick() {
  }
}
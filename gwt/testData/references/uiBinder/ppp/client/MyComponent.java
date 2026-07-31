package ppp.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;

public class MyComponent {
  interface MyUiBinder extends UiBinder<DivElement, MyComponent> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);
  @UiField SpanElement nameSpan;
  @UiField Button myButton;

  public MyDivObject() {
    myElement = myBinder.createAndBindUi(this);
    nameSpan.setInnerText("World");
  }

  @UiHandler("myBut<caret>t")
  public void handleClick() {
  }
}
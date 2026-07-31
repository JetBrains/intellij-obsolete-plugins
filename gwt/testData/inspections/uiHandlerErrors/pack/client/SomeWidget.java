package pack.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class SomeWidget {
  interface MyUiBinder extends UiBinder<DivElement, SomeWidget> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);
  @UiField Button myButton;

  public MyDivObject() {
    myElement = myBinder.createAndBindUi(this);
    nameSpan.setInnerText("World");
  }

  @UiHandler("unresolvedField")
  void handleClick1(ClickEvent e) { }

  @UiHandler("myButton")
  private void handleClick2(ClickEvent e) { }

  @UiHandler("myButton")
  public void handleClick3(ClickEvent e, int i) { }

  @UiHandler("myButton")
  public void handleClick4(Object o) { }

  @UiHandler("myButton")
  public void handleClick5(ScrollEvent e) { }

  @UiHandler("myButton")
  public void handleClick5(MouseEvent e) { }

  @UiHandler("myButton")
  public void correctHandler(ClickEvent e) {}

  @UiField TabLayoutPanel tabLayoutPanel;
  @UiHandler("tabLayoutPanel")
  public void genericHandler(SelectionEvent<Integer> event) {}

  @UiHandler("fieldFromXml")
  public void correctHandler(ClickEvent e) {}

  @UiField TextBox textBox;
  @UiHandler("textBox")
  public void handleChange(ValueChangeEvent<String> event) {
  }

  @UiField HasValue<String> stringHolder;
  @UiHandler("stringHolder")
  void onStringChanged(ValueChangeEvent<String> event) {
  }
}
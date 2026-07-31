package pack.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;

public class SomeWidget {
  interface MyUiBinder extends UiBinder<DivElement, SomeWidget> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);
  @UiField Button myNotProvidedButton;
  @UiField(provided = true) Button myProvidedButton;
  @UiField(provided = false) Button myProvidedFalseButton;
  Button myNonUiField;

  public MyDivObject() {
    myElement = myBinder.createAndBindUi(this);
    myNotProvidedButton = new Button();
    myProvidedButton = new Button();
    myProvidedFalseButton = new Button();
    myNonUiFieldButton = new Button();
    myUnresolved = new Button();
  }
}
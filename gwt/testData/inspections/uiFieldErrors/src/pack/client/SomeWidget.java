package pack.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class SomeWidget extends BaseWidget {
  interface MyUiBinder extends UiBinder<DivElement, SomeWidget> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);
  SpanElement fieldWithoutAnnotation;
  private @UiField SpanElement privateField;
  @UiField Button fieldWithWrongType;
  @UiField SpanElement notBoundField;
  @UiField SpanElement duplicatedField;

  @UiField Button myButton;

  @UiField(provided = true) Button myProvidedFieldOfSubType;
  @UiField Button myIncorrectFieldOfSubType;
  @UiField IsWidget myFieldOfSuperType;
  @UiField(provided = true) IsWidget myIncorrectProvidedFieldOfSuperType;

  @UiField Element elementWithoutSpecialClass;

  public SomeWidget() {
    myElement = myBinder.createAndBindUi(this);
    nameSpan.setInnerText("World");
  }
}
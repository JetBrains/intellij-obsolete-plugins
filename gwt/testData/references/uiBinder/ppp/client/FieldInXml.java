package ppp.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.*;

public class FieldInXml {
  interface MyUiBinder extends UiBinder<DivElement, FieldInXml> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  public FieldInXml() {
    myElement = myBinder.createAndBindUi(this);
  }

  @UiHandler("spa<caret>n")
  public void handleClick() {
  }
}
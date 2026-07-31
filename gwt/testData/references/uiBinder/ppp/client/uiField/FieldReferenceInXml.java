package ppp.client.uiField;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.*;

public class FieldReferenceInXml {
  interface MyUiBinder extends UiBinder<HTMLPanel, FieldReferenceInXml> {
  }

  private static MyUiBinder myBinder = GWT.create(FieldReferenceInXml.class);

  public FieldReferenceInXml() {
    myElement = myBinder.createAndBindUi(this);
  }
}
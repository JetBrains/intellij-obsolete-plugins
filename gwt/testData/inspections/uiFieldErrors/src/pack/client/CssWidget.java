package pack.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;

public class CssWidget {
  interface MyUiBinder extends UiBinder<DivElement, CssWidget> {
  }
  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  @UiField MyStyle typedStyle;

  @UiField MyStyle style;

  interface MyStyle extends CssResource {
  }
}
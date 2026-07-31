package xxx.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;

public class LazyDomElementComponent extends Composite {
  interface MyUiBinder extends UiBinder<Widget, LazyDomElementComponent> {
  }
  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  @UiField LazyDomElement<SpanElement> nameSpan;

  public LazyDomElementComponent() {
    initWidget(myBinder.createAndBindUi(this));
  }
}
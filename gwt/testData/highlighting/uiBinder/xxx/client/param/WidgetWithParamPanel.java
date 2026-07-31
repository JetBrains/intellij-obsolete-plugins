package xxx.client.param;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;

public class WidgetWithParamPanel extends Composite {
  interface MyUiBinder extends UiBinder<Widget, WidgetWithParamPanel> {
  }
  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  @UiField WidgetWithParam regular;
  @UiField(provided = true) WidgetWithParam provided;

  public WidgetWithParamPanel() {
    initWidget(myBinder.createAndBindUi(this));
  }
}
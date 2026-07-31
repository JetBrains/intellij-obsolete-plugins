package xxx.client.param;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;

public class WidgetWithParam extends Composite {
  interface Binder extends UiBinder<Widget, WidgetWithParam> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiConstructor
  public WidgetWithParam(String param) {
  }
}
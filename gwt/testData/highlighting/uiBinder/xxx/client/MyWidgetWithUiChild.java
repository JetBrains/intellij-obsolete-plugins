package xxx.client;

import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.user.client.ui.Widget;

public class MyWidgetWithUiChild extends Widget {
  @UiChild
  public void addInner(MyWidget inner) {
  }

  @UiChild
  public void addParam(MyWidget inner, String x) {
  }
}
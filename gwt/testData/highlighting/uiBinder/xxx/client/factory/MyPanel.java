package xxx.client.factory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MyPanel extends Composite {
  interface MyUiBinder extends UiBinder<Widget, MyPanel> {
  }

  private static final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  @UiField
  Data data;

  public UserDashboard() {
      initWidget(uiBinder.createAndBindUi(this));
  }

  @UiFactory
  public Data createData(String param) {
      return new Data(param);
  }

}
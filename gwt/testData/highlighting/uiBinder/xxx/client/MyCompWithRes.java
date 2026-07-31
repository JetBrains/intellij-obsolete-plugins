package xxx.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class MyCompWithRes extends Composite {
  interface MyUiBinder extends UiBinder<Widget, MyCompWithRes> {
  }

  private static MyUiBinder myBinder = GWT.create(MyUiBinder.class);

  @UiField SpanElement nameSpan;
  @UiField(provided = true)
  final Resources res;

  public MyCompWithRes(Resources resources) {
    this.res = resources;
    initWidget(myBinder.createAndBindUi(this));
  }

}
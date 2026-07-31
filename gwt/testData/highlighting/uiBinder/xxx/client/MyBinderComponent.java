package xxx.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MyBinderComponent extends Composite {

  interface MyBinderComponentUiBinder extends UiBinder<Widget, MyBinderComponent> {}

  interface <warning descr="Interface 'MyNotImplementedInterface' has no concrete subclass">MyNotImplementedInterface</warning> {}

  private static final MyBinderComponentUiBinder ourUiBinder = GWT.create(MyBinderComponentUiBinder.class);

  public MyBinderComponent() {
    initWidget(ourUiBinder.createAndBindUi(this));
  }
}

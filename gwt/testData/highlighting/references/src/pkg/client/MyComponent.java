package com.test.ui.management.client.personalization;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MyComponent extends Composite {

  @UiTemplate("MyComponent.ui.xml")
  interface MyComponentUiBinder extends UiBinder<Widget, MyComponent> {}

  private static final MyComponentUiBinder ourUiBinder = GWT.create(MyComponentUiBinder.class);

  public MyComponent() {
    initWidget(ourUiBinder.createAndBindUi(this));
  }
}

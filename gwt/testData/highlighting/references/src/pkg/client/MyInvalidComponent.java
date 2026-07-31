package com.test.ui.management.client.personalization;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MyInvalidComponent extends Composite {

  @UiTemplate("MyInvalidComponent.ui.xml")
  interface MyInvalidComponentUiBinder extends UiBinder<Widget, MyInvalidComponent> {}

  private static final MyInvalidComponentUiBinder ourUiBinder = GWT.create(MyInvalidComponentUiBinder.class);

  public MyInvalidComponent() {
    initWidget(ourUiBinder.createAndBindUi(this));
  }
}

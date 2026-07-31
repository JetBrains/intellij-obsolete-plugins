package xxx.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class UiBinderErrorsComponent extends Composite {

  @UiTemplate("UiChildTag.ui.xml")
  interface ValidUiBinder extends UiBinder<HTMLPanel, UiBinderErrorsComponent> {}

  @UiTemplate("UiChildTag.ui.xml")
  interface InvalidRootUiBinder extends UiBinder<<error descr="UiBinder interface parameter isn't consistent with 'g:HTMLPanel' tag. 'com.google.gwt.user.client.ui.HTMLPanel' or its superclass is expected">FlowPanel</error>, UiBinderErrorsComponent> {}

  @UiTemplate("invalidReference")
  interface <error descr="UiBinder interface doesn't have corresponding ui.xml file">NoUiXmlUiBinder</error> extends UiBinder<HTMLPanel, UiBinderErrorsComponent> {}

  @UiTemplate("WithoutRootTag.ui.xml")
  interface AbsentRootUiBinder extends UiBinder<<error descr="Cannot detect root tag in corresponding ui.xml file">HTMLPanel</error>, UiBinderErrorsComponent> {}

  @UiTemplate("WithTwoRootTags.ui.xml")
  interface AmbiguousRootUiBinder extends UiBinder<<error descr="Too many root tags detected in corresponding ui.xml file">HTMLPanel</error>, UiBinderErrorsComponent> {}

  private static final ValidUiBinder ourUiBinder = GWT.create(ValidUiBinder.class);

  public UiBinderErrorsComponent() {
    initWidget(ourUiBinder.createAndBindUi(this));
  }
}

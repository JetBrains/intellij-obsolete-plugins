package xxx.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiRenderer;

public class MyRendererComponent {

  interface MyRendererComponentUiRenderer extends UiRenderer {}

  interface <warning descr="Interface 'MyNotImplementedInterface' has no concrete subclass">MyNotImplementedInterface</warning> {}

  private static final MyRendererComponentUiRenderer ourUiRenderer = GWT.create(MyRendererComponentUiRenderer.class);
}

package ppp.client.uiTemplate;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.uibinder.client.UiTemplate;

public class MyComponentOuterShort extends AbstractCell<String> {

  private static MyShortUiRenderer myRenderer = GWT.create(MyShortUiRenderer.class);

  @Override
  public void render(Context context, String value, SafeHtmlBuilder builder) {
    myRenderer.render(builder, value);
  }
}

@UiTemplate("MyComponent.ui.xml")
interface MyShortUiRenderer extends UiRenderer {
  void render(SafeHtmlBuilder sb, String entity);
}

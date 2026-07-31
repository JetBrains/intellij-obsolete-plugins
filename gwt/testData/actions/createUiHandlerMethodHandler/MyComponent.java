import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class MyComponent extends Composite {

    interface MyComponentUiBinder extends UiBinder<HTMLPanel, MyComponent> {}

    private static MyComponentUiBinder ourUiBinder = GWT.create(MyComponentUiBinder.class);

    @UiField
    Button myButton1;

    @UiField
    Button myButton2;

    <caret>

    public MyComponent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}
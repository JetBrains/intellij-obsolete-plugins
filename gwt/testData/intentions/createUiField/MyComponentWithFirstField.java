import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

class MyComponentWithFirstField extends Composite {

    interface MyComponentWithFirstFieldUiBinder extends UiBinder<HTMLPanel, MyComponentWithFirstField> {}

    private static MyComponentWithFirstFieldUiBinder ourUiBinder = GWT.create(MyComponentWithFirstFieldUiBinder.class);

    @UiField
    Button myButton1;

    public MyComponentWithFirstField() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}
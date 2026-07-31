import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

class MyComponentWithoutFields extends Composite {

    interface MyComponentWithoutFieldsUiBinder extends UiBinder<HTMLPanel, MyComponentWithoutFields> {}

    private static MyComponentWithoutFieldsUiBinder ourUiBinder = GWT.create(MyComponentWithoutFieldsUiBinder.class);
    @UiField
    Button myButton2;

    public MyComponentWithoutFields() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}
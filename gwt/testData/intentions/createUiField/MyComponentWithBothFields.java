import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

class MyComponentWithBothFields extends Composite {

    interface MyComponentWithBothFieldsUiBinder extends UiBinder<HTMLPanel, MyComponentWithBothFields> {}

    private static MyComponentWithBothFieldsUiBinder ourUiBinder = GWT.create(MyComponentWithBothFieldsUiBinder.class);

    @UiField
    Button myButton1;

    @UiField
    Button myButton2;

    public MyComponentWithBothFields() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
}
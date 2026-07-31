import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

class MyComponentWithFirstMethod extends Composite {

    interface MyComponentWithFirstMethodUiBinder extends UiBinder<HTMLPanel, MyComponentWithFirstMethod> {}

    private static MyComponentWithFirstMethodUiBinder ourUiBinder = GWT.create(MyComponentWithFirstMethodUiBinder.class);

    public MyComponentWithFirstMethod() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiHandler("myButton1")
    public void myButton1Click(ClickEvent event) {
    }
}
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

class MyComponentWithBothMethods extends Composite {

    interface MyComponentWithBothMethodsUiBinder extends UiBinder<HTMLPanel, MyComponentWithBothMethods> {}

    private static MyComponentWithBothMethodsUiBinder ourUiBinder = GWT.create(MyComponentWithBothMethodsUiBinder.class);

    public MyComponentWithBothMethods() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiHandler("myButton1")
    public void myButton1Click(ClickEvent event) {
    }

    @UiHandler("myButton2")
    public void myButton2Click(ClickEvent event) {
    }
}
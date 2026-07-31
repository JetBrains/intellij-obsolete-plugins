import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

class MyComponentWithoutMethods extends Composite {

    interface MyComponentWithoutMethodsUiBinder extends UiBinder<HTMLPanel, MyComponentWithoutMethods> {}

    private static MyComponentWithoutMethodsUiBinder ourUiBinder = GWT.create(MyComponentWithoutMethodsUiBinder.class);

    public MyComponentWithoutMethods() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiHandler("myButton2")
    public void myButton2Click(ClickEvent event) {
    }
}
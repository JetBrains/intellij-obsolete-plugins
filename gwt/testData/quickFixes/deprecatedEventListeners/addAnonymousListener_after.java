import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;

public class MyModule {
    public void method() {
        Button b = new Button();
        b.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Window.alert("123");
            }
        });
    }
}

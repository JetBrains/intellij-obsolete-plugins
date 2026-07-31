import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.BlurEvent;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        Button button = new Button("aaa");
        button.addFocusListener(new <caret>FocusListenerAdapter() {
            public void onLostFocus(Widget sender) {
                System.out.println("xxx");
            }
        });
    }
}

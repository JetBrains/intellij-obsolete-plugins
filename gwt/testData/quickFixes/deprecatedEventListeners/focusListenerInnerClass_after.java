import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        Button button = new Button("aaa");
        button.addFocusHandler(new MyFocusListenerAdapter());
    }

    private static class MyFocusListenerAdapter implements FocusHandler {
        public void onFocus(FocusEvent focusEvent) {
            System.out.println("xxx");
        }
    }
}

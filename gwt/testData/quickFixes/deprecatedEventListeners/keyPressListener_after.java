import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        Button button = new Button("aaa");
        button.addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                System.out.println("abcd");
            }
        });
    }
}

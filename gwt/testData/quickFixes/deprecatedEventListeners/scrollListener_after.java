import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.ui.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        ScrollHandler listener = new ScrollHandler() {
            public void onScroll(ScrollEvent scrollEvent) {
                System.out.println("123");
            }
        };
        ScrollPanel panel = new ScrollPanel();
        panel.addScrollHandler(listener);
    }
}

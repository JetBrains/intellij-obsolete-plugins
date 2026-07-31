import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.*;

public class MyModule implements EntryPoint {
    public void onModuleLoad() {
        <caret>ScrollListener listener = new ScrollListener() {
            public void onScroll(Widget widget, int scrollLeft, int scrollTop) {
                System.out.println("123");
            }
        };
        ScrollPanel panel = new ScrollPanel();
        panel.addScrollListener(listener);
    }
}

package client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.CellList;

public interface FromLib extends CellList.Style {
    String childStyle();

    public interface Res extends ClientBundle {
        @Source("fromLib.css")
        FromLib style();
    }
}
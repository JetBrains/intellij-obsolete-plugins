package xxx.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import xxx.client.MyService;

public class MyServiceImpl extends RemoteServiceServlet implements MyService {
  public String calc() {
    return "42";
  }
}
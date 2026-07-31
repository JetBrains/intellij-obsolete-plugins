package server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import client.MyService;

public class MyServiceImpl extends RemoteServiceServlet implements MyService {
  public String calc() {
    return "42";
  }
}
package org.intellij.j2ee.web.resin;

import com.intellij.util.xmlb.annotations.Tag;

public class ResinModelDataBase {

  @Tag("port")
  private int myPort = ResinUtil.DEFAULT_PORT;
  @Tag("mbean-port")
  private int myJmxPort = 9999; // TODO: to const;

  private String myCharset = "";

  public String getCharset() {
    return myCharset;
  }

  public void setCharset(String charset) {
    myCharset = charset;
  }

  public int getPort() {
    return myPort;
  }

  public void setPort(int port) {
    myPort = port;
  }

  public int getJmxPort() {
    return myJmxPort;
  }

  public void setJmxPort(int jmxPort) {
    myJmxPort = jmxPort;
  }
}

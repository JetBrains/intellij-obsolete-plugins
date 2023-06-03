package org.intellij.j2ee.web.resin.resin;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.PathUtil;

public class WebApp {

  private final boolean myDefaultContextPath;
  private String myLocation;
  private String myContextPath;
  private String myHost;
  private String myCharset;

  public WebApp(boolean defaultContextPath, String contextPath, String host, String location, String charset) {
    myDefaultContextPath = defaultContextPath;
    myContextPath = contextPath;
    myHost = host;
    myLocation = location;
    myCharset = charset;
  }

  public String getLocation() {
    return myLocation;
  }

  public void setLocation(String location) {
    this.myLocation = location;
  }

  public String getContextPath() {
    return myDefaultContextPath ? "/" + FileUtilRt.getNameWithoutExtension(PathUtil.getFileName(myLocation)) : myContextPath;
  }

  public void setContextPath(String contextPath) {
    this.myContextPath = contextPath;
  }

  public String getHost() {
    if (myHost == null) return "";
    return myHost;
  }

  public void setHost(String host) {
    this.myHost = host;
  }

  public String getCharSet() {
    return myCharset;
  }

  public void setCharSet(String charset) {
    this.myCharset = charset;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WebApp webApp)) return false;

    if (myContextPath != null ? !myContextPath.equals(webApp.myContextPath) : webApp.myContextPath != null) return false;
    if (myHost != null ? !myHost.equals(webApp.myHost) : webApp.myHost != null) return false;
    if (myLocation != null ? !myLocation.equals(webApp.myLocation) : webApp.myLocation != null) return false;
    if (myDefaultContextPath != webApp.myDefaultContextPath) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myLocation != null ? myLocation.hashCode() : 0);
    result = 29 * result + (myContextPath != null ? myContextPath.hashCode() : 0);
    result = 29 * result + (myHost != null ? myHost.hashCode() : 0);
    result = 29 * result + (myDefaultContextPath ? 1 : 0);
    return result;
  }
}

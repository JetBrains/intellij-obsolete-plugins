package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import com.intellij.cvsSupport2.config.CvsRootConfiguration;
import com.intellij.cvsSupport2.config.ProxySettings;
import com.intellij.cvsSupport2.connections.CvsConnectionSettings;
import com.intellij.cvsSupport2.connections.IDEARootFormatter;
import com.intellij.cvsSupport2.connections.local.LocalConnectionSettings;
import com.intellij.cvsSupport2.connections.login.CvsLoginWorker;
import com.intellij.cvsSupport2.connections.pserver.PServerCvsSettings;
import com.intellij.cvsSupport2.connections.pserver.PServerLoginProvider;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ThreeState;
import org.netbeans.lib.cvsclient.connection.PServerPasswordScrambler;

/**
 * author: lesya
 */
public class CvsRootFormatterTest extends HeavyPlatformTestCase {
  private static final String PASSWORD = "PASSWORD_FROM_PROVIDER";

  private static final PServerLoginProvider ourPasswordProvider = new PServerLoginProvider(){
    @Override
    public String getScrambledPasswordForCvsRoot(String cvsroot) {
      return PASSWORD;
    }

    @Override
    public CvsLoginWorker getLoginWorker(Project project, PServerCvsSettings pServerCvsSettings) {
      return new CvsLoginWorker() {
        @Override
        public boolean promptForPassword() {
          return true;
        }

        @Override
        public ThreeState silentLogin(boolean forceCheck) {
          return ThreeState.YES;
        }

        @Override
        public void goOffline() {
        }
      };
    }
  };

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PServerLoginProvider.registerPasswordProvider(ourPasswordProvider);
  }

  public void testValueOf(){

    int defaultPort = 2401;
    checkPServerSettings(":pserver:user:password@host:repo",
                         "user", "host", defaultPort, PServerPasswordScrambler.getInstance().scramble("password"), "repo");

    checkPServerSettings(":pserver:user@host:repo",
                         "user", "host", defaultPort, PASSWORD, "repo");

    checkPServerSettings(":pserver:user@host:1234:repo",
                         "user", "host", 1234, PASSWORD, "repo");

    checkPServerSettings(":pserver:user@host:1234/repo",
                         "user", "host", 1234, PASSWORD, "/repo");

    checkPServerSettings(":pserver:user@host/repo",
                         "user", "host", defaultPort, PASSWORD, "/repo");

    checkPServerSettings(":pserver:user@host:c:/repo",
                         "user", "host", defaultPort, PASSWORD, "c:/repo");

    checkPServerSettings(":pserver:user@host/repo/", "user", "host", defaultPort, PASSWORD, "/repo");

    checkLocalSettings("c:/repo", "c:/repo");
    checkLocalSettings(":local:c:/repo", "c:/repo");

    checkPServerWithProxySettings(":pserver;proxy=proxyhost;proxyport=8080:user@host/repo/", "user", "host", defaultPort, PASSWORD, "/repo","proxyhost",
                                  "8080");

  }

  public void testSCR3703() {
    checkPServerSettings(":pserver;username=Username;hostname=Hostname:c:/cvs ",
                         "Username", "Hostname", 2401, PASSWORD, "c:/cvs");
  }

  public void testWinCvs() {
    checkWinCvsFormat(":pserver;username=USER_NAME;password=PASSWORD;test;test=;hostname=HOST_NAME;port=1234;proxy=PROXY_NAME;" +
                      "proxyport=PROXY_PORT_NUMBER;tunnel=TUNNEL;proxyuser=PROXY_SERVER_ADDRES;proxypassword=PROXY_PASSWORD:REPOSITORY_PATH",
                      "USER_NAME", "PASSWORD", "HOST_NAME", 1234, "PROXY_NAME", "PROXY_PORT_NUMBER",
                      "TUNNEL", "PROXY_USER", "PROXY_PASSWORD", "REPOSITORY_PATH");

    checkWinCvsFormat(":pserver;username=user:host:/repository",
                      "user", "PASSWORD_FROM_PROVIDER", "host", 2401, null, null, null, null, null, "/repository");
  }

  private static void checkWinCvsFormat(final String cvsRoot,
                                        final String user,
                                        final String password,
                                        final String host,
                                        final int port,
                                        final String proxy,
                                        final String proxyPort,
                                        final String tunnel,
                                        final String proxyUser,
                                        final String proxyPassword,
                                        final String repo) {
    PServerCvsSettings settings = (PServerCvsSettings)createSettingsOn(cvsRoot);

    assertEquals(proxy, settings.PROXY_HOST);
    assertEquals(proxyPort, settings.PROXY_PORT);

    assertEquals(user, settings.USER);
    assertEquals(host, settings.HOST);
    assertEquals(repo, settings.REPOSITORY);
    assertEquals(password, settings.PASSWORD);
    assertEquals(port, settings.PORT);

  }

  private static void checkPServerSettings(String cvsRoot, String user, String host, int port, String password, String repository) {

    PServerCvsSettings settings = (PServerCvsSettings)createSettingsOn(cvsRoot);

    assertEquals(user, settings.USER);
    assertEquals(host, settings.HOST);
    assertEquals(repository, settings.REPOSITORY);
    assertEquals(password, settings.PASSWORD);
    assertEquals(port, settings.PORT);
  }

  private static void checkPServerWithProxySettings(String cvsRoot, String user, String host, int port, String password, String repository,
                                                    String proxyHost, String proxyPort) {
    checkPServerSettings(cvsRoot, user, host, port, password, repository);
    PServerCvsSettings settings = (PServerCvsSettings)createSettingsOn(cvsRoot);

    assertEquals(proxyHost, settings.PROXY_HOST);
    assertEquals(proxyPort, settings.PROXY_PORT);
    ProxySettings proxySettings = settings.getProxySettings();
    assertTrue(proxySettings.USE_PROXY);
    assertEquals(proxyHost, proxySettings.PROXY_HOST);
    assertEquals(Integer.parseInt(proxyPort), proxySettings.PROXY_PORT);

  }

  private static void checkLocalSettings(String cvsRoot, String repository) {
    CvsConnectionSettings settings = createSettingsOn(cvsRoot);
    assertTrue(settings instanceof LocalConnectionSettings);
    assertEquals(repository, settings.REPOSITORY);
  }

  private static CvsConnectionSettings createSettingsOn(String cvsRoot) {
    CvsRootConfiguration cvsRootConfiguration = CvsApplicationLevelConfiguration.createNewConfiguration(CvsApplicationLevelConfiguration.getInstance());
    cvsRootConfiguration.CVS_ROOT = cvsRoot;
    return new IDEARootFormatter(cvsRootConfiguration).createConfiguration();
  }
}

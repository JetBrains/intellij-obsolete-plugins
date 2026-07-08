[![obsolete JetBrains project](https://jb.gg/badges/obsolete.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# IntelliJ IDEA Obsolete Plugins

Over the years, IntelliJ IDEA has accumulated support for a large array of technologies, and many of those technologies are no longer actively maintained. We know that there are still people using those technologies, and up until now we’ve been maintaining plugins for them as part of the main IntelliJ IDEA source repository. However, our project has been growing, and carrying this baggage is getting more and more difficult both for our users (for whom the plugins affect the size of installation and potentially performance) and for our development team. At the same time, we’ve established procedures for maintaining a stable third-party plugin API, so we’re confident that moving plugins out of the main repository will not affect their stability as the IDE evolves.

Starting with IntelliJ IDEA 2019.2, we moved a number of plugins out of the main IntelliJ IDEA source repository to this repository. Those plugins will no longer be bundled with IntelliJ IDEA, and we will no longer update them alongside IntelliJ IDEA releases. However, these plugins currently remain available for download from the JetBrains Marketplace, subject to JetBrains' right to limit or discontinue their availability at any time at its sole discretion.

You are welcome to submit pull requests with fixes and improvements. By submitting a pull request, you agree to license your contribution under an open-source license with no copyleft effect (e.g., MIT, Apache 2.0) and acknowledge that your submission is voluntary and provided without expectation of compensation or reward. JetBrains reserves the right, at its sole discretion, to review, modify, accept, or reject any submissions, and to independently decide whether to release updates incorporating those changes.

### Forward Compatibility and Maintenance

Although these plugins are no longer actively updated alongside IntelliJ IDEA releases, JetBrains currently intends to make commercially reasonable efforts to preserve compatibility with future IDE versions and to address critical issues.

Please note that these plugins are provided strictly on an "AS IS" and "AS AVAILABLE" basis. The statements herein reflect JetBrains' current maintenance intentions only and do not constitute a legally binding obligation, guarantee, warranty, or service-level commitment. While our community is important to us, and we always hope to keep you informed as things change, JetBrains may eventually need to sunset a project, step back from maintenance entirely, or retire a plugin from the Marketplace. If we eventually need to fully retire a plugin or explicitly mark it as unsupported, our standard practice will be to alert users in advance so you have time to prepare.

>[!TIP]
>We understand that supporting an obsolete plugin might be a burden for a customer, too. The [JetBrains Professional Services](https://jb.gg/ps) team offers various services related to JetBrains products, including custom plugin development. Please get in touch with us at ps@jetbrains.com to further discuss how we could help you with your plugin maintenance problems.

This repository includes the following plugins:
  * [J2ME](https://plugins.jetbrains.com/plugin/12318-j2me)
  * [JsTestDriver](https://plugins.jetbrains.com/plugin/4468-jstestdriver-plugin)
  * [Struts 1.x](https://plugins.jetbrains.com/plugin/110-struts-1-x/) (plugin is no longer compatible with IntelliJ IDEA 2020.2 or newer and will not be updated)
  * [Heroku integration](https://plugins.jetbrains.com/plugin/7605-heroku-integration)
  * [RubyMotion support](https://plugins.jetbrains.com/plugin/10674-rubymotion-support)
  * [Compass support](https://plugins.jetbrains.com/plugin/13705-compass)
  * [Generate Ant Build](https://plugins.jetbrains.com/plugin/14169-ant-build-generation)
  * [CVS](https://plugins.jetbrains.com/plugin/10746-cvs)
  * [Old REST Client](https://plugins.jetbrains.com/plugin/16216-old-rest-client)
  * [IDETalk](https://plugins.jetbrains.com/plugin/233-idetalk)
  * [Tapestry](https://plugins.jetbrains.com/plugin/14589-tapestry)
  * [Vaadin](https://plugins.jetbrains.com/plugin/13199-vaadin-6-8)
  * [JBoss Seam](https://plugins.jetbrains.com/plugin/14587-jboss-seam)
  * [dmServer](https://plugins.jetbrains.com/plugin/4542-virgo-dmserver)
  * [struts2](https://plugins.jetbrains.com/plugin/1698-struts-2)
  * [arquillian](https://plugins.jetbrains.com/plugin/16872-arquillian)
  * [jbpm](https://plugins.jetbrains.com/plugin/14588-jboss-jbpm)
  * [Guice](https://plugins.jetbrains.com/plugin/16876-guice)
  * [Spring OSGi](https://plugins.jetbrains.com/plugin/16877-spring-osgi)
  * [Helidon](https://plugins.jetbrains.com/plugin/16874-helidon)
  * [Properties Resource Bundle Editor](https://plugins.jetbrains.com/plugin/17035-resource-bundle-editor)
  * [Grails](https://plugins.jetbrains.com/plugin/18504-grails)
  * [XSLT Debugger](https://plugins.jetbrains.com/plugin/1818-xslt-debugger)
  * [Chef](https://plugins.jetbrains.com/plugin/7548-chef)
  * [Puppet](https://plugins.jetbrains.com/plugin/7180-puppet)
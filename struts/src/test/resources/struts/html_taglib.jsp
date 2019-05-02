<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<%-- forward, action, page, href --%>
<html:link forward="servletForward"/>
<html:link forward="<error>notExisting</error>"/>

<html:link action="/login"/>
<html:link action="/login.do"/>
<html:link action="/login?param=bla"/>
<html:link action="/login?param=${expression}"/>
<html:link action="<error>/notExisting</error>"/>

<html:link page="/index.jsp"/>
<html:link page="/<error>notExisting</error>"/>

<html:link href="http://www.jetbrains.com"/>
<html:link href="/index.jsp"/>
<html:link href="/notExisting.jsp"/>

<%-- <html:javascript> --%>
<html:javascript formName="loginForm"/>
<html:javascript formName="<error>notExisting</error>"/>

<%-- <html:form> focus --%>
<html:form action="/my" focus="username"/>
<html:form action="/my" focus="<error>notExisting</error>"/>
<html:form action="/login" focus="notExisting"/><%-- DynaForm --%>

<html:form action="/myValidatorFormAction">
  <html:text name="bean.field"/>
  <html:text name="bean.notExisting"/>
</html:form>

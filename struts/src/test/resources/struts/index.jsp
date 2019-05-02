<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>
<%@ taglib prefix="html-el" uri="http://struts.apache.org/tags-html-el" %>
<%@ taglib prefix="sslext" uri="sslext" %>

<%@ include file="/WEB-INF/struts-config.xml"%>
<%@ include file="/WEB-INF/validation.xml"%>
<%@ include file="/WEB-INF/tiles-defs.xml"%>
<%@ include file="/WEB-INF/sslext.tld"%>

<html>
<body>

<html:form action="<error>/loginn</error>">
</html:form>

<html:form action="/login">
  <html:text property="username"/>
  <html:text property="password"/>
  <%-- DynaForm properties are soft --%>
  <html:text property="notExisting"/>
</html:form>
<html:form action="/login?parameter=xxx">
  <html:text property="username"/>
  <html:text property="password"/>
  <%-- DynaForm properties are soft --%>
  <html:text property="notExisting"/>
</html:form>
<html:form action="/login#ancor">
  <html:text property="username"/>
  <html:text property="password"/>
  <%-- DynaForm properties are soft --%>
  <html:text property="notExisting"/>
</html:form>

<html:form action="/my">
  <html:text property="username"/>
  <html:text property="password"/>
  <html:text property="<error>notExisting</error>"/>
</html:form>

<html:form action="/my">
  <html:text property="username"/>
  <html:text property="password"/>
  <html:text property="<error>notExisting</error>"/>
</html:form>

<html:form action="/my.do?param=xxx">
  <html:text property="username"/>
  <html:text property="password"/>
  <html:text property="<error>notExisting</error>"/>
</html:form>

<html-el:form action="/login?parameter=xxx">
  <html:text property="username"/>
  <html:text property="password"/>
  <%-- DynaForm properties are soft --%>
  <html:text property="notExisting"/>
</html-el:form>

<sslext:form action="/login?parameter=xxx">
  <html:text property="username"/>
  <html:text property="password"/>
  <%-- DynaForm properties are soft --%>
  <html:text property="notExisting"/>
</sslext:form>


</body>
</html>

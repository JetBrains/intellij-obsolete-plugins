<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<html>
<body>

<html:form action="/action">
  <html:text property="username"/>
  <html:text property="password"/>
  <html:text property="<error>secondUsername</error>"/>
  <html:text property="<error>secondPassword</error>"/>
</html:form>

</body>
</html>

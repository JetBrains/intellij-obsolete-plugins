<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<html>
<body>

<html:form action="/action">
  <html:text property="<error>username</error>"/>
  <html:text property="<error>password</error>"/>
  <html:text property="secondUsername"/>
  <html:text property="secondPassword"/>
</html:form>

<html:img page="/icon.png"/>
<html:img page="/<error>notExisting.png</error>"/>
<html:img module="/second" page="/icon.png"/>
<html:img module="<error>/notExisting</error>" page="/icon.png"/>

<html:img module="/second" page="/<error>root.png</error>"/>
<html:img page="/<error>root.png</error>"/>
<html:img module="/" page="/root.png"/>

<html:link forward="secondForward"/>
<html:link forward="<error>notExistingForward</error>"/>
<html:link module="/second" forward="secondForward"/>
<html:link module="<error>notExisting</error>" forward="secondForward"/>

<html:link forward="<error>rootForward</error>"/>
<html:link module="/second" forward="<error>rootForward</error>"/>
<html:link module="/" forward="rootForward"/>

</body>
</html>

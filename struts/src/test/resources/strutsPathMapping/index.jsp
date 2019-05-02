<%@ taglib prefix="html" uri="http://struts.apache.org/tags-html" %>

<html>
<body>

<html:form action="/login?param=value">
  <html:text property="username"/>
  <html:text property="password"/>  
</html:form>

<!--http://struts.apache.org/1.2.7/userGuide/struts-html.html#form-->
<!--
The URL to which this form will be submitted. This value is also used to select the ActionMapping we are assumed to be processing, from which we can identify the appropriate form bean and scope.
If you are using extension mapping for selecting the controller servlet, this value should be equal to the path attribute of the corresponding <action> element, optionally followed by the correct extension suffix.
If you are using path mapping to select the controller servlet, this value should be exactly equal to the path attribute of the corresponding <action> element.
-->

<html:form action="<error>/do/login</error>">
</html:form>

</body>
</html>


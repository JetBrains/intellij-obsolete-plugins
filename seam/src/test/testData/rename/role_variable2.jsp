<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<f:view>
  <h:form>
    <h:commandButton action="#{blog_role_2}"/>
    <h:commandButton action="#{blog_role_2.something}"/>
  </h:form>
</f:view>
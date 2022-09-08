<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<f:view>
  <h:form>
    <h:commandButton action="#{blog_role_<caret>simple}"/>
    <h:commandButton action="#{blog_role_simple.something}"/>
  </h:form>
</f:view>
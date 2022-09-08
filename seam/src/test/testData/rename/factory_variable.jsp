<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<f:view>
  <h:form>
    <h:commandButton action="#{blog_facto<caret>ry}"/>
    <h:commandButton action="#{blog_factory.something}"/>
  </h:form>
</f:view>
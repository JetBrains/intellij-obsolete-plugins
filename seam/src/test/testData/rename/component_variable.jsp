<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<f:view>
  <h:form>
    <h:commandButton action="#{blo<caret>g}"/>
    <h:commandButton action="#{blog.something}"/>
  </h:form>
</f:view>
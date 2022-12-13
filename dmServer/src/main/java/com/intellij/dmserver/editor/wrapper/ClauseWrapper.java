package com.intellij.dmserver.editor.wrapper;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;
import org.osmorc.manifest.lang.psi.*;

import java.util.HashMap;
import java.util.Map;

public final class ClauseWrapper {

  private final String myName;

  private final Map<String, AttributeWrapper> myAttributes;

  private final Map<String, AttributeWrapper> myDirectives;

  @Nullable
  public static ClauseWrapper create(Clause clause) {
    HeaderValuePart valuePart = PsiTreeUtil.getChildOfType(clause, HeaderValuePart.class);
    return valuePart == null || valuePart.getUnwrappedText().isEmpty() ? null : new ClauseWrapper(clause, valuePart.getUnwrappedText());
  }

  private ClauseWrapper(Clause clause, String clauseName) {
    myName = clauseName;
    myAttributes = initializeAssignments(clause, Attribute.class);
    myDirectives = initializeAssignments(clause, Directive.class);
  }

  private static <T extends AssignmentExpression> Map<String, AttributeWrapper> initializeAssignments(Clause clause,
                                                                                                      Class<T> assignmentClass) {
    Map<String, AttributeWrapper> result = new HashMap<>();
    for (T attribute = PsiTreeUtil.getChildOfType(clause, assignmentClass);
         attribute != null;
         attribute = PsiTreeUtil.getNextSiblingOfType(attribute, assignmentClass)) {
      AttributeWrapper attributeWrapper = AttributeWrapper.create(attribute);
      if (attributeWrapper != null) {
        result.put(attributeWrapper.getName(), attributeWrapper);
      }
    }
    return result;
  }

  public String getName() {
    return myName;
  }

  @Nullable
  public String getAttributeValue(String attributeName) {
    return getAssignmentValue(myAttributes, attributeName);
  }

  @Nullable
  public String getDirectiveValue(String directiveName) {
    return getAssignmentValue(myDirectives, directiveName);
  }

  private static String getAssignmentValue(Map<String, AttributeWrapper> assignments, String assignmentName) {
    AttributeWrapper assignment = assignments.get(assignmentName);
    return assignment == null ? null : assignment.getValue();
  }
}

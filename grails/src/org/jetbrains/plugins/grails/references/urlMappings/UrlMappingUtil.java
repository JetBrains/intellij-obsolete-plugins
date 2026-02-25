// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.urlMappings;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspLinkElementDescriptor;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.lexer.TokenSets;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrStringContentImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrStringImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UrlMappingUtil {
  private static final Key<Pair<Long, Map<String, NamedUrlMapping>>> FILE_KEY = Key.create("Url Mapping file key");

  public static final String GROUP = "group";

  private UrlMappingUtil() {
  }

  public static Map<String, NamedUrlMapping> getNamedUrlMappings(final @NotNull Module module) {
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> {
      return CachedValueProvider.Result.create(calculateNamedUrlMappings(module), PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  private static Map<String, NamedUrlMapping> calculateNamedUrlMappings(@NotNull Module module) {
    Map<String, NamedUrlMapping> res = new HashMap<>();

    for (GrClassDefinition classDefinition : GrailsArtifact.URLMAPPINGS.getInstances(module).values()) {
      addMapping(res, classDefinition);
    }

    return res;
  }

  private static void addMapping(Map<String, NamedUrlMapping> res, PsiClass aClass) {
    PsiFile file = aClass.getContainingFile().getOriginalFile();

    Pair<Long, Map<String, NamedUrlMapping>> pair = file.getUserData(FILE_KEY);

    if (pair == null || pair.first != file.getModificationStamp()) {
      Map<String, NamedUrlMapping> map = loadMapping(aClass);
      pair = new Pair<>(file.getModificationStamp(), map);
      file.putUserData(FILE_KEY, pair);
    }

    res.putAll(pair.second);
  }

  private static Map<String, NamedUrlMapping> loadMapping(PsiClass aClass) {
    Map<String, NamedUrlMapping> res = new HashMap<>();

    PsiField field = aClass.findFieldByName("mappings", false);

    if (!(field instanceof GrField) || !field.hasModifierProperty(PsiModifier.STATIC)) return Collections.emptyMap();

    GrExpression initializer = ((GrField)field).getInitializerGroovy();

    if (!(initializer instanceof GrClosableBlock)) return Collections.emptyMap();

    for (PsiElement e = initializer.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (!(e instanceof GrMethodCall method)) continue;

      if (!PsiUtil.isReferenceWithoutQualifier(method.getInvokedExpression(), "name")) continue;
      GrArgumentList arguments = method.getArgumentList();
      GrNamedArgument[] namedArguments = arguments.getNamedArguments();
      if (namedArguments.length != 1) continue;

      GrNamedArgument na = namedArguments[0];

      GrArgumentLabel label = na.getLabel();
      if (label == null) continue;

      GrExpression expr = na.getExpression();
      if (!(expr instanceof GrMethodCall innerMethod)) continue;

      String name = label.getName();

      Map<String, Pair<PsiElement, Boolean>> params = getParamsByInvokedExpression(innerMethod.getInvokedExpression());

      if (params != null) {
        res.put(name, new NamedUrlMapping(name, label, params));
      }
    }

    return res;
  }

  public static @Nullable Map<String, Pair<PsiElement, Boolean>> getParamsByInvokedExpression(@Nullable GrExpression invokedExpression) {
    if (invokedExpression instanceof GrReferenceExpression) {
      if (((GrReferenceExpression)invokedExpression).isQualified()) return null;

      final PsiElement referenceNameElement = ((GrReferenceExpression)invokedExpression).getReferenceNameElement();
      if (referenceNameElement == null) return null;

      final IElementType elementType = referenceNameElement.getNode().getElementType();
      if (!TokenSets.STRING_LITERAL_SET.contains(elementType)) return null;

      return Collections.emptyMap();
    }
    else if (invokedExpression instanceof GrString) {
      Map<String, Pair<PsiElement, Boolean>> params = new HashMap<>();

      for (PsiElement injection = invokedExpression.getFirstChild(); injection != null; injection = injection.getNextSibling()) {
        if (injection instanceof GrStringInjection) {
          String param = extractName((GrStringInjection)injection);
          if (param != null) {
            PsiElement next = injection.getNextSibling();
            boolean optional = next instanceof GrStringContentImpl && next.getText().startsWith("?");

            params.put(param, Pair.create(injection, optional));
          }
        }
      }

      return params;
    }

    return null;
  }

  private static @Nullable String extractName(GrStringInjection injection) {
    PsiElement dollar = injection.getFirstChild();
    if (dollar == null) return null;

    PsiElement next = dollar.getNextSibling();
    if (next == null || next.getNextSibling() != null) return null;

    if (next instanceof GrReferenceExpression) {
      if (((GrReferenceExpression)next).getQualifierExpression() != null) return null;
      return ((GrReferenceExpression)next).getReferenceName();
    }

    if (!(next instanceof GrClosableBlock)) return null;

    PsiElement closureStart = next.getFirstChild();
    if (closureStart == null) return null;
    PsiElement ref = closureStart.getNextSibling();
    if (ref instanceof GrParameterList) ref = ref.getNextSibling();
    if (!(ref instanceof GrReferenceExpression)) return null;

    PsiElement closureEnd = ref.getNextSibling();
    if (closureEnd == null || closureEnd.getNextSibling() != null) return null;

    if (((GrReferenceExpression)ref).getQualifierExpression() != null) return null;
    return ((GrReferenceExpression)ref).getReferenceName();
  }

  public static class NamedUrlMapping {
    private final String myName;
    private final PsiElement myElement;
    private final NotNullLazyValue<XmlElementDescriptor> myElementDescriptor;

    public NamedUrlMapping(String name, PsiElement element, Map<String, Pair<PsiElement, Boolean>> params) {
      myName = name;
      myElement = element;
      myElementDescriptor = NotNullLazyValue.atomicLazy(() -> new GspLinkElementDescriptor(name, element, params));
    }

    public String getName() {
      return myName;
    }

    public PsiElement getElement() {
      return myElement;
    }

    public @NotNull XmlElementDescriptor getElementDescriptor() {
      return myElementDescriptor.getValue();
    }
  }

  public static boolean isMappingDefinition(@NotNull GrMethodCall methodCall) {
    GrExpression invokedExpression = methodCall.getInvokedExpression();

    if (invokedExpression instanceof GrReferenceExpression) {
      if (((GrReferenceExpression)invokedExpression).isQualified()) return false;
      final PsiElement referenceNameElement = ((GrReferenceExpression)invokedExpression).getReferenceNameElement();
      if (referenceNameElement == null) return false;
      final IElementType type = referenceNameElement.getNode().getElementType();
      if (!TokenSets.STRING_LITERAL_SET.contains(type)) return false;
    }
    else if (!(invokedExpression instanceof GrStringImpl)) return false;

    PsiElement parent = methodCall.getParent();

    if (!(parent instanceof GrClosableBlock)) {
      if (!(parent instanceof GrNamedArgument)) return false;

      GrMethodCall mc = PsiUtil.getMethodCallByNamedParameter((GrNamedArgument)parent);
      if (mc == null) return false;
      if (!PsiUtil.isReferenceWithoutQualifier((mc).getInvokedExpression(), "name")) return false;

      parent = mc.getParent();
      if (!(parent instanceof GrClosableBlock)) return false;
    }

    parent = unwrapCloseableBlock((GrClosableBlock)parent);

    return isMappingField((GrClosableBlock)parent);
  }

  private static @NotNull GrClosableBlock unwrapCloseableBlock(@NotNull GrClosableBlock block) {
    PsiElement parent = block.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();

    if (!(parent instanceof GrMethodCall)) return block;

    String methodName = PsiUtil.getUnqualifiedMethodName((GrMethodCall)parent);
    if (!GROUP.equals(methodName)) return block;

    PsiElement groupParent = parent.getParent();
    if (groupParent instanceof GrClosableBlock) {
      return (GrClosableBlock)groupParent;
    }

    return block;
  }

  public static boolean isMappingField(@NotNull GrClosableBlock closure) {
    PsiElement field = closure.getParent();
    if (!(field instanceof GrField)) return false;

    if (!"mappings".equals(((GrField)field).getName())) return false;

    PsiClass aClass = ((GrField)field).getContainingClass();
    return GrailsArtifact.URLMAPPINGS.isInstance(aClass);
  }
}

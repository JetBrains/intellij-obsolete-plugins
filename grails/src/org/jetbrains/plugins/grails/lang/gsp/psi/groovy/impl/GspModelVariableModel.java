// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.gspIndex.GspIncludeIndex;
import org.jetbrains.plugins.grails.lang.gsp.gspIndex.GspIncludeInfo;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.codeInspection.utils.ControlFlowUtils;
import org.jetbrains.plugins.groovy.lang.parser.GroovyElementTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.controlFlow.ReadWriteVariableInstruction;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.GrVariableImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class GspModelVariableModel {

  private static final Logger LOG = Logger.getInstance(GspModelVariableModel.class);

  private static final Object INVALID_VARIABLE_MARKER = new Object();

  private static final int MAX_RESOLVE_USAGES = 6;
  private static final int MAX_ATTEMPTS_RESOLVE = 25;

  private final Map<String, ParameterDescriptor> myDescriptors = new HashMap<>();

  private final GspFile myGspFile;

  public GspModelVariableModel(GspFile gspFile) {
    myGspFile = gspFile;

    String fileName = StringUtil.trimEnd(gspFile.getName(), ".gsp");

    Module module = ModuleUtilCore.findModuleForPsiElement(gspFile);
    if (module == null) return;

    collectArgumentFromActionReturn(gspFile, module);

    FileBasedIndex.getInstance().processValues(GspIncludeIndex.NAME, fileName, null,
                                               (file, value) -> {
                                                 for (GspIncludeInfo includeInfo : value) {
                                                   IncludePoint point = new IncludePoint(file, includeInfo.getOffset());

                                                   for (String argumentName : includeInfo.getNamedArguments()) {
                                                     getOrCreateParameterDescriptor(argumentName).myUsages.add(point);
                                                   }
                                                 }
                                                 return true;
                                               }, module.getModuleContentScope());
  }

  public static GspModelVariableModel getInstance(final @NotNull GspFile file) {
    return CachedValuesManager.getCachedValue(file,
                                              () -> new CachedValueProvider.Result<>(new GspModelVariableModel(file),
                                                                                     PsiModificationTracker.MODIFICATION_COUNT));
  }

  private ParameterDescriptor getOrCreateParameterDescriptor(String name) {
    ParameterDescriptor descriptor = myDescriptors.get(name);
    if (descriptor == null) {
      descriptor = new ParameterDescriptor(name);
      myDescriptors.put(name, descriptor);
    }

    return descriptor;
  }

  public Collection<String> getArgumentNames() {
    return myDescriptors.keySet();
  }

  public @Nullable PsiVariable getVariable(@Nullable String name) {
    ParameterDescriptor descriptor = myDescriptors.get(name);
    if (descriptor != null) {
      return descriptor.getVariable();
    }

    return null;
  }

  private void collectArgumentFromActionReturn(GspFile gspFile, Module module) {
    VirtualFile virtualFile = gspFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) return;

    String gspName = virtualFile.getNameWithoutExtension();

    String controllerName = GrailsUtils.getControllerNameByGsp(virtualFile);

    PsiMethod method = GrailsUtils.getControllerActions(controllerName, module).get(gspName);
    if (method == null) return;

    GrCodeBlock block;

    if (method instanceof GrAccessorMethod) {
      GrExpression initializerGroovy = ((GrAccessorMethod)method).getProperty().getInitializerGroovy();
      if (!(initializerGroovy instanceof GrClosableBlock)) return;

      block = (GrClosableBlock)initializerGroovy;
    }
    else if (method instanceof GrMethod) {
      block = ((GrMethod)method).getBlock();
    }
    else {
      if (ApplicationManager.getApplication().isInternal()) {
        LOG.error("Unknown action type");
      }

      return;
    }

    if (block == null) return;

    for (GrStatement returnStatement : ControlFlowUtils.collectReturns(block, true)) {
      GrExpression value = ControlFlowUtils.extractReturnExpression(returnStatement);
      if (value instanceof GrListOrMap) {
        collectNamedArguments(value);
      }
      else if (value instanceof GrReferenceExpression ref) {
        if (!ref.isQualified()) {
          PsiElement resolve = ref.resolve();
          if (resolve instanceof GrVariableImpl) {
            for (ReadWriteVariableInstruction inst : ControlFlowUtils.findAccess((GrVariable)resolve, ref, false, false)) {
              PsiElement element = inst.getElement();
              if (element instanceof GrVariable) {
                collectNamedArguments(((GrVariable)element).getInitializerGroovy());
              }
              else if (element instanceof GrReferenceExpression) {
                PsiElement parent = element.getParent();
                if (parent instanceof GrAssignmentExpression) {
                  if (((GrAssignmentExpression)parent).getLValue() == element) {
                    collectNamedArguments(((GrAssignmentExpression)parent).getRValue());
                  }
                }
                else if (parent instanceof GrBinaryExpression
                         && ((GrBinaryExpression)parent).getOperationTokenType() == GroovyElementTypes.COMPOSITE_LSHIFT_SIGN) {
                  if (((GrBinaryExpression)parent).getLeftOperand() == element) {
                    collectNamedArguments(((GrBinaryExpression)parent).getRightOperand());
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void collectNamedArguments(@Nullable PsiElement initializer) {
    if (!(initializer instanceof GrListOrMap)) return;

    for (GrNamedArgument namedArgument : ((GrListOrMap)initializer).getNamedArguments()) {
      String labelName = GrailsPsiUtil.getPlainLabelName(namedArgument);
      if (labelName == null) continue;

      getOrCreateParameterDescriptor(labelName).myArgumentsFromActionReturn.add(namedArgument);
    }
  }

  private static PsiVariable createVariableDefinedInNamedArguments(String name,
                                                                   Collection<PsiElement> arguments,
                                                                   PsiElement scope) {
    PsiType type = null;
    List<PsiElement> declarations = new ArrayList<>(arguments.size());

    PsiManager manager = scope.getManager();

    for (PsiElement argument : arguments) {
      PsiType argumentType = null;

      if (argument instanceof GrNamedArgument) {
        GrExpression expression = ((GrNamedArgument)argument).getExpression();
        if (expression != null) {
          argumentType = expression.getType();
        }
        declarations.add(((GrNamedArgument)argument).getLabel());
      }
      else {
        XmlAttribute attribute = (XmlAttribute)argument;
        XmlAttributeValue valueElement = attribute.getValueElement();
        if (valueElement != null) {
          argumentType = GrailsPsiUtil.getAttributeExpressionType(valueElement);
        }
        declarations.add(attribute);
      }

      type = TypesUtil.getLeastUpperBoundNullable(argumentType, type, manager);
    }

    if (type == null) {
      type = PsiType.getJavaLangObject(manager, scope.getResolveScope());
    }

    return new GrLightVariable(manager, name, type, declarations, scope);
  }

  private final class ParameterDescriptor {
    private final String myName;

    private final AtomicReference<Object> myVariable = new AtomicReference<>();

    private final List<IncludePoint> myUsages = new ArrayList<>();

    private final List<GrNamedArgument> myArgumentsFromActionReturn = new ArrayList<>();

    private ParameterDescriptor(String name) {
      myName = name;
    }

    private @Nullable PsiVariable calculateVariable() {
      List<PsiElement> arguments = new ArrayList<>();

      int resolvedUsages = 0;

      if (myUsages.size() > MAX_RESOLVE_USAGES) {
        // Optimization: calculate count of resolved usages. if count of resolved usages > MAX_RESOLVE_USAGES we don't need calculate type of variable.

        for (IncludePoint usage : myUsages) {
          if (usage.myResolved == Boolean.TRUE) {
            resolvedUsages++;
          }
        }

        if (resolvedUsages > MAX_RESOLVE_USAGES) {
          return createUntypedVariable();
        }
      }

      int numberOfAttemptsResolve = MAX_ATTEMPTS_RESOLVE;

      for (IncludePoint usage : myUsages) {
        if (usage.myResolved == null) {
          if (usage.resolve()) {
            resolvedUsages++;
            if (resolvedUsages > MAX_RESOLVE_USAGES) {
              return createUntypedVariable();
            }
          }

          numberOfAttemptsResolve--;
          if (numberOfAttemptsResolve == 0) {
            return resolvedUsages > 0 ? createUntypedVariable() : null;
          }
        }
      }

      // We're sure that usages <= MAX_RESOLVE_USAGES. Each usage in 'usages' is initialized.
      for (IncludePoint usage : myUsages) {
        if (usage.myResolved) {
          ContainerUtil.addIfNotNull(arguments, usage.findArgument(myName));
        }
      }

      arguments.addAll(myArgumentsFromActionReturn);

      if (arguments.isEmpty()) return null;

      return createVariableDefinedInNamedArguments(myName, arguments, myGspFile.getGroovyLanguageRoot());
    }

    private PsiVariable createUntypedVariable() {
      GrLightVariable res = new GrLightVariable(myGspFile.getManager(), myName, CommonClassNames.JAVA_LANG_OBJECT, myGspFile);
      res.setNavigationElement(res);
      return res;
    }

    public @Nullable PsiVariable getVariable() {
      Object value = myVariable.get();

      if (INVALID_VARIABLE_MARKER == value) {
        return null;
      }

      if (value != null) {
        return (PsiVariable)value;
      }

      PsiVariable variable = calculateVariable();
      if (variable == null) {
        myVariable.compareAndSet(null, INVALID_VARIABLE_MARKER);
        return null;
      }

      if (!myVariable.compareAndSet(null, variable)) {
        Object o = myVariable.get();
        return o == INVALID_VARIABLE_MARKER ? null : (PsiVariable)o;
      }

      return variable;
    }
  }

  private final class IncludePoint {
    private final VirtualFile myFile;
    private final int myOffset;

    private GrListOrMap myListOrMap;
    private GspGrailsTag myTag;

    private volatile Boolean myResolved;

    private IncludePoint(VirtualFile file, int offset) {
      myFile = file;
      myOffset = offset;
    }

    private @Nullable PsiElement findArgument(String name) {
      if (myListOrMap != null) {
        return myListOrMap.findNamedArgument(name);
      }

      return myTag.getAttribute(name);
    }

    private boolean resolveInternal() {
      PsiFile file = myGspFile.getManager().findFile(myFile);

      if (!(file instanceof GspFile || file instanceof GroovyFile)) return false;

      PsiElement elementAt = file.findElementAt(myOffset);
      if (elementAt == null) return false;

      PsiElement viewElement = elementAt.getParent();
      if (viewElement == null) return false;

      for (PsiReference reference : viewElement.getReferences()) {
        if (reference instanceof FileReference) {
          final FileReference lastReference = ((FileReference)reference).getFileReferenceSet().getLastReference();
          if (lastReference == null) break;

          PsiElement resolve = RecursionManager.doPreventingRecursion(viewElement, false, lastReference::resolve);

          if (resolve != myGspFile) break;

          if (viewElement instanceof GspGrailsTag) {
            myTag = (GspGrailsTag)viewElement;
            return true;
          }

          PsiElement parent = viewElement.getParent();
          if (parent instanceof XmlAttribute) {
            PsiElement xmlTag = parent.getParent();
            if (!(xmlTag instanceof XmlTag)) break;

            XmlAttribute modelAttr = ((XmlTag)xmlTag).getAttribute("model");
            if (modelAttr == null) break;

            myListOrMap = GspIncludeIndex.extractMap(modelAttr);
            return true;
          }

          if (parent instanceof GrNamedArgument) {
            PsiElement model = PsiUtil.getNamedArgumentValue((GrNamedArgument)parent, "model");
            if (model instanceof GrListOrMap) {
              myListOrMap = (GrListOrMap)model;
              return true;
            }
          }

          break;
        }
      }

      return false;
    }

    public boolean resolve() {
      Boolean resolved = myResolved;
      if (resolved == null) {
        resolved = resolveInternal();
        myResolved = resolved;
      }

      return resolved;
    }
  }
}

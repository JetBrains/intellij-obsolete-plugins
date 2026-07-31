/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.rpc;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RemoteServiceUtil {
  public static final @NonNls String REMOTE_SERVICE_INTERFACE_NAME = "com.google.gwt.user.client.rpc.RemoteService";
  public static final @NonNls String ASYNC_CALLBACK_INTERFACE_NAME = "com.google.gwt.user.client.rpc.AsyncCallback";
  public static final @NonNls String SERVICE_PATH_ANNOTATION_NAME = "com.google.gwt.user.client.rpc.RemoteServiceRelativePath";
  private static final @NonNls String REMOTE_SERVICE_SERVLET_NAME = "com.google.gwt.user.server.rpc.RemoteServiceServlet";
  public static final @NonNls String ASYNC_SUFFIX = "Async";
  public static final @NonNls String IMPL_SERVICE_SUFFIX = "Impl";
  private static final @NonNls String VOID_CLASS_NAME = "java.lang.Void";

  private RemoteServiceUtil() {
  }

  public static boolean isMethodPresentedInAsync(@NotNull PsiMethod method, @NotNull PsiClass async) {
    return findMethodInAsync(method, async) != null;
  }

  public static boolean isMethodPresentedInSync(@NotNull PsiMethod asyncMethod, @NotNull PsiClass sync) {
    return findMethodInSync(asyncMethod, sync) != null;
  }

  public static @Nullable PsiMethod findSynchronousMethod(@NotNull PsiMethod asyncMethod) {
    final PsiClass asyncClass = asyncMethod.getContainingClass();
    if (asyncClass != null) {
      final PsiClass syncClass = findSynchronousInterface(asyncClass);
      if (syncClass != null) {
        return findMethodInSync(asyncMethod, syncClass);
      }
    }
    return null;
  }

  public static @Nullable PsiMethod findMethodInSync(final @NotNull PsiMethod asyncMethod, final @NotNull PsiClass sync) {
    PsiParameter[] asyncParameters = asyncMethod.getParameterList().getParameters();
    PsiType[] asyncParameterTypes = getParameterTypes(asyncParameters, null);

    for (Pair<PsiMethod, PsiSubstitutor> pair : sync.findMethodsAndTheirSubstitutorsByName(asyncMethod.getName(), true)) {
      PsiMethod syncMethod = pair.getFirst();
      PsiSubstitutor substitutor = pair.getSecond();

      PsiParameter[] syncParameters = syncMethod.getParameterList().getParameters();
      PsiType[] syncParameterTypes = getParameterTypes(syncParameters, substitutor);

      if (areParametersCorresponding(syncParameterTypes, asyncParameterTypes, substitutor.substitute(syncMethod.getReturnType()), sync.getProject())) {
        return syncMethod;
      }
    }

    return null;
  }

  public static @Nullable PsiMethod findMethodInAsync(@NotNull PsiMethod syncMethod, @NotNull PsiClass async) {
    PsiParameter[] syncParameters = syncMethod.getParameterList().getParameters();
    PsiType[] syncParameterTypes = getParameterTypes(syncParameters, null);

    for (Pair<PsiMethod, PsiSubstitutor> pair : async.findMethodsAndTheirSubstitutorsByName(syncMethod.getName(), true)) {
      PsiMethod asyncMethod = pair.getFirst();
      PsiSubstitutor substitutor = pair.getSecond();

      PsiParameter[] asyncParameters = asyncMethod.getParameterList().getParameters();
      PsiType[] asyncParameterTypes = getParameterTypes(asyncParameters, substitutor);

      if (areParametersCorresponding(syncParameterTypes, asyncParameterTypes, syncMethod.getReturnType(), async.getProject())) {
        return asyncMethod;
      }
    }
    return null;
  }

  private static PsiType @NotNull [] getParameterTypes(PsiParameter[] parameters, PsiSubstitutor substitutor) {
    PsiType[] parameterTypes = PsiType.createArray(parameters.length);
    for (int i = 0; i < parameters.length; i++) {
      parameterTypes[i] = substitutor == null ? parameters[i].getType() : substitutor.substitute(parameters[i].getType());
    }
    return parameterTypes;
  }

  private static boolean areParametersCorresponding(final PsiType[] syncParameters, final PsiType[] asyncParameters,
                                                    @Nullable PsiType syncReturnType, Project project) {
    if (asyncParameters.length != syncParameters.length + 1) return false;

    for (int i = 0; i != syncParameters.length; ++i) {
      if (!areErasuresEqual(syncParameters[i], asyncParameters[i])) {
        return false;
      }
    }

    PsiType lastParameterType = asyncParameters[syncParameters.length];
    if (!(lastParameterType instanceof PsiClassType lastClassType)) {
      return false;
    }
    PsiClassType.ClassResolveResult resolveResult = lastClassType.resolveGenerics();
    PsiClass psiClass = resolveResult.getElement();
    if (psiClass == null || !ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName())) {
      return false;
    }
    if (PsiClassType.isRaw(resolveResult) || !psiClass.hasTypeParameters()) {
      return true;
    }

    PsiType actualTypeParameter = resolveResult.getSubstitutor().substitute(psiClass.getTypeParameters()[0]);
    if (syncReturnType instanceof PsiPrimitiveType) {
      PsiClassType boxedType = getBoxedType((PsiPrimitiveType)syncReturnType, lastParameterType.getResolveScope(), project);
      if (boxedType != null) {
        syncReturnType = boxedType;
      }
    }

    return actualTypeParameter != null && syncReturnType != null && areErasuresEqual(syncReturnType, actualTypeParameter);
  }

  private static boolean areErasuresEqual(@NotNull PsiType t1, @NotNull PsiType t2) {
    final PsiType type1 = TypeConversionUtil.erasure(t1);
    final PsiType type2 = TypeConversionUtil.erasure(t2);
    return type1.equals(type2);
  }

  public static boolean isRemoteServiceInterface(final @Nullable PsiClass aClass) {
    if (aClass == null || !aClass.isInterface()) return false;
    return InheritanceUtil.isInheritor(aClass, true, REMOTE_SERVICE_INTERFACE_NAME);
  }

  public static boolean isRemoteServiceImplementation(final @Nullable PsiClass aClass) {
    if (aClass == null || aClass.isInterface() || aClass.hasModifierProperty(PsiModifier.ABSTRACT)) return false;
    return InheritanceUtil.isInheritor(aClass, true, REMOTE_SERVICE_SERVLET_NAME);
  }

  public static @Nullable PsiClass findRemoteServiceInterface(PsiClass serviceImpl) {
    final PsiClass[] interfaces = serviceImpl.getInterfaces();
    for (PsiClass anInterface : interfaces) {
      if (isRemoteServiceInterface(anInterface)) {
        return anInterface;
      }
    }
    return null;
  }

  public static @Nullable PsiClass findSynchronousInterface(final @Nullable PsiClass asyncInterface) {
    if (asyncInterface == null) {
      return null;
    }

    String name = asyncInterface.getQualifiedName();
    if (name == null || !name.endsWith(ASYNC_SUFFIX)) {
      return null;
    }

    final PsiManager psiManager = asyncInterface.getManager();
    final GlobalSearchScope scope = asyncInterface.getResolveScope();

    String syncName = name.substring(0, name.length() - ASYNC_SUFFIX.length());
    final PsiClass sync = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(syncName, scope);
    if (InheritanceUtil.isInheritor(sync, true, REMOTE_SERVICE_INTERFACE_NAME)) {
      return sync;
    }
    return null;
  }

  public static @Nullable PsiClass findAsynchronousInterface(@Nullable PsiClass aClass) {
    if (aClass == null) return null;
    return JavaPsiFacade.getInstance(aClass.getProject()).findClass(aClass.getQualifiedName() + ASYNC_SUFFIX, aClass.getResolveScope());
  }

  public static @Nullable PsiMethod findAsynchronousMethod(@NotNull PsiMethod method) {
    PsiClass psiClass = method.getContainingClass();
    if (psiClass == null || !isRemoteServiceInterface(psiClass)) return null;

    PsiClass asyncClass = findAsynchronousInterface(psiClass);
    if (asyncClass == null) return null;

    return findMethodInAsync(method, asyncClass);
  }

  public static void copyAllMethodsToAsync(@NotNull PsiClass sync, @NotNull PsiClass async, @NotNull GwtVersion gwtVersion) throws IncorrectOperationException {
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(sync.getProject()).getElementFactory();

    for (PsiMethod method : async.getMethods()) {
      method.delete();
    }

    for (PsiMethod method : sync.getMethods()) {
      copyMethodToAsync(method, async, elementFactory, gwtVersion);
    }
  }

  public static PsiMethod copyMethodToAsync(@NotNull PsiMethod method, @NotNull PsiClass async, @NotNull GwtVersion gwtVersion) throws IncorrectOperationException {
    return copyMethodToAsync(method, async, JavaPsiFacade.getInstance(method.getProject()).getElementFactory(), gwtVersion);
  }

  private static PsiMethod copyMethodToAsync(final @NotNull PsiMethod method, final @NotNull PsiClass async,
                                             final @NotNull PsiElementFactory elementFactory,
                                             final @NotNull GwtVersion gwtVersion) throws IncorrectOperationException {
    final PsiType asyncCallbackType;
    if (!gwtVersion.isGenericsSupported()) {
      asyncCallbackType = createAsyncCallbackType(async, null);
    }
    else {
      asyncCallbackType = createAsyncCallbackType(async, method.getReturnType());
    }
    PsiMethod newMethod = (PsiMethod)method.copy();
    newMethod.getParameterList().add(elementFactory.createParameter("async", asyncCallbackType));
    final PsiReferenceList throwsList = newMethod.getThrowsList();
    for (PsiJavaCodeReferenceElement element : throwsList.getReferenceElements()) {
      element.delete();
    }
    GwtGenericsUtil.removeTypeArgsJavadocTags(newMethod);
    final PsiTypeElement returnTypeElement = newMethod.getReturnTypeElement();
    assert returnTypeElement != null;
    returnTypeElement.replace(elementFactory.createTypeElement(PsiTypes.voidType()));
    return (PsiMethod) async.add(newMethod);
  }

  public static PsiClassType createAsyncCallbackType(@NotNull PsiElement context, @Nullable PsiType parameter) {
    Project project = context.getProject();
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
    GlobalSearchScope scope = context.getResolveScope();
    PsiClass asyncCallbackClass = psiFacade.findClass(ASYNC_CALLBACK_INTERFACE_NAME, scope);
    PsiElementFactory elementFactory = psiFacade.getElementFactory();
    if (asyncCallbackClass == null) {
      return elementFactory.createTypeByFQClassName(ASYNC_CALLBACK_INTERFACE_NAME, scope);
    }

    PsiTypeParameter[] typeParameters = asyncCallbackClass.getTypeParameters();
    if (parameter instanceof PsiPrimitiveType) {
      parameter = getBoxedType((PsiPrimitiveType)parameter, scope, project);
    }
    if (parameter == null || typeParameters.length == 0) {
      return elementFactory.createType(asyncCallbackClass);
    }
    return elementFactory.createType(asyncCallbackClass, PsiSubstitutor.EMPTY.put(typeParameters[0], parameter));
  }

  private static @Nullable PsiClassType getBoxedType(@NotNull PsiPrimitiveType type, @NotNull GlobalSearchScope scope, @NotNull Project project) {
    if (TypeConversionUtil.isVoidType(type)) {
      return JavaPsiFacade.getInstance(project).getElementFactory().createTypeByFQClassName(VOID_CLASS_NAME, scope);
    }
    return type.getBoxedType(PsiManager.getInstance(project), scope);
  }

  public static PsiMethod copyMethodToSync(final PsiMethod method, final PsiClass sync) throws IncorrectOperationException {
    Project project = method.getProject();
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    PsiMethod newMethod = (PsiMethod)method.copy();
    PsiParameter[] parameters = newMethod.getParameterList().getParameters();
    PsiType newReturnType = PsiType.getJavaLangObject(PsiManager.getInstance(project), sync.getResolveScope());
    if (parameters.length > 0) {
      PsiParameter callbackParameter = parameters[parameters.length - 1];
      PsiType type = method.getParameterList().getParameters()[parameters.length - 1].getType();
      if (type instanceof PsiClassType classType) {
        PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
        PsiClass psiClass = resolveResult.getElement();
        if (psiClass != null && ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName())) {
          PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
          if (typeParameters.length > 0) {
            PsiType parameter = resolveResult.getSubstitutor().substitute(typeParameters[0]);
            if (parameter != null) {
              PsiType unboxed;
              if (isBoxedVoidType(parameter)) {
                unboxed = PsiTypes.voidType();
              }
              else {
                unboxed = PsiPrimitiveType.getUnboxedType(parameter);
              }
              newReturnType = unboxed != null ? unboxed : parameter;
            }
          }
          callbackParameter.delete();
        }
      }
    }
    PsiTypeElement typeElement = newMethod.getReturnTypeElement();
    if (typeElement != null) {
      PsiTypeElement newTypeElement = elementFactory.createTypeElement(newReturnType);
      typeElement.replace(newTypeElement);
    }
    return (PsiMethod) sync.add(newMethod);
  }

  private static boolean isBoxedVoidType(PsiType parameter) {
    if (!(parameter instanceof PsiClassType)) {
      return false;
    }
    final PsiClass psiClass = ((PsiClassType)parameter).resolve();
    return psiClass != null && VOID_CLASS_NAME.equals(psiClass.getQualifiedName());
  }

}

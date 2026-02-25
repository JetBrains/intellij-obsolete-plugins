// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.jobs;

import com.intellij.psi.PsiClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public final class JobsMemberProvider extends MemberProvider {
  private static final String CLASS_SOURCE = "class JobElements {" +
                                             " public static Date schedule(String cronExpression, Map params = null) {}" +
                                             " public static Date schedule(Long interval, Integer repeatCount = org.quartz.SimpleTrigger.REPEAT_INDEFINITELY, Map params = null) {}" +
                                             " public static Date schedule(Date scheduleDate) {}" +
                                             " public static Date schedule(Date scheduleDate, Map params) {}" +
                                             " public static Date schedule(org.quartz.Trigger trigger) {}" +
                                             " public static void triggerNow(Map params = null) {}" +
                                             " public static boolean removeJob() {}" +
                                             " public static Date reschedule(org.quartz.Trigger trigger) {}" +
                                             " public static boolean unschedule(String triggerName, String triggerGroup = org.codehaus.groovy.grails.plugins.quartz.GrailsTaskClassProperty.DEFAULT_TRIGGERS_GROUP) {}" +
                                             "}";

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    DynamicMemberUtils.process(processor, psiClass, ref, CLASS_SOURCE);
  }
}

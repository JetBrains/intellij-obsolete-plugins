package com.intellij.seam.model.xml;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedPackagesSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.constants.SeamNonComponentAnnotations;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.core.*;
import com.intellij.seam.model.xml.drools.ManagedWorkingMemory;
import com.intellij.seam.model.xml.drools.RuleAgent;
import com.intellij.seam.model.xml.drools.RuleBase;
import com.intellij.seam.model.xml.framework.EntityHome;
import com.intellij.seam.model.xml.framework.EntityQuery;
import com.intellij.seam.model.xml.framework.HibernateEntityHome;
import com.intellij.seam.model.xml.framework.HibernateEntityQuery;
import com.intellij.seam.model.xml.jms.ManagedQueueSender;
import com.intellij.seam.model.xml.jms.ManagedTopicPublisher;
import com.intellij.seam.model.xml.jms.QueueConnection;
import com.intellij.seam.model.xml.jms.TopicConnection;
import com.intellij.seam.model.xml.mail.MailSession;
import com.intellij.seam.model.xml.mail.Meldware;
import com.intellij.seam.model.xml.mail.MeldwareUser;
import com.intellij.seam.model.xml.pdf.DocumentStore;
import com.intellij.seam.model.xml.pdf.KeyStoreConfig;
import com.intellij.seam.model.xml.persistence.*;
import com.intellij.seam.model.xml.remoting.Debug;
import com.intellij.seam.model.xml.remoting.RemotingConfig;
import com.intellij.seam.model.xml.security.Identity;
import com.intellij.seam.model.xml.spring.ContextLoader;
import com.intellij.seam.model.xml.spring.SpringTransaction;
import com.intellij.seam.model.xml.spring.TaskExecutorDispatcher;
import com.intellij.seam.model.xml.theme.ThemeSelector;
import com.intellij.seam.model.xml.web.*;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtension;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SeamComponentsExtender extends DomExtender<SeamComponents> {
  private static final Map<Pair<String, String>, Class> myCustomNamespacedComponentsMap =
    new HashMap<>();
  private static final Map<Pair<String, String>, Pair<String, Class>> myCustomComponentsRegistryMap =
    new HashMap<>();

  static {
    myCustomNamespacedComponentsMap
        .put(Pair.create("entity-home", SeamNamespaceConstants.FRAMEWORK_NAMESPACE), EntityHome.class);
    myCustomNamespacedComponentsMap
        .put(Pair.create("resource-loader", SeamNamespaceConstants.CORE_NAMESPACE), ResourceLoader.class);

    myCustomComponentsRegistryMap.put(Pair.create("resource-bundle", SeamNamespaceConstants.CORE_NAMESPACE),
                                      new Pair<>("org.jboss.seam.core.ResourceBundle", ResourceBundle.class));
  }

  @Override
  public void registerExtensions(@NotNull final SeamComponents seamComponents, @NotNull final DomExtensionsRegistrar registrar) {
    registerStaticExtensions(registrar);

    XmlFile xmlFile = DomUtil.getFile(seamComponents);

    Project project = xmlFile.getProject();
    Map<PsiPackage, String> myNamespaceToPackageName = getNamespaceToPackageName(JavaPsiFacade.getInstance(project), project);

    for (PsiPackage psiPackage : myNamespaceToPackageName.keySet()) {
      for (PsiClass psiClass : psiPackage.getClasses()) {
        registerSeamComponent(psiClass, myNamespaceToPackageName.get(psiPackage), registrar);
      }
    }

    for (Pair<String, String> pair : myCustomComponentsRegistryMap.keySet()) {
      final Pair<String, Class> classesPair = myCustomComponentsRegistryMap.get(pair);

      registerExtensions(registrar, project, pair.getFirst(), pair.getSecond(), classesPair.getFirst(), classesPair.getSecond());
    }
  }

  private static void registerStaticExtensions(final DomExtensionsRegistrar registrar) {
    // core namespace
    registrar.registerCollectionChildrenExtension(new XmlName("init", SeamNamespaceConstants.CORE_NAMESPACE), Init.class);
    registrar.registerCollectionChildrenExtension(new XmlName("manager", SeamNamespaceConstants.CORE_NAMESPACE), Manager.class);
    registrar.registerCollectionChildrenExtension(new XmlName("pojo-cache", SeamNamespaceConstants.CORE_NAMESPACE), PojoCache.class);
    //registrar.registerCollectionChildrenExtension(new XmlName("resource-bundle", SeamNamespaceConstants.CORE_NAMESPACE), ResourceBundle.class);
    //registrar.registerCollectionChildrenExtension(new XmlName("resource-loader", SeamNamespaceConstants.CORE_NAMESPACE), ResourceLoader.class);

    // drools namespace
    registrar.registerCollectionChildrenExtension(new XmlName("managed-working-memory", SeamNamespaceConstants.DROOLS_NAMESPACE),
                                                  ManagedWorkingMemory.class);
    registrar.registerCollectionChildrenExtension(new XmlName("rule-agent", SeamNamespaceConstants.DROOLS_NAMESPACE), RuleAgent.class);
    registrar.registerCollectionChildrenExtension(new XmlName("rule-base", SeamNamespaceConstants.DROOLS_NAMESPACE), RuleBase.class);

    // tramework namespace
    //registrar.registerCollectionChildrenExtension(new XmlName("entity-home", SeamNamespaceConstants.FRAMEWORK_NAMESPACE), EntityHome.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("entity-query", SeamNamespaceConstants.FRAMEWORK_NAMESPACE), EntityQuery.class);
    registrar.registerCollectionChildrenExtension(new XmlName("hibernate-entity-home", SeamNamespaceConstants.FRAMEWORK_NAMESPACE),
                                                  HibernateEntityHome.class);
    registrar.registerCollectionChildrenExtension(new XmlName("hibernate-entity-query", SeamNamespaceConstants.FRAMEWORK_NAMESPACE),
                                                  HibernateEntityQuery.class);

    // jms namespace
    registrar.registerCollectionChildrenExtension(new XmlName("managed-queue-sender", SeamNamespaceConstants.JMS_NAMESPACE),
                                                  ManagedQueueSender.class);
    registrar.registerCollectionChildrenExtension(new XmlName("managed-topic-publisher", SeamNamespaceConstants.JMS_NAMESPACE),
                                                  ManagedTopicPublisher.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("queue-connection", SeamNamespaceConstants.JMS_NAMESPACE), QueueConnection.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("topic-connection", SeamNamespaceConstants.JMS_NAMESPACE), TopicConnection.class);

    // mail namespace
    registrar.registerCollectionChildrenExtension(new XmlName("mail-session", SeamNamespaceConstants.MAIL_NAMESPACE), MailSession.class);
    registrar.registerCollectionChildrenExtension(new XmlName("meldware", SeamNamespaceConstants.MAIL_NAMESPACE), Meldware.class);
    registrar.registerCollectionChildrenExtension(new XmlName("meldware-user", SeamNamespaceConstants.MAIL_NAMESPACE), MeldwareUser.class);

    // pdf namespace
    registrar.registerCollectionChildrenExtension(new XmlName("document-store", SeamNamespaceConstants.PDF_NAMESPACE), DocumentStore.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("key-store-config", SeamNamespaceConstants.PDF_NAMESPACE), KeyStoreConfig.class);

    // persistence namespace
    registrar.registerCollectionChildrenExtension(new XmlName("entity-manager-factory", SeamNamespaceConstants.PERSISTENCE_NAMESPACE),
                                                  EntityManagerFactory.class);
    registrar.registerCollectionChildrenExtension(new XmlName("filter", SeamNamespaceConstants.PERSISTENCE_NAMESPACE), Filter.class);
    registrar.registerCollectionChildrenExtension(new XmlName("hibernate-session-factory", SeamNamespaceConstants.PERSISTENCE_NAMESPACE),
                                                  HibernateSessionFactory.class);
    registrar.registerCollectionChildrenExtension(new XmlName("managed-hibernate-session", SeamNamespaceConstants.PERSISTENCE_NAMESPACE),
                                                  ManagedHibernateSession.class);
    registrar.registerCollectionChildrenExtension(new XmlName("managed-persistence-context", SeamNamespaceConstants.PERSISTENCE_NAMESPACE),
                                                  ManagedPersistenceContext.class);

    // remote namespace
    registrar.registerCollectionChildrenExtension(new XmlName("debug", SeamNamespaceConstants.REMOTING_NAMESPACE), Debug.class);
    registrar.registerCollectionChildrenExtension(new XmlName("remoting-config", SeamNamespaceConstants.REMOTING_NAMESPACE),
                                                  RemotingConfig.class);
    registrar.registerCollectionChildrenExtension(new XmlName("identity", SeamNamespaceConstants.SECURITY_NAMESPACE), Identity.class);

    // spring namespace
    registrar
        .registerCollectionChildrenExtension(new XmlName("context-loader", SeamNamespaceConstants.SPRING_NAMESPACE), ContextLoader.class);
    registrar.registerCollectionChildrenExtension(new XmlName("spring-transaction", SeamNamespaceConstants.SPRING_NAMESPACE),
                                                  SpringTransaction.class);
    registrar.registerCollectionChildrenExtension(new XmlName("task-executor-dispatcher", SeamNamespaceConstants.SPRING_NAMESPACE),
                                                  TaskExecutorDispatcher.class);
    // theme namespace
    registrar
        .registerCollectionChildrenExtension(new XmlName("theme-selector", SeamNamespaceConstants.THEME_NAMESPACE), ThemeSelector.class);

    // web namespace
    registrar
        .registerCollectionChildrenExtension(new XmlName("ajax-4-jsf-filter", SeamNamespaceConstants.WEB_NAMESPACE), Ajax4jsfFilter.class);
    registrar.registerCollectionChildrenExtension(new XmlName("authentication-filter", SeamNamespaceConstants.WEB_NAMESPACE),
                                                  AuthenticationFilter.class);
    registrar.registerCollectionChildrenExtension(new XmlName("character-encoding-filter", SeamNamespaceConstants.WEB_NAMESPACE),
                                                  CharacterEncodingFilter.class);
    registrar.registerCollectionChildrenExtension(new XmlName("context-filter", SeamNamespaceConstants.WEB_NAMESPACE), ContextFilter.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("exception-filter", SeamNamespaceConstants.WEB_NAMESPACE), ExceptionFilter.class);
    registrar.registerCollectionChildrenExtension(new XmlName("logging-filter", SeamNamespaceConstants.WEB_NAMESPACE), LoggingFilter.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("multipart-filter", SeamNamespaceConstants.WEB_NAMESPACE), MultipartFilter.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("redirect-filter", SeamNamespaceConstants.WEB_NAMESPACE), RedirectFilter.class);
    registrar
        .registerCollectionChildrenExtension(new XmlName("servlet-session", SeamNamespaceConstants.WEB_NAMESPACE), ServletSession.class);
  }

  private static void registerExtensions(final DomExtensionsRegistrar registrar,
                                         final Project project,
                                         final String tagName,
                                         final String namespace,
                                         final String className,
                                         final Class aClass) {
    final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
    if (psiClass != null) {
      registrar.registerCollectionChildrenExtension(new XmlName(tagName, namespace), aClass);
    }
  }

  private static void registerSeamComponent(final PsiClass psiClass, final String namespace, final DomExtensionsRegistrar registrar) {
    final String xmlName = getXmlName(psiClass);

    Class aClass = myCustomNamespacedComponentsMap.get(Pair.create(xmlName, namespace));

    Class componentClass = aClass == null ? CustomSeamComponent.class : aClass;

    final DomExtension domExtension = registrar.registerCollectionChildrenExtension(new XmlName(xmlName, namespace), componentClass);

    domExtension.putUserData(CustomSeamComponent.COMPONENT_TYPE, psiClass);
  }

  private static String getXmlName(final PsiClass psiClass) {
    return StringUtil.join(NameUtil.nameToWordsLowerCase(psiClass.getName()), "-");
  }

  private static Map<PsiPackage, String> getNamespaceToPackageName(final JavaPsiFacade facade, final Project project) {
    Map<PsiPackage, String> namespaceToPackageName = new HashMap<>();
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    PsiClass namespaceAnnotationClass = facade.findClass(SeamNonComponentAnnotations.NAMESPACE_ANNOTATION, scope);
    if (namespaceAnnotationClass != null) {
      for (final PsiPackage target : AnnotatedPackagesSearch.search(namespaceAnnotationClass, scope)) {
        final PsiAnnotation annotation = AnnotationUtil.findAnnotation(target, SeamNonComponentAnnotations.NAMESPACE_ANNOTATION);
        if (annotation == null) continue;

        final PsiAnnotationMemberValue psiAnnotationMemberValue = annotation.findAttributeValue("value");
        if (psiAnnotationMemberValue instanceof PsiLiteralExpression) {
          namespaceToPackageName.put(target, StringUtil.stripQuotesAroundValue(psiAnnotationMemberValue.getText()));
        }
      }
    }
    return namespaceToPackageName;
  }
}

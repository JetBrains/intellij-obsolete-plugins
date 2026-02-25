package org.jetbrains.groovy.grails.rt;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

public final class AddAgentJarToClassPathTransformer implements ClassFileTransformer {
  private final Set<ClassLoader> oldClassLoaders = new HashSet<>();

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) throws IllegalClassFormatException {
    if (!AgentUtils.isGrailsClassLoader(loader)) {
      return null;
    }

    synchronized (oldClassLoaders) {
      if (!oldClassLoaders.add(loader)) {
        return null;
      }

      String agentClassUrl = Agent.class.getClassLoader().getResource(Agent.class.getName().replace('.', '/') + ".class").toString();
      int jarEnd = agentClassUrl.indexOf('!');
      if (!agentClassUrl.startsWith("jar:") || jarEnd == -1) {
        return null;
      }

      String jarPath = agentClassUrl.substring("jar:".length(), jarEnd);
      try {
        Method addUrlMethod = loader.getClass().getMethod("addURL", URL.class);
        addUrlMethod.invoke(loader, new URL(jarPath));
      }
      catch (Exception e) {
        System.out.println("Failed to add IDE test listener, some IDE features will be disabled");
        e.printStackTrace();
      }
    }
    return null;
  }
}

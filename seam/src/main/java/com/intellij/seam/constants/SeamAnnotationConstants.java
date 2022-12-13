package com.intellij.seam.constants;

import org.jetbrains.annotations.NonNls;

public interface SeamAnnotationConstants {
  // Component Defifnition Annotations
  @NonNls String COMPONENT_ANNOTATION = "org.jboss.seam.annotations.Name";
  @NonNls String SCOPE_ANNOTATION = "org.jboss.seam.annotations.Scope";
  @NonNls String ROLE_ANNOTATION = "org.jboss.seam.annotations.Role";
  @NonNls String ROLES_ANNOTATION = "org.jboss.seam.annotations.Roles";
  @NonNls String BYPASS_INTERCEPTORS_ANNOTATION = "org.jboss.seam.annotations.intercept.BypassInterceptors";
  @NonNls String JNDI_NAME_ANNOTATION = "org.jboss.seam.annotations.JndiName";
  @NonNls String CONVERSATIONAL_ANNOTATION = "org.jboss.seam.annotations.Conversational";
  @NonNls String STARTUP_ANNOTATION = "org.jboss.seam.annotations.Startup";
  @NonNls String INSTALL_ANNOTATION = "org.jboss.seam.annotations.Install";
  @NonNls String SYNCHRONIZED_ANNOTATION = "org.jboss.seam.annotations.Synchronized";
  @NonNls String READ_ONLY_ANNOTATION = "org.jboss.seam.annotations.ReadOnly";                                        
  @NonNls String AUTO_CREATE_ANNOTATION = "org.jboss.seam.annotations.AutoCreate";

  // Annotations for bijection
  @NonNls String IN_ANNOTATION = "org.jboss.seam.annotations.In";
  @NonNls String OUT_ANNOTATION = "org.jboss.seam.annotations.Out";

  @NonNls String UNWRAP_ANNOTATION = "org.jboss.seam.annotations.Unwrap";
  @NonNls String FACTORY_ANNOTATION = "org.jboss.seam.annotations.Factory";
  @NonNls String LOGGER_ANNOTATION = "org.jboss.seam.annotations.Logger";
  @NonNls String REQUEST_PARAMETER_ANNOTATION_1_0 = "org.jboss.seam.annotations.RequestParameter";
  @NonNls String REQUEST_PARAMETER_ANNOTATION_2_0 = "org.jboss.seam.annotations.web.RequestParameter";

  //  Annotations for component lifecycle methods
  @NonNls String CREATE_ANNOTATION = "org.jboss.seam.annotations.Create";
  @NonNls String DESTROY_ANNOTATION = "org.jboss.seam.annotations.Destroy";

  @NonNls String OBSERVER_ANNOTATION = "org.jboss.seam.annotations.Observer";
  @NonNls String RAISE_EVENT_ANNOTATION = "org.jboss.seam.annotations.RaiseEvent";

  //Annotations for context demarcation
  @NonNls String BEGIN_ANNOTATION = "org.jboss.seam.annotations.Begin";
  @NonNls String END_ANNOTATION = "org.jboss.seam.annotations.End";
  @NonNls String START_TASK_ANNOTATION = "org.jboss.seam.annotations.bpm.StartTask";
  @NonNls String BEGIN_TASK_ANNOTATION = "org.jboss.seam.annotations.bpm.BeginTask";
  @NonNls String END_TASK_ANNOTATION = "org.jboss.seam.annotations.bpm.EndTask";
  @NonNls String CREATE_PROCESS_ANNOTATION = "org.jboss.seam.annotations.bpm.CreateProcess";
  @NonNls String RESUME_PROCESS_ANNOTATION = "org.jboss.seam.annotations.bpm.ResumeProcess";
  @NonNls String TRANSITION_ANNOTATION = "org.jboss.seam.annotations.bpm.Transition";

 // Annotations for use with Seam JavaBean components in a J2EE environment
  @NonNls String J2EE_TRANSACTIONAL_ANNOTATION = "org.jboss.seam.annotations.Transactional";

  // Annotations for Seam Remoting
  @NonNls String REMOTING_WEB_REMOTE_ANNOTATION = "org.jboss.seam.annotations.remoting.WebRemote";

 
  // Annotations for use with JSF
  @NonNls String JSF_CONVERTER_ANNOTATION = "org.jboss.seam.annotations.faces.Converter";
  @NonNls String JSF_VALIDATOR_ANNOTATION = "org.jboss.seam.annotations.faces.Validator";
  @NonNls String JSF_DATA_MODEL_ANNOTATION = "org.jboss.seam.annotations.datamodel.DataModel";
  @NonNls String JSF_DATA_MODEL_SELECTION_ANNOTATION = "org.jboss.seam.annotations.datamodel.DataModelSelection";
  @NonNls String JSF_DATA_MODEL_SELECTION_INDEX_ANNOTATION = "org.jboss.seam.annotations.datamodel.DataModelSelectionIndex";
}

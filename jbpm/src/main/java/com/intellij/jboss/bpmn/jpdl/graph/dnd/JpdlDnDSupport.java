package com.intellij.jboss.bpmn.jpdl.graph.dnd;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlDataModel;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNode;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.graph.nodes.*;
import com.intellij.jboss.bpmn.jpdl.model.xml.*;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.JavaActivity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.builder.dnd.GraphDnDSupport;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.IconManager;
import com.intellij.util.Function;
import com.intellij.util.xml.DomUtil;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JpdlDnDSupport implements GraphDnDSupport<JpdlNode, JpdlNodeType> {
  private static final String UNKNOWN = "unknown";
  private final JpdlDataModel myDataModel;

  public JpdlDnDSupport(final JpdlDataModel dataModel) {
    myDataModel = dataModel;
  }


  @Override
  public Map<JpdlNodeType, Pair<String, Icon>> getDnDActions() {
    Map<JpdlNodeType, Pair<String, Icon>> nodes = new LinkedHashMap<>();

    nodes.put(JpdlNodeType.START, Pair.create("Start", JbossJbpmIcons.Jpdl.Start));
    nodes.put(JpdlNodeType.STATE, Pair.create("State", JbossJbpmIcons.Jpdl.Page));
    nodes.put(JpdlNodeType.TASK, Pair.create("Task", JbossJbpmIcons.Jpdl.Task));
    nodes.put(JpdlNodeType.JAVA, Pair.create("Java", IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Class)));
    nodes.put(JpdlNodeType.DECISIION, Pair.create("Decision", JbossJbpmIcons.Jpdl.Decision));
    nodes.put(JpdlNodeType.FORK, Pair.create("Fork", JbossJbpmIcons.Jpdl.Fork));
    nodes.put(JpdlNodeType.JOIN, Pair.create("Join", JbossJbpmIcons.Jpdl.Join));
    nodes.put(JpdlNodeType.SUBPROCESS, Pair.create("Sub Process", JbossJbpmIcons.Jpdl.SubProcess));
    nodes.put(JpdlNodeType.CUSTOM, Pair.create("Custom", JbossJbpmIcons.Jpdl.Custom));
    nodes.put(JpdlNodeType.SCRIPT, Pair.create("Script", JbossJbpmIcons.Jpdl.Script));
    nodes.put(JpdlNodeType.SQL, Pair.create("Sql", JbossJbpmIcons.Jpdl.Sql));
    nodes.put(JpdlNodeType.HQL, Pair.create("Hql", JbossJbpmIcons.Jpdl.Hql));
    nodes.put(JpdlNodeType.RULES, Pair.create("Rules", JbossJbpmIcons.Jpdl.Rule));
    nodes.put(JpdlNodeType.RULES_DECISION, Pair.create("Rules Decision", JbossJbpmIcons.Jpdl.Rule_decision));
    nodes.put(JpdlNodeType.MAIL, Pair.create("Mail", JbossJbpmIcons.Jpdl.Mail));
    nodes.put(JpdlNodeType.GROUP, Pair.create("Group", JbossJbpmIcons.Jpdl.Process));

    nodes.put(JpdlNodeType.END, Pair.create("End", JbossJbpmIcons.Jpdl.End));
    nodes.put(JpdlNodeType.END_CANCEL, Pair.create("End Cancel", JbossJbpmIcons.Jpdl.End_cancel));
    nodes.put(JpdlNodeType.END_ERROR, Pair.create("End Error", JbossJbpmIcons.Jpdl.End_error));


    return nodes;
  }

  @Override
  public boolean canStartDragging(final JpdlNodeType jpdlNodeType) {
    final ProcessDefinition processDefinition = getDataModel().getProcessDefinition();
    if (processDefinition != null) {
      if (jpdlNodeType == JpdlNodeType.START) {
        return processDefinition.getStarts().size() == 0;
      }
    }
    return true;
  }

  @Override
  public JpdlNode drop(final JpdlNodeType jpdlNodeType) {
    final ProcessDefinition processDefinition = getDataModel().getProcessDefinition();

    if (processDefinition == null) return null;

    final Project project = getDataModel().getProject();
    return switch (jpdlNodeType) {
      case STATE -> startInWCA(project, processDefinition, getDropStateFunction());
      case TASK -> startInWCA(project, processDefinition, getDropTaskFunction());
      case FORK -> startInWCA(project, processDefinition, getDropForkFunction());
      case JOIN -> startInWCA(project, processDefinition, getDropJoinFunction());
      case JAVA -> startInWCA(project, processDefinition, getDropJavaFunction());
      case CUSTOM -> startInWCA(project, processDefinition, getDropCustomFunction());
      case DECISIION -> startInWCA(project, processDefinition, getDropDecisionFunction());
      case START -> startInWCA(project, processDefinition, getDropStartFunction());
      case SUBPROCESS -> startInWCA(project, processDefinition, getDropSubProcessFunction());
      case END -> startInWCA(project, processDefinition, getDropEndFunction());
      case MAIL -> startInWCA(project, processDefinition, getDropMailFunction());
      case END_CANCEL -> startInWCA(project, processDefinition, getDropEndCancelFunction());
      case END_ERROR -> startInWCA(project, processDefinition, getDropEndErrorFunction());
      case SCRIPT -> startInWCA(project, processDefinition, getDropScriptFunction());
      case RULES -> startInWCA(project, processDefinition, getDropRuleFunction());
      case RULES_DECISION -> startInWCA(project, processDefinition, getDropRulesDecisionFunction());
      case SQL -> startInWCA(project, processDefinition, getDropSqlFunction());
      case HQL -> startInWCA(project, processDefinition, getDropHqlFunction());
      case GROUP -> startInWCA(project, processDefinition, getDropGroupFunction());
      default -> null;
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropForkFunction() {
    return processDefinition -> {
      final Fork fork = processDefinition.addFork();
      fork.getName().setStringValue(UNKNOWN);
      return new ForkNode(fork.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropJoinFunction() {
    return processDefinition -> {
      final Join join = processDefinition.addJoin();
      join.getName().setStringValue(UNKNOWN);
      return new JoinNode(join.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropJavaFunction() {
    return processDefinition -> {
      final JavaActivity javaActivity = processDefinition.addJava();
      javaActivity.getName().setStringValue(UNKNOWN);
      return new JavaNode(javaActivity.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropCustomFunction() {
    return processDefinition -> {
      final Custom custom = processDefinition.addCustom();
      custom.getName().setStringValue(UNKNOWN);
      return new CustomNode(custom.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropScriptFunction() {
    return processDefinition -> {
      final Script script = processDefinition.addScript();
      script.getName().setStringValue(UNKNOWN);
      return new ScriptNode(script.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropSqlFunction() {
    return processDefinition -> {
      final Sql sql = processDefinition.addSql();
      sql.getName().setStringValue(UNKNOWN);
      return new SqlNode(sql.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropHqlFunction() {
    return processDefinition -> {
      final Hql hql = processDefinition.addHql();
      hql.getName().setStringValue(UNKNOWN);
      return new HqlNode(hql.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropGroupFunction() {
    return processDefinition -> {
      final Group group = processDefinition.addGroup();
      group.getName().setStringValue(UNKNOWN);
      return new GroupNode(group.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropRuleFunction() {
    return processDefinition -> {
      final Rules rules = processDefinition.addRules();
      rules.getName().setStringValue(UNKNOWN);
      return new RulesNode(rules.createStableCopy());
    };
  }

  private Function<ProcessDefinition, JpdlNode> getDropRulesDecisionFunction() {
    return processDefinition -> {
      final RulesDecision rulesDecision = processDefinition.addRulesDecision();
      rulesDecision.getName().setStringValue(UNKNOWN);
      return new RulesDecisionNode(rulesDecision.createStableCopy());
    };
  }

  public JpdlDataModel getDataModel() {
    return myDataModel;
  }

  private static Function<ProcessDefinition, JpdlNode> getDropEndFunction() {
    return processDefinition -> {
      final End end = processDefinition.addEnd();
      end.getName().setStringValue(UNKNOWN);
      return new EndActivityNode.EndNode(end.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropEndCancelFunction() {
    return processDefinition -> {
      final EndCancel endCancel = processDefinition.addEndCancel();
      endCancel.getName().setStringValue(UNKNOWN);
      return new EndActivityNode.EndCancelNode(endCancel.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropEndErrorFunction() {
    return processDefinition -> {
      final EndError endError = processDefinition.addEndError();
      endError.getName().setStringValue(UNKNOWN);
      return new EndActivityNode.EndErrorNode(endError.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropStartFunction() {
    return processDefinition -> {
      final Start start = processDefinition.addStart();
      start.getName().setStringValue(UNKNOWN);
      return new StartNode(start.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropTaskFunction() {
    return processDefinition -> {
      final Task task = processDefinition.addTask();
      task.getName().setStringValue(UNKNOWN);
      return new TaskNode(task.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropSubProcessFunction() {
    return processDefinition -> {
      final SubProcess processState = processDefinition.addSubProcess();
      processState.getName().setStringValue(UNKNOWN);
      return new SubProcessNode(processState.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropStateFunction() {
    return processDefinition -> {
      final State state = processDefinition.addState();

      state.getName().setStringValue(UNKNOWN);

      return new StateNode(state.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropMailFunction() {
    return processDefinition -> {
      final Mail mail = processDefinition.addMail();

      mail.getName().setStringValue(UNKNOWN);

      return new MailNode(mail.createStableCopy());
    };
  }

  private static Function<ProcessDefinition, JpdlNode> getDropDecisionFunction() {
    return processDefinition -> {
      final Decision decision = processDefinition.addDecision();
      decision.getName().setStringValue(UNKNOWN);
      return new DecisionNode(decision.createStableCopy());
    };
  }

  private static JpdlNode startInWCA(final Project project,
                                     final ProcessDefinition processDefinition,
                                     final Function<ProcessDefinition, JpdlNode> function) {
    return WriteCommandAction.writeCommandAction(project, DomUtil.getFile(processDefinition))
      .compute(() -> function.fun(processDefinition));
  }
}

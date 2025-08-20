package com.intellij.play.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.console.LanguageConsoleBuilder;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.ide.CommonActionsManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ProcessLanguageConsoleViewWrapper extends JPanel {
  private final LanguageConsoleView myConsoleView;

  public ProcessLanguageConsoleViewWrapper(@NotNull LanguageConsoleView consoleView) {
    myConsoleView = consoleView;

    setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT));

    final DefaultActionGroup toolbarActions = new DefaultActionGroup();
    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("PlayLangConsoleView", toolbarActions, false);

    setLayout(new BorderLayout());
    add(actionToolbar.getComponent(), BorderLayout.WEST);
    add(myConsoleView.getComponent(), BorderLayout.CENTER);

    actionToolbar.setTargetComponent(this);

    final java.util.List<AnAction> actions = fillToolBarActions(toolbarActions);

    registerActionShortcuts(actions, myConsoleView.getConsoleEditor().getComponent());
    registerActionShortcuts(actions, this);

    updateUI();
  }

  public static void registerActionShortcuts(final List<? extends AnAction> actions, final JComponent component) {
    for (AnAction action : actions) {
      action.registerCustomShortcutSet(action.getShortcutSet(), component);
    }
  }

  protected List<AnAction> fillToolBarActions(final DefaultActionGroup toolbarActions) {
    List<AnAction> actionList = new ArrayList<>();

    //stop
    final AnAction stopAction = createStopAction();
    actionList.add(stopAction);

    //close
    final AnAction closeAction = createCloseAction(getProcessRunner());
    actionList.add(closeAction);

    // run action
    actionList.add(LanguageConsoleBuilder.registerExecuteAction(myConsoleView, text -> {
      try {
        getProcessRunner().runProcess(text);
      }
      catch (ExecutionException ignored) {
      }
    }, getProcessRunner().getHistoryId(), getProcessRunner().getHistoryPersistenceId(), console -> getProcessRunner().isReady()));

    // Help
    actionList.add(CommonActionsManager.getInstance().createHelpAction("interactive_console"));

    Collections.addAll(actionList, myConsoleView.createConsoleActions());

    toolbarActions.addAll(actionList);

    return actionList;
  }

  protected abstract BasicConsoleProcessRunner getProcessRunner();

  protected AnAction createCloseAction(final BasicConsoleProcessRunner processRunner) {
    return new CloseAction(null, null, myConsoleView.getProject()) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (processRunner.getState() == BasicConsoleProcessRunner.State.EXECUTING) {
          final Process process = processRunner.getProcessHandler().getProcess();
          OSProcessUtil.killProcessTree(process);
        }
        super.actionPerformed(e);
      }

      @Override
      public Executor getExecutor() {
        return processRunner.getExecutor();
      }

      @Override
      public RunContentDescriptor getContentDescriptor() {
        return processRunner.getContentDescriptor();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(true);
      }
    };
  }

  protected AnAction createStopAction() {
    return ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM);
  }
}

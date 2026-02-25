// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.DisposeAwareRunnable;
import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.groovy.mvc.ConsoleProcessDescriptor;

import javax.swing.JComponent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service(Service.Level.PROJECT)
@State(name = "GrailsConsole", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class GrailsConsole implements Disposable, PersistentStateComponent<GrailsConsoleState> {
  //private static final Key<Boolean> UPDATING_BY_CONSOLE_PROCESS = Key.create("UPDATING_BY_CONSOLE_PROCESS");
  private static final Logger LOG = Logger.getInstance(GrailsConsole.class);

  public static final @NonNls String TOOL_WINDOW_ID = "Grails Console";
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("Grails notifications");

  private final Project myProject;
  private final ConsoleViewImpl myConsole;
  private final ToolWindowEx myToolWindow;
  private final Content myContent;
  private boolean myIsAutoCloseEnabled = true;
  //private final JPanel myPanel = new JPanel(new BorderLayout());
  private final Queue<MyProcessInConsole> myProcessQueue = new LinkedList<>();
  private final MyKillProcessAction myKillAction = new MyKillProcessAction();

  private boolean myExecuting = false;

  public GrailsConsole(Project project) {
    myProject = project;
    myConsole = (ConsoleViewImpl)TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();
    Disposer.register(this, myConsole);

    myToolWindow = (ToolWindowEx)ToolWindowManager.getInstance(myProject).registerToolWindow(
      TOOL_WINDOW_ID, false, ToolWindowAnchor.BOTTOM, this, true
    );
    myToolWindow.setIcon(GroovyMvcIcons.Grails_13);
    myToolWindow.setAdditionalGearActions(new DefaultActionGroup(new ToggleAction(GrailsBundle.message("action.text.auto.close.when.done")) {

      @Override
      public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
      }

      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return myIsAutoCloseEnabled;
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean state) {
        myIsAutoCloseEnabled = state;
      }
    }));

    //Create runner UI layout
    final RunnerLayoutUi.Factory factory = RunnerLayoutUi.Factory.getInstance(myProject);
    final RunnerLayoutUi layoutUi = factory.create("", "", "session", this);
    final JComponent uiComponent = layoutUi.getComponent();

    // Adding actions
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(myKillAction);
    group.addSeparator();

    layoutUi.getOptions().setLeftToolbar(group, "GrailsConsoleToolbar");

    final Content console = layoutUi.createContent("Grails Console 111", myConsole.getComponent(), "", null, null);
    console.setCloseable(false);
    layoutUi.addContent(console, 0, PlaceInGrid.right, false);

    //myPanel.add(uiComponent, BorderLayout.CENTER);

    final ContentManager manager = myToolWindow.getContentManager();
    final ContentFactory contentFactory = ContentFactory.getInstance();
    myContent = contentFactory.createContent(uiComponent, null, true);
    manager.addContent(myContent);
  }

  @Override
  public @NotNull GrailsConsoleState getState() {
    return new GrailsConsoleState(myIsAutoCloseEnabled);
  }

  @Override
  public void loadState(@NotNull GrailsConsoleState state) {
    myIsAutoCloseEnabled = state.getAutoCloseEnabled();
  }

  public static GrailsConsole getInstance(@NotNull Project project) {
    return project.getService(GrailsConsole.class);
  }

  //public static boolean isUpdatingVfsByConsoleProcess(@NotNull Module module) {
  //  Boolean flag = module.getUserData(UPDATING_BY_CONSOLE_PROCESS);
  //  return flag != null && flag;
  //}

  public void show(final @Nullable Runnable runnable, boolean focus) {
    Runnable r = null;
    if (runnable != null) {
      r = DisposeAwareRunnable.create(runnable, myProject);
    }

    myToolWindow.activate(r, focus);
  }

  private static class MyProcessInConsole implements ConsoleProcessDescriptor {
    final GeneralCommandLine commandLine;
    final @Nullable Runnable onDone;
    final boolean closeOnDone;
    final boolean showConsole;
    final String[] input;
    private final List<ProcessListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    private OSProcessHandler myHandler;

    MyProcessInConsole(final GeneralCommandLine commandLine,
                       final @Nullable Runnable onDone,
                       final boolean showConsole,
                       final boolean closeOnDone,
                       final String[] input) {
      this.commandLine = commandLine;
      this.onDone = onDone;
      this.closeOnDone = closeOnDone;
      this.input = input;
      this.showConsole = showConsole;
    }

    @Override
    public ConsoleProcessDescriptor addProcessListener(@NotNull ProcessListener listener) {
      if (myHandler != null) {
        myHandler.addProcessListener(listener);
      }
      else {
        myListeners.add(listener);
      }
      return this;
    }

    @Override
    public ConsoleProcessDescriptor waitWith(ProgressIndicator progressIndicator) {
      if (myHandler != null) {
        doWait(progressIndicator);
      }
      return this;
    }

    private void doWait(ProgressIndicator progressIndicator) {
      while (!myHandler.waitFor(500)) {
        if (progressIndicator.isCanceled()) {
          myHandler.destroyProcess();
          break;
        }
      }
    }

    public void setHandler(OSProcessHandler handler) {
      myHandler = handler;
      for (final ProcessListener listener : myListeners) {
        handler.addProcessListener(listener);
      }
    }
  }

  public static @NotNull ConsoleProcessDescriptor executeProcess(Project project,
                                                                 final GeneralCommandLine commandLine,
                                                                 final @Nullable Runnable onDone,
                                                                 final boolean closeOnDone,
                                                                 final String... input) {
    return getInstance(project).executeProcess(commandLine, onDone, true, closeOnDone, input);
  }

  public @NotNull ConsoleProcessDescriptor executeProcess(final GeneralCommandLine commandLine,
                                                          final @Nullable Runnable onDone,
                                                          boolean showConsole,
                                                          final boolean closeOnDone,
                                                          final String... input) {
    ThreadingAssertions.assertEventDispatchThread();

    final MyProcessInConsole process = new MyProcessInConsole(commandLine, onDone, showConsole, closeOnDone, input);
    if (isExecuting()) {
      myProcessQueue.add(process);
    }
    else {
      executeProcessImpl(process, true);
    }
    return process;
  }

  public boolean isExecuting() {
    return myExecuting;
  }

  private void executeProcessImpl(final MyProcessInConsole pic, boolean toFocus) {
    final GeneralCommandLine commandLine = pic.commandLine;
    final String[] input = pic.input;
    final Runnable onDone = pic.onDone;

    myExecuting = true;

    final ModalityState modalityState = ModalityState.current();
    final boolean modalContext = modalityState != ModalityState.nonModal();

    if (!modalContext && pic.showConsole) {
      show(null, toFocus);
    }

    FileDocumentManager.getInstance().saveAllDocuments();
    final OSProcessHandler handler;
    try {
      handler = new OSProcessHandler(commandLine);

      @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
      OutputStreamWriter writer = new OutputStreamWriter(handler.getProcess().getOutputStream(), StandardCharsets.UTF_8);
      try {
        for (String s : input) {
          writer.write(s);
        }
        writer.flush();
      }
      catch (IOException e) {
        LOG.info("Cannot write Grails process input", e);
      }

      final Ref<Boolean> gotError = new Ref<>(false);
      handler.addProcessListener(new ProcessListener() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key key) {
          if (key == ProcessOutputTypes.STDERR) gotError.set(true);
          LOG.debug("got text: " + event.getText());
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          final int exitCode = event.getExitCode();
          if (exitCode == 0 && !gotError.get().booleanValue()) {
            ApplicationManager.getApplication().invokeLater(() -> {
              if (myProject.isDisposed()) return;
              if (pic.closeOnDone && myIsAutoCloseEnabled) myToolWindow.hide(null);
            }, modalityState);
          }
        }
      });
    }
    catch (final ExecutionException ex) {
      ExecutionUtil.handleExecutionError(myProject, TOOL_WINDOW_ID, "Grails", ex);
      LOG.info(ex);
      try {
        if (onDone != null) onDone.run();
      }
      catch (Exception e) {
        LOG.error(e);
      }
      myExecuting = false;
      return;
    }

    pic.setHandler(handler);
    myKillAction.setHandler(handler);

    myContent.setDisplayName(GrailsBundle.message("content.display.name.grails.executing"));
    myConsole.scrollToEnd();
    myConsole.attachToProcess(handler);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      handler.startNotify();
      if (!handler.waitFor()) return; // the process has not ended

      Integer exitCode = handler.getExitCode(); // wait until CLI creates content of directory
      if (exitCode == null || exitCode != 0) return; // something went wrong

      ApplicationManager.getApplication().invokeLater(() -> {
        if (myProject.isDisposed()) return;

        //module.putUserData(UPDATING_BY_CONSOLE_PROCESS, true);
        LocalFileSystem.getInstance().refresh(false);
        //module.putUserData(UPDATING_BY_CONSOLE_PROCESS, null);

        try {
          if (onDone != null) onDone.run();
        }
        catch (Exception e) {
          LOG.error(e);
        }
        myConsole.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
        myKillAction.setHandler(null);
        myContent.setDisplayName("");

        myExecuting = false;

        final MyProcessInConsole pic1 = myProcessQueue.poll();
        if (pic1 != null) {
          executeProcessImpl(pic1, false);
        }
      }, modalityState);
    });
  }

  @Override
  public void dispose() {
  }

  private class MyKillProcessAction extends AnAction {
    private OSProcessHandler myHandler = null;

    MyKillProcessAction() {
      super(GrailsBundle.message("action.MyKillProcessAction.description"), GrailsBundle.message("action.MyKillProcessAction.description"), AllIcons.Debugger.KillProcess);
    }

    public void setHandler(@Nullable OSProcessHandler handler) {
      myHandler = handler;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(isEnabled());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
      if (myHandler != null) {
        final Process process = myHandler.getProcess();
        process.destroy();
        myConsole.print("Process terminated", ConsoleViewContentType.ERROR_OUTPUT);
      }
    }

    public boolean isEnabled() {
      return myHandler != null;
    }
  }

  public ConsoleViewImpl getConsole() {
    return myConsole;
  }
}

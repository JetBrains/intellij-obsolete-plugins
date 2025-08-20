package com.intellij.jboss.bpmn.jbpm.ui;

import com.intellij.diagram.DiagramDataModel;
import com.intellij.jboss.bpmn.jbpm.dnd.ChartDnDSupport;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.layout.ChartPersistentLayouter;
import com.intellij.jboss.bpmn.jbpm.model.ChartSource;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.graph.builder.actions.AbstractGraphAction;
import com.intellij.openapi.graph.builder.dnd.SimpleDnDPanel;
import com.intellij.openapi.graph.impl.view.DefaultBackgroundRendererImpl;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ChartDesignerComponent<T>
  extends JPanel implements Disposable {
  public static final byte POST_EVENT = 13;

  private final MergingUpdateQueue mergingUpdateQueue;
  private final ChartBuilder<T> myBuilder;

  public ChartDesignerComponent(Project project, ChartSource source, ChartProvider<T> chartProvider) {
    setLayout(new BorderLayout());
    myBuilder = createChartBuilder(project, source, chartProvider);
    Disposer.register(this, myBuilder);
    JComponent graphComponent = myBuilder.getView().getJComponent();

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
      ActionPlaces.TOOLBAR, AbstractGraphAction.getCommonToolbarActions(), true);
    toolbar.setTargetComponent(graphComponent);
    add(toolbar.getComponent(), BorderLayout.NORTH);
    mergingUpdateQueue = new MergingUpdateQueue(chartProvider.getChartId(), 100, true, this);
    Disposer.register(this, mergingUpdateQueue);

    ChartDnDSupport dnDSupport = chartProvider.creteDnDSupport(myBuilder.getDataModel());
    if (dnDSupport != null) {
      SimpleDnDPanel simpleDnDPanel = dnDSupport.createDnDActions(project, myBuilder);
      simpleDnDPanel.getTree().setShowsRootHandles(true);

      Splitter splitter = new Splitter(false, 0.85f);
      add(splitter, BorderLayout.CENTER);
      splitter.setFirstComponent(graphComponent);
      splitter.setSecondComponent(simpleDnDPanel.getTree());
      splitter.setDividerWidth(5);
    }
    else {
      add(graphComponent);
    }

    UiNotifyConnector.installOn(this, new Activatable() {
      @Override
      public void showNotify() {
        refreshDataModel();
      }
    });
  }

  private ChartBuilder<T> createChartBuilder(Project project, ChartSource source, ChartProvider<T> chartProvider) {
    VirtualFile file = source.getFile();
    final PsiFile psiFile = file == null ? null : PsiManager.getInstance(project).findFile(file);

    final ChartBuilder<T> chartBuilder = ChartBuilderFactory.create(
      project,
      chartProvider,
      source);

    @Nullable ChartLayoutCoordinator chartLayoutCoordinator = chartBuilder.getChartLayoutCoordinator();

    chartBuilder.getView().setFitContentOnResize(true);
    chartBuilder.getPresentationModel().registerActions();

    Graph2DView view = chartBuilder.getView();
    //view.getCanvasComponent().setBackground(JBColor.background());
    ((DefaultBackgroundRendererImpl)view.getBackgroundRenderer()).setColor(JBColor.background());

    GraphSettings settings = chartBuilder.getGraphPresentationModel().getSettings();
    if (chartLayoutCoordinator != null) {
      ChartPersistentLayouter<T> chartPersistentLayouter = new ChartPersistentLayouter<>(
        chartLayoutCoordinator,
        chartProvider.getVfsResolver(),
        chartBuilder.getDataModel(),
        new PsiFile[]{psiFile},
        DocumentReference.EMPTY_ARRAY);
      settings.setCurrentLayouter(chartPersistentLayouter);
      chartBuilder.getGraph().addGraphListener(_e -> {
        if (_e.getType() == POST_EVENT) {
          chartBuilder.relayout();
        }
      });
    }
    source.addChangeListener(() -> {
      if (isShowing()) {
        scheduleUpdate();
      }
    });
    return chartBuilder;
  }

  public void scheduleUpdate() {
    mergingUpdateQueue.queue(new Update("Update") {
      @Override
      public void run() {
        refreshDataModel();
      }
    });
  }

  public void refreshDataModel() {
    final DiagramDataModel model = myBuilder.getDataModel();

    model.refreshDataModel();
    model.setModelInitializationFinished();

    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> myBuilder.queryUpdate().withDataReload().withPresentationUpdate().withRelayout().run());
  }

  @Override
  public void dispose() {
  }
}

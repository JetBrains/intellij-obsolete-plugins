// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives;

import com.intellij.openapi.graph.base.Edge;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.renderer.AbstractColoredNodeCellRenderer;
import com.intellij.openapi.graph.builder.renderer.GradientFilledPanel;
import com.intellij.openapi.graph.services.GraphSelectionService;
import com.intellij.openapi.graph.view.Graph2DSelectionEvent;
import com.intellij.openapi.graph.view.Graph2DSelectionListener;
import com.intellij.openapi.graph.view.NodeRealizer;
import com.intellij.openapi.graph.view.ViewChangeListener;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.CellRendererPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import icons.JetgroovyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.perspectives.graph.DataModelAndSelectionModificationTracker;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassNode;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassesRelationsDataModel;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DomainClassNodeRenderer extends AbstractColoredNodeCellRenderer {
  private final GraphBuilder<DomainClassNode, DomainClassRelationsInfo> myBuilder;
  private final DomainClassesRelationsDataModel myDataModel;
  private final Color myBackgroundColor = new JBColor(new Color(252, 250, 209), UIUtil.getToolTipBackground());
  private final Color myCaptionBackgroundColor = new JBColor(new Color(215, 213, 172), new Color(0x454321));

  private java.util.List<Edge> selectedEdges = new ArrayList<>();
  ViewChangeListener myViewChangeListener;

  public DomainClassNodeRenderer(@NotNull GraphBuilder<DomainClassNode, DomainClassRelationsInfo> builder, DataModelAndSelectionModificationTracker modificationTracker, DomainClassesRelationsDataModel dataModel) {
    super(modificationTracker);
    myBuilder = builder;
    myDataModel = dataModel;

    myBuilder.getView().getGraph2D().addGraph2DSelectionListener(new Graph2DSelectionListener() {
      @Override
      public void onGraph2DSelectionEvent(Graph2DSelectionEvent event) {
        if (event.isEdgeSelection()) {
          selectedEdges = GraphSelectionService.getInstance().getSelectedEdges(event.getGraph2D());
        }
      }
    });

     myBuilder.getView().getGraph2D().addGraph2DSelectionListener(modificationTracker);
  }

  @Override
  public void tuneNode(NodeRealizer realizer, JPanel wrapper) {
    final Icon entityIcon = JetgroovyIcons.Groovy.Class;
    final Icon varNameIcon = JetgroovyIcons.Groovy.Property;

    Node node = realizer.getNode();
    DomainClassNode domainClassNode = myBuilder.getNodeObject(node);
    @NlsSafe String nodeName = myBuilder.getNodeName(domainClassNode);
    JLabel nodeNameLabel = new JLabel(nodeName, entityIcon, SwingConstants.CENTER);
    nodeNameLabel.setBorder(JBUI.Borders.empty(3));
    nodeNameLabel.setHorizontalAlignment(SwingConstants.LEFT);

    GradientFilledPanel namePanel = new GradientFilledPanel(myCaptionBackgroundColor);
    namePanel.setLayout(new BorderLayout());
    namePanel.add(nodeNameLabel, BorderLayout.CENTER);
    namePanel.setBorder(BorderFactory.createLineBorder(JBColor.WHITE, 1));

    namePanel.setGradientColor(new JBColor(new Color(186, 222, 193), LightColors.SLIGHTLY_GREEN));

    nodeNameLabel.setForeground(JBColor.foreground());
    wrapper.add(namePanel, BorderLayout.NORTH);

    assert domainClassNode != null;

    final List<DomainClassRelationsInfo> outEdges = myDataModel.getNodesToOutsMap().get(domainClassNode);

    if (outEdges != null) {
      Iterator<DomainClassRelationsInfo> iterator = outEdges.iterator();

      JPanel relationsPanel = new CellRendererPanel(new GridBagLayout());
      relationsPanel.setBorder(JBUI.Borders.empty(2, 5));
      relationsPanel.setBackground(myBackgroundColor);

      boolean isBold;
      if (!outEdges.isEmpty()) {
        while (iterator.hasNext()) {
          DomainClassRelationsInfo edge = iterator.next();
          isBold = selectedEdges.contains(myBuilder.getEdge(edge));

          final @NlsSafe String varName = edge.getVarName();
          final JLabel varNamePanel = new JLabel(varName);
          if (isBold) {
            varNamePanel.setFont(StartupUiUtil.getLabelFont().deriveFont(Font.BOLD));
          }

          varNamePanel.setIcon(varNameIcon);

          final String type = edge.getTarget().getTypeDefinition().getName();

          final JLabel typeLabel = new JLabel(type);
          if (isBold) {
            typeLabel.setFont(StartupUiUtil.getLabelFont().deriveFont(Font.BOLD));
          }

          final JPanel typePanel = new CellRendererPanel(new BorderLayout());
          typePanel.add(typeLabel, BorderLayout.EAST);

          relationsPanel.add(varNamePanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1, 0,
                                                                  GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
                                                                  JBUI.insets(2), 0, 0));
          relationsPanel.add(typePanel, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1, 0, GridBagConstraints.LINE_END,
              GridBagConstraints.BOTH, JBUI.insets(2), 0, 0));
        }

        Dimension preferredSize = relationsPanel.getPreferredSize();
        relationsPanel.setPreferredSize(new Dimension((int) preferredSize.getWidth() + 20, (int) preferredSize.getHeight()));

      } else {
        Dimension preferredSize = nodeNameLabel.getPreferredSize();
        nodeNameLabel.setPreferredSize(new Dimension((int) preferredSize.getWidth() + 25, (int) preferredSize.getHeight()));
      }

      wrapper.add(relationsPanel);
    }
  }
}

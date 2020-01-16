package com.intellij.javaee.heroku.cloud.action;

import com.intellij.javaee.heroku.cloud.HerokuApplicationRuntime;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.impl.runtime.ui.RemoteServersDeploymentManager;
import com.intellij.remoteServer.impl.runtime.ui.tree.DeploymentNode;
import com.intellij.remoteServer.impl.runtime.ui.tree.ServersTreeNodeSelector;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoteServer.util.ApplicationActionUtils.*;

public class ShowLogAction extends DumbAwareAction {
    private final static String LOG_NAME = "Log";

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(getApplicationRuntime(e, HerokuApplicationRuntime.class) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ServersTreeNodeSelector selector = RemoteServersDeploymentManager.getNodeSelector(e);
        if (selector == null) return;

        DeploymentNode node = getDeploymentTarget(e);
        HerokuApplicationRuntime applicationRuntime = getApplicationRuntime(node, HerokuApplicationRuntime.class);
        if (applicationRuntime == null) return;

        applicationRuntime.showLog(project, createLogSelector(project, selector, node, LOG_NAME));
    }
}

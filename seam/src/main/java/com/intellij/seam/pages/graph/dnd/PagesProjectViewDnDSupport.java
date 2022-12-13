package com.intellij.seam.pages.graph.dnd;

import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.jsf.yfilesGraph.dnd.BasicWebFacetProjectViewDnDSupport;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pages.graph.beans.BasicPagesEdge;
import com.intellij.seam.pages.graph.beans.BasicPagesNode;
import com.intellij.seam.pages.graph.beans.PageNode;
import com.intellij.seam.pages.xml.PagesDomModelManager;
import com.intellij.seam.pages.xml.PagesModel;
import com.intellij.seam.pages.xml.pages.Page;
import com.intellij.seam.pages.xml.pages.Pages;
import com.intellij.util.Function;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PagesProjectViewDnDSupport extends BasicWebFacetProjectViewDnDSupport<BasicPagesNode, BasicPagesEdge> {
  private final XmlFile myXmlFile;

  public PagesProjectViewDnDSupport(final XmlFile xmlFile,
                                    GraphBuilder<BasicPagesNode, BasicPagesEdge> builder,
                                    @NotNull final WebFacet facetScope) {
    super(builder, facetScope);
    myXmlFile = xmlFile;
  }


  private static Function<Pages, PageNode> getPageNodeFunction(@NotNull @NlsSafe String webPath) {
    return pages -> {
      Page page = pages.addPage();
      page.getViewId().setStringValue(webPath);
      return new PageNode(page.createStableCopy(), webPath);
    };
  }

  private static PageNode startInWCA(final Pages pages, final Function<? super Pages, ? extends PageNode> function) {
    return WriteCommandAction.writeCommandAction(pages.getManager().getProject(), DomUtil.getFile(pages))
                             .compute(() -> function.fun(pages));

  }

  @Nullable
  private Pages getPages() {
    final PagesModel pagesModel = PagesDomModelManager.getInstance(myXmlFile.getProject()).getPagesModel(myXmlFile);
    if (pagesModel != null) {
      return pagesModel.getRoots().get(0).getRootElement();
    }
    return null;
  }

  @Override
  @Nullable
  protected BasicPagesNode createNodeObject(@NotNull final String webPath) {
    final Pages pages = getPages();

    return pages != null ? startInWCA(pages, getPageNodeFunction(webPath)) : null;
  }

  @Override
  protected boolean areNodesEquals(@NotNull final String webPath, @NotNull final BasicPagesNode nodeObject) {
    return webPath.equals(nodeObject.getName());
  }
}


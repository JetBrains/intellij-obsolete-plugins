package com.intellij.dmserver.libraries.obr;

import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.libraries.*;
import com.intellij.dmserver.libraries.obr.data.*;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.event.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

public class DownloadBundlesEditor {

  @NonNls
  public static final String URL_BASE = "https://www.springsource.com";

  private JButton mySearchButton;
  private JTextField mySearchText;
  private JButton myDownloadButton;
  private JCheckBox myIncludeDependenciesCheckbox;
  private JButton myBundleDepsToResultsButton;
  private JButton myLibraryDepsToResultsButton;
  private JButton myClearButton;
  private JPanel myLibrariesGroup;
  private JPanel myBundlesGroup;
  private JPanel myMainPanel;
  private JScrollPane myLibrariesPane;
  private JScrollPane myLibraryDepsPane;
  private JScrollPane myBundlesPane;
  private JScrollPane myBundleDepsPane;
  private JLabel myStatusLabel;
  private JTable myLibrariesTable;
  private JTable myLibraryDepsTable;
  private JTable myBundlesTable;
  private JTable myBundleDepsTable;
  private DownloadTargetEditor myBundleTargetEditor;
  private DownloadTargetEditor myLibraryTargetEditor;

  private BundlesProcessor myBundlesProcessor;

  private LibrariesProcessor myLibrariesProcessor;

  private final ServerLibrariesContext myContext;

  private EnableBehavior myEnableBehavior;

  public DownloadBundlesEditor(ServerLibrariesContext context) {
    myContext = context;
    getBundlesProcessor().init();
    getLibrariesProcessor().init();

    setupComponents();

    setupEnableBehavior();
  }

  private BundlesProcessor getBundlesProcessor() {
    if (myBundlesProcessor == null) {
      myBundlesProcessor = new BundlesProcessor();
    }
    return myBundlesProcessor;
  }

  private LibrariesProcessor getLibrariesProcessor() {
    if (myLibrariesProcessor == null) {
      myLibrariesProcessor = new LibrariesProcessor();
    }
    return myLibrariesProcessor;
  }

  private boolean isBundleDownloaded(BundleData bundleData) {
    return getBundlesProcessor().isComponentDownloaded(bundleData);
  }

  private void createUIComponents() {
    myLibrariesTable = getLibrariesProcessor().getTable();
    myLibraryDepsTable = getLibrariesProcessor().getDependencyTable();
    myBundlesTable = getBundlesProcessor().getTable();
    myBundleDepsTable = getBundlesProcessor().getDependencyTable();
  }

  private void setupComponents() {

    mySearchText.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSearch();
      }
    });
    mySearchButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doSearch();
      }
    });

    getBundlesProcessor().setupDepsToResultsButton(myBundleDepsToResultsButton);
    getLibrariesProcessor().setupDepsToResultsButton(myLibraryDepsToResultsButton);


    myDownloadButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doDownload();
      }
    });

    myClearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doClear();
      }
    });

    myBundleTargetEditor.init(myContext.getInstallation());
    myLibraryTargetEditor.init(myContext.getInstallation());
  }

  private void setupEnableBehavior() {

    myEnableBehavior = new EnableBehavior();

    myEnableBehavior.addComponent(mySearchButton, new EnableStateEvaluator() {
      @Override
      public boolean isEnabled() {
        return mySearchText.getDocument().getLength() > 0;
      }
    });
    myEnableBehavior.addComponent(myDownloadButton, new EnableStateEvaluator() {
      @Override
      public boolean isEnabled() {
        return getBundlesProcessor().hasComponentsToDownload() || getLibrariesProcessor().hasComponentsToDownload();
      }
    });
    myEnableBehavior.addComponent(myClearButton, new EnableStateEvaluator() {
      @Override
      public boolean isEnabled() {
        return getBundlesProcessor().hasFoundComponents() || getLibrariesProcessor().hasFoundComponents();
      }
    });

    getBundlesProcessor().setupEnableBehavior();
    getLibrariesProcessor().setupEnableBehavior();

    mySearchText.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myEnableBehavior.updateEnableState(mySearchButton);
      }
    });

    myEnableBehavior.updateAllEnableStates();
  }

  private void doClear() {
    myBundlesProcessor.clearComponents();
    myLibrariesProcessor.clearComponents();
  }

  private void setStatusText(@Nls String statusText) {
    myStatusLabel.setText(statusText);
  }

  private void startTask(@Nls String title, final DownloadTask task) {
    setStatusText("");
    new Task.Modal(myContext.getProject(), title, true) {

      @Override
      public void run(final @NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        task.run(new DownloadTaskIndicator() {

          @Override
          public void setProgressText(@Nls String progressText) {
            indicator.setText(progressText);
          }

          @Override
          public boolean isCancelled() {
            return indicator.isCanceled();
          }
        });
      }

      @Override
      public void onFinished() {
        task.postRun();
        myBundlesProcessor.refreshTable();
        myLibrariesProcessor.refreshTable();
      }
    }.queue();
  }

  private void doSearch() {
    if (StringUtil.isEmpty(mySearchText.getText())) {
      return;
    }

    startTask(getSearchProgressText(), new DownloadTaskBase() {

      @Override
      public void run(DownloadTaskIndicator indicator) {
        try {
          String searchText;
          searchText = URLEncoder.encode(mySearchText.getText(), StandardCharsets.UTF_8);

          int[] refLibrariesCount = new int[1];
          int[] refBundlesCount = new int[1];

          if (!getLibrariesProcessor().search(searchText, refLibrariesCount,
                                              DmServerBundle.message("DownloadBundlesEditor.status.searching.for.libraries"),
                                              "DownloadBundlesEditor.status.collecting.library.details", indicator)) {
            setResultText(
              DmServerBundle.message("DownloadBundlesEditor.result.details.collecting.interrupted.libs-only", refLibrariesCount[0]));
          }
          else if (!getBundlesProcessor().search(searchText, refBundlesCount,
                                                 DmServerBundle.message("DownloadBundlesEditor.status.searching.for.bundles"),
                                                 "DownloadBundlesEditor.status.collecting.bundle.details", indicator)) {
            setResultText(
              DmServerBundle.message("DownloadBundlesEditor.result.details.collecting.interrupted.libs-and-bundles", refLibrariesCount[0],
                                     refBundlesCount[0]));
          }
          else {
            setResultText(
              refLibrariesCount[0] == 0 && refBundlesCount[0] == 0 //
              ? DmServerBundle.message("DownloadBundlesEditor.result.nothing") //
              : DmServerBundle.message("DownloadBundlesEditor.result.ok.libs-and-bundles", refLibrariesCount[0], refBundlesCount[0]));
          }
        }
        catch (IOException | XPathExpressionException e) {
          setResultException(e);
        }
      }
    });
  }

  private void doDownload() {
    final Map<String, LibraryData> librariesToDownload = new HashMap<>();
    final Map<String, BundleData> bundlesToDownload = new HashMap<>();

    getLibrariesProcessor().collectComponentsToDownload(librariesToDownload, bundlesToDownload);
    getBundlesProcessor().collectComponentsToDownload(bundlesToDownload, bundlesToDownload);


    if (librariesToDownload.isEmpty() && bundlesToDownload.isEmpty()) {
      return;
    }

    startTask(getDownloadProgressText(), new DownloadTaskBase() {

      private final ArrayList<File> myBundleJars = new ArrayList<>();
      private final ArrayList<File> myLibraryDefs = new ArrayList<>();

      @Override
      public void run(DownloadTaskIndicator indicator) {
        try {
          if (getBundlesProcessor()
                .download(bundlesToDownload.values(), myBundleJars, "DownloadBundlesEditor.status.downloading.bundles", indicator)
              &&
              getLibrariesProcessor()
                .download(librariesToDownload.values(), myLibraryDefs, "DownloadBundlesEditor.status.downloading.libraries", indicator)) {

            new ChainIndicator(indicator, DmServerBundle.message("DownloadBundlesEditor.status.updating.index"));
            AvailableBundlesProvider.getInstance(myContext.getProject()).resetRepositoryIndex();

            setResultText(DmServerBundle.message("DownloadBundlesEditor.result.download.successful"));
          }
          else {
            setResultText(DmServerBundle.message("DownloadBundlesEditor.result.download.interrupted"));
          }
        }
        catch (IOException | XPathExpressionException e) {
          setResultException(e);
        }
        finally {
          getBundlesProcessor().updateDownloadedRows();
          getLibrariesProcessor().updateDownloadedRows();
        }
      }
    });
  }

  public void initSearch(String packageName) {
    mySearchText.setText(packageName);
  }

  public Component getMainPanel() {
    return myMainPanel;
  }

  public static abstract class BundleColumn extends Column<BundleData> {

    public BundleColumn(String name) {
      super(name);
    }
  }

  public static abstract class LibraryBundleColumn extends Column<LibraryBundleData> {

    public LibraryBundleColumn(String name) {
      super(name);
    }
  }

  public static abstract class BundleDownloadColumn extends Column<BundleDownloadData> {

    public BundleDownloadColumn(String name) {
      super(name);
    }
  }

  public static abstract class LibraryDownloadColumn extends Column<LibraryDownloadData> {

    public LibraryDownloadColumn(String name) {
      super(name);
    }
  }

  private interface DownloadTaskIndicator extends ProgressListener {

    boolean isCancelled();
  }

  private static class ChainIndicator implements DownloadTaskIndicator {

    private final DownloadTaskIndicator myParent;
    private final String myPrefix;

    ChainIndicator(DownloadTaskIndicator parent, String prefix) {
      myParent = parent;
      myPrefix = prefix;
      myParent.setProgressText(myPrefix);
    }

    @Override
    public void setProgressText(String progressText) {
      myParent.setProgressText(DmServerBundle.message("DownloadBundlesEditor.status.chain", myPrefix, progressText));
    }

    @Override
    public boolean isCancelled() {
      return myParent.isCancelled();
    }
  }

  private interface DownloadTask {

    void run(DownloadTaskIndicator indicator);

    void postRun();
  }

  private abstract class DownloadTaskBase implements DownloadTask {

    private Exception myThrownException = null;

    @Nls
    private String myStatusText = null;

    @Override
    public void postRun() {
      if (myThrownException != null) {
        setStatusText(DmServerBundle.message("DownloadBundlesEditor.RunnableTaskBase.error.generic-error", myThrownException.getMessage()));
      }
      else if (myStatusText != null) {
        setStatusText(myStatusText);
      }
    }

    public void setResultText(@Nls String statusText) {
      myStatusText = statusText;
    }

    public void setResultException(Exception thrownException) {
      myThrownException = thrownException;
    }
  }

  private abstract class CodeComponentProcessor<DR extends BundleData, DC extends Column<DR>, D extends AbstractCodeData<? extends AbstractCodeDataDetails<DR>>, SR extends AbstractCodeDownloadData<D>, SC extends Column<SR>> {

    protected final XPathUtils XPATH = XPathUtils.getInstance();

    private Set<String> myDownloadedComponentIDs;

    private JTableWrapper<SR, SC> myTableWrapper;

    private final ArrayList<SR> myComponentRows = new ArrayList<>();

    private JTableWrapper<DR, DC> myDependencyTableWrapper;

    private JButton myDepsToResultsButton;

    public void init() {
      initDownloadedComponents(getDownloadedComponents());
    }

    @Nullable
    protected abstract VirtualFile getDestinationFolder();

    protected abstract List<D> getDownloadedComponents();

    private void initDownloadedComponents(List<D> downloadedCodeComponents) {
      myDownloadedComponentIDs = new HashSet<>();
      for (D downloadedCodeComponent : downloadedCodeComponents) {
        myDownloadedComponentIDs.add(downloadedCodeComponent.getID());
      }
    }

    public JBTable getTable() {
      if (myTableWrapper == null) {
        myTableWrapper = new GrayableTableWrapper<>(createTableColumns()) {

          @Override
          public GrayableTableModelBase createTableModel() {
            return new GrayableTableModelBase() {

              @Override
              public boolean isCellEditable(int row, int column) {
                return super.isCellEditable(row, column) && !isComponentDownloaded(getRows().get(row).getCodeData());
              }

              @Override
              public boolean isCellEnabled(int row, int column) {
                return !isComponentDownloaded(getRows().get(row).getCodeData());
              }
            };
          }
        };

        ListSelectionModel selectionModel = myTableWrapper.getTable().getSelectionModel();

        selectionModel.addListSelectionListener(new ListSelectionListener() {

          @Override
          public void valueChanged(ListSelectionEvent event) {
            refreshDependencyTable();
          }
        });
      }

      return myTableWrapper.getTable();
    }

    public boolean isComponentDownloaded(D codeComponent) {
      return myDownloadedComponentIDs.contains(codeComponent.getID());
    }

    protected final List<SR> getRows() {
      return myComponentRows;
    }

    public boolean hasComponentsToDownload() {
      for (SR row : getRows()) {
        if (row.getDownload()) {
          return true;
        }
      }
      return false;
    }


    public boolean hasFoundComponents() {
      return !getRows().isEmpty();
    }


    public void setupEnableBehavior() {
      myEnableBehavior.addComponent(myDepsToResultsButton, new EnableStateEvaluator() {
        @Override
        public boolean isEnabled() {
          return myDependencyTableWrapper.getTable().getSelectedRowCount() > 0;
        }
      });
      myDependencyTableWrapper.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          myEnableBehavior.updateEnableState(myDepsToResultsButton);
        }
      });

      myTableWrapper.getTable().getModel().addTableModelListener(new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
          myEnableBehavior.updateEnableState(myDownloadButton);
          myEnableBehavior.updateEnableState(myClearButton);
        }
      });
    }

    public void clearComponents() {
      getRows().clear();
      refreshTable();
    }

    public void refreshTable() {
      myTableWrapper.setInputRows(getRows());
    }


    public JBTable getDependencyTable() {
      if (myDependencyTableWrapper == null) {
        myDependencyTableWrapper = new GrayableTableWrapper<>(createDependencyTableColumns()) {

          @Override
          public GrayableTableModelBase createTableModel() {
            return new GrayableTableModelBase() {

              @Override
              public boolean isCellEnabled(int row, int column) {
                return !isBundleDownloaded(getDependency(row));
              }
            };
          }
        };

        myDependencyTableWrapper.getTable().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        myDependencyTableWrapper.getTable().addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent event) {
            JTable table = (JTable)event.getSource();
            Point point = event.getPoint();
            if (event.getClickCount() == 2) {
              int clickedRow = table.rowAtPoint(point);
              if (clickedRow != -1) {
                doMoveToResults(new int[]{clickedRow});
              }
            }
          }
        });
      }
      return myDependencyTableWrapper.getTable();
    }

    private void refreshDependencyTable() {
      int selectedRow = myTableWrapper.getTable().getSelectedRow();
      myDependencyTableWrapper.setInputRows(
        selectedRow == -1 ? Collections.emptyList() : getRows().get(selectedRow).getCodeData().getDetails().getDependencies());
    }

    public boolean search(String searchText,
                          int[] refResultLength,
                          @Nls String searchStatusText,
                          @NonNls @PropertyKey(resourceBundle = DmServerBundle.BUNDLE) String detailsStatusBundleKey,
                          DownloadTaskIndicator indicator)
      throws XPathExpressionException, IOException {

      indicator.setProgressText(searchStatusText);

      String lowerParamPrefix = StringUtil.toLowerCase(getParamPrefix());
      char[] chars = lowerParamPrefix.toCharArray();
      chars[0] = StringUtil.toUpperCase(new String(new char[]{chars[0]})).charAt(0);
      String upperParamPrefix = new String(chars);

      @NonNls String searchUrlFormat =
        "{0}/repository/app/search?query={1}&include{2}=true&_include{2}=on&version=&pageSize={3,number,0}&page=1&type={4}BySymbolicName";
      int PAGE_SIZE = 1000;
      String searchUrl = MessageFormat.format(searchUrlFormat, URL_BASE, searchText, upperParamPrefix, PAGE_SIZE, lowerParamPrefix);

      NodeList componentNodeList = (NodeList)XPATH.evaluateXPath("//div[@id='results-fragment']/ul[1]/li", //
                                                                 XPATH.createHtmlRoot(HttpRetriever.retrievePage(searchUrl)),
                                                                 XPathConstants.NODESET);

      refResultLength[0] = componentNodeList.getLength();

      if (componentNodeList.getLength() != 0) {

        for (int iComponent = 0; iComponent < componentNodeList.getLength(); iComponent++) {
          if (indicator.isCancelled()) {
            return false;
          }

          DownloadTaskIndicator childIndicator =
            new ChainIndicator(indicator, DmServerBundle.message(detailsStatusBundleKey, iComponent + 1, componentNodeList.getLength()));

          Node componentNode = componentNodeList.item(iComponent);

          SR row = createRow(XPATH.evaluateXPath("a/text()", componentNode).trim(), //
                             XPATH.evaluateXPath("text()[2]", componentNode).trim(), //
                             URL_BASE + XPATH.evaluateXPath("a/@href", componentNode).trim());

          row.getCodeData().loadDetails(childIndicator);

          getRows().add(row);
        }
      }
      return true;
    }


    private BundleData getDependency(int iRow) {
      int selectedBundleRow = myTableWrapper.getTable().getSelectedRow();
      if (selectedBundleRow == -1) {
        return null;
      }
      return getRows().get(selectedBundleRow).getCodeData().getDetails().getDependencies().get(iRow);
    }

    private void markDownloaded(D component) {
      myDownloadedComponentIDs.add(component.getID());
    }

    public void updateDownloadedRows() {
      for (SR row : getRows()) {
        if (myDownloadedComponentIDs.contains(row.getCodeData().getID())) {
          row.setDownload(false);
        }
      }
    }

    private void doMoveToResults(int[] selectedDependencyRows) {
      final ArrayList<BundleData> dependencies = new ArrayList<>();
      for (int selectedDependencyRow : selectedDependencyRows) {
        BundleData dependency = getDependency(selectedDependencyRow);
        if (dependency != null) {
          dependencies.add(dependency);
        }
      }
      if (dependencies.isEmpty()) {
        return;
      }

      startTask(getToResultsProgressText(), new DownloadTaskBase() {

        @Override
        public void run(DownloadTaskIndicator indicator) {
          try {
            int iBundle = 0;
            for (BundleData dependency : dependencies) {
              iBundle++;
              DownloadTaskIndicator childIndicator = new ChainIndicator(indicator, DmServerBundle.message(
                "DownloadBundlesEditor.status.collecting.details.for.dependencies", iBundle, dependencies.size()));

              BundleDownloadData bundleRow = new BundleDownloadData(dependency.getName(), dependency.getVersion(), dependency.getLink());
              bundleRow.getCodeData().loadDetails(childIndicator);
              getBundlesProcessor().addRow(bundleRow);
              if (indicator.isCancelled()) {
                setResultText(DmServerBundle.message("DownloadBundlesEditor.result.details.collecting.interrupted"));
                return;
              }
            }
            setResultText(DmServerBundle.message("DownloadBundlesEditor.result.details.collecting.successful"));
          }
          catch (XPathExpressionException | IOException e) {
            setResultException(e);
          }
        }
      });
    }

    public void setupDepsToResultsButton(JButton depsToResultsButton) {
      myDepsToResultsButton = depsToResultsButton;
      depsToResultsButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doMoveToResults(myDependencyTableWrapper.getTable().getSelectedRows());
        }
      });
    }

    public void collectComponentsToDownload(Map<String, D> componentsToDownload, Map<String, BundleData> dependenciesToDownload) {
      for (SR row : getRows()) {
        if (row.getDownload()) {
          componentsToDownload.put(row.getCodeData().getID(), row.getCodeData());
          for (DR dependency : row.getCodeData().getDetails().getDependencies()) {
            if (shouldDownloadDependency(dependency)) {
              dependenciesToDownload.put(dependency.getID(), dependency);
            }
          }
        }
      }
    }

    protected boolean shouldDownloadDependency(DR dependency) {
      return myIncludeDependenciesCheckbox.isSelected();
    }

    public boolean download(Collection<D> components2download,
                            List<File> componentFiles,
                            @NonNls @PropertyKey(resourceBundle = DmServerBundle.BUNDLE) String downloadingStatusText,
                            DownloadTaskIndicator indicator)
      throws IOException, XPathExpressionException {
      int iComponent = 0;
      for (D componentToDownload : components2download) {
        iComponent++;
        DownloadTaskIndicator childIndicator =
          new ChainIndicator(indicator, DmServerBundle.message(downloadingStatusText, iComponent, components2download.size()));
        if (indicator.isCancelled()) {
          return false;
        }

        if (!downloadComponent(componentToDownload, componentFiles, childIndicator)) {
          return false;
        }
        markDownloaded(componentToDownload);
      }
      return true;
    }


    protected final File downloadCodeElement(String link, ProgressListener progressListener) throws IOException {
      VirtualFile destinationFolder = getDestinationFolder();
      if (destinationFolder == null) {
        throw new IOException("Destination folder does not exist");
      }
      return HttpRetriever.downloadFile(link, VfsUtilCore.virtualToIoFile(destinationFolder), progressListener);
    }

    protected abstract boolean downloadComponent(D componentToDownload, List<File> componentFiles, DownloadTaskIndicator indicator)
      throws IOException, XPathExpressionException;

    protected abstract SC[] createTableColumns();

    protected abstract DC[] createDependencyTableColumns();

    protected abstract SR createRow(String name, String version, String link);

    @NonNls
    protected abstract String getParamPrefix();
  }

  private class BundlesProcessor
    extends CodeComponentProcessor<BundleData, BundleColumn, BundleData, BundleDownloadData, BundleDownloadColumn> {

    @Override
    protected BundleDownloadColumn[] createTableColumns() {
      return new BundleDownloadColumn[]{
        new BundleDownloadColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.add")) {

          @Override
          public Object getColumnValue(BundleDownloadData row) {
            return row.getDownload();
          }

          @Override
          public Class<?> getValueClass() {
            return Boolean.class;
          }

          @Override
          public boolean isEditable() {
            return true;
          }

          @Override
          public void setColumnValue(int iRow, Object value) {
            getRows().get(iRow).setDownload((Boolean)value);
          }

          @Override
          public boolean needPack() {
            return true;
          }
        }, new BundleDownloadColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.name")) {

        @Override
        public Object getColumnValue(BundleDownloadData row) {
          return row.getCodeData().getName();
        }
      }, new BundleDownloadColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.version")) {

        @Override
        public Object getColumnValue(BundleDownloadData row) {
          return row.getCodeData().getVersion();
        }

        @Override
        public boolean needPack() {
          return true;
        }
      }};
    }

    @Override
    protected BundleColumn[] createDependencyTableColumns() {
      return new BundleColumn[]{ //
        new BundleColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.name")) {

          @Override
          public String getColumnValue(BundleData row) {
            return row.getName();
          }
        }, new BundleColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.version")) {

        @Override
        public String getColumnValue(BundleData row) {
          return row.getVersion();
        }

        @Override
        public boolean needPack() {
          return true;
        }
      }};
    }

    @Override
    protected BundleDownloadData createRow(String name, String version, String link) {
      return new BundleDownloadData(name, version, link);
    }

    @NonNls
    @Override
    protected String getParamPrefix() {
      return "bundles";
    }

    public void addRow(BundleDownloadData row) {
      getRows().add(row);
    }

    @Override
    protected boolean downloadComponent(BundleData componentToDownload, List<File> componentFiles, DownloadTaskIndicator indicator)
      throws IOException, XPathExpressionException {
      componentToDownload.loadDetails(
        new ChainIndicator(indicator, DmServerBundle.message("DownloadBundlesEditor.status.collecting.details")));
      if (indicator.isCancelled()) {
        return false;
      }
      componentFiles.add(downloadCodeElement(componentToDownload.getDetails().getBinaryJarLink(),
                                             new ChainIndicator(indicator, DmServerBundle.message(
                                               "DownloadBundlesEditor.status.downloading.binary.jar"))));
      if (indicator.isCancelled()) {
        return false;
      }
      if (componentToDownload.getDetails().getSourceJarLink() != null) {
        componentFiles.add(downloadCodeElement(componentToDownload.getDetails().getSourceJarLink(),
                                               new ChainIndicator(indicator, DmServerBundle.message(
                                                 "DownloadBundlesEditor.status.downloading.source.jar"))));
      }
      return true;
    }

    @Override
    protected List<BundleData> getDownloadedComponents() {
      List<BundleData> result = new ArrayList<>();
      for (BundleDefinition bundleDef :
        AvailableBundlesProvider.getInstance(myContext.getProject()).getAllRepositoryBundles()) {
        result.add(new BundleData(bundleDef));
      }
      return result;
    }

    @Nullable
    @Override
    protected VirtualFile getDestinationFolder() {
      return myBundleTargetEditor.getTargetDir();
    }
  }

  private class LibrariesProcessor
    extends CodeComponentProcessor<LibraryBundleData, LibraryBundleColumn, LibraryData, LibraryDownloadData, LibraryDownloadColumn> {

    @Override
    protected LibraryDownloadColumn[] createTableColumns() {
      return new LibraryDownloadColumn[]{//
        new LibraryDownloadColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.add")) {

          @Override
          public Object getColumnValue(LibraryDownloadData row) {
            return row.getDownload();
          }

          @Override
          public Class<?> getValueClass() {
            return Boolean.class;
          }

          @Override
          public boolean isEditable() {
            return true;
          }

          @Override
          public void setColumnValue(int iRow, Object value) {
            getRows().get(iRow).setDownload((Boolean)value);
          }

          @Override
          public boolean needPack() {
            return true;
          }
        }, new LibraryDownloadColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.name")) {

        @Override
        public Object getColumnValue(LibraryDownloadData row) {
          return row.getCodeData().getName();
        }
      }, new LibraryDownloadColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.version")) {

        @Override
        public Object getColumnValue(LibraryDownloadData row) {
          return row.getCodeData().getVersion();
        }

        @Override
        public boolean needPack() {
          return true;
        }
      }};
    }

    @Override
    protected LibraryBundleColumn[] createDependencyTableColumns() {
      return new LibraryBundleColumn[]{ //
        new LibraryBundleColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.in-library")) {

          @Override
          public Object getColumnValue(LibraryBundleData row) {
            return row.isIncluded();
          }

          @Override
          public Class<?> getValueClass() {
            return Boolean.class;
          }

          @Override
          public boolean needPack() {
            return true;
          }
        }, new LibraryBundleColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.name")) {

        @Override
        public String getColumnValue(LibraryBundleData row) {
          return row.getName();
        }
      }, new LibraryBundleColumn(DmServerBundle.message("DownloadBundlesEditor.BundlesProcessor.column.version")) {

        @Override
        public String getColumnValue(LibraryBundleData row) {
          return row.getVersion();
        }

        @Override
        public boolean needPack() {
          return true;
        }
      }};
    }


    @Override
    protected LibraryDownloadData createRow(String name, String version, String link) {
      return new LibraryDownloadData(name, version, link);
    }

    @NonNls
    @Override
    protected String getParamPrefix() {
      return "libraries";
    }

    @Override
    protected boolean downloadComponent(LibraryData componentToDownload, List<File> componentFiles, DownloadTaskIndicator indicator)
      throws IOException, XPathExpressionException {
      componentFiles.add(downloadCodeElement(componentToDownload.getDetails().getLibraryDefLink(),
                                             new ChainIndicator(indicator,
                                                                DmServerBundle.message(
                                                                  "DownloadBundlesEditor.status.downloading.library.definition"))));
      return true;
    }

    @Override
    protected boolean shouldDownloadDependency(LibraryBundleData dependency) {
      return super.shouldDownloadDependency(dependency) || dependency.isIncluded();
    }

    @Override
    protected List<LibraryData> getDownloadedComponents() {
      List<LibraryData> result = new ArrayList<>();
      for (LibraryDefinition libraryDef
        : AvailableBundlesProvider.getInstance(myContext.getProject()).getAllRepositoryLibraries()) {
        if (areLibraryBundlesDownloaded(libraryDef)) {
          result.add(new LibraryData(libraryDef.getSymbolicName(), libraryDef.getVersion(), null));
        }
      }
      return result;
    }

    private boolean areLibraryBundlesDownloaded(LibraryDefinition libraryDef) {
      for (BundleDefinition bundleDef : libraryDef.getBundleDefs()) {
        if (!getBundlesProcessor().isComponentDownloaded(new BundleData(bundleDef))) {
          return false;
        }
      }
      return true;
    }

    @Override
    @Nullable
    protected VirtualFile getDestinationFolder() {
      return myLibraryTargetEditor.getTargetDir();
    }
  }

  @Nls
  private static String getSearchProgressText() {
    return DmServerBundle.message("DownloadBundlesEditor.label.progress.searching");
  }

  @Nls
  private static String getDownloadProgressText() {
    return DmServerBundle.message("DownloadBundlesEditor.label.progress.downloading");
  }

  @Nls
  private static String getToResultsProgressText() {
    return DmServerBundle.message("DownloadBundlesEditor.label.progress.adding");
  }
}

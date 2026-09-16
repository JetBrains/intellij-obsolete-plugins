package com.jetbrains.bigdatatools.dataproc.ui.component

import com.google.cloud.dataproc.v1.OrderedJob
import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.RawCommandLineEditor
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.dependend.drivers.DataprocDriversProvider
import com.jetbrains.bigdatatools.dataproc.submit.DataprocHadoopJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocHiveJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocPigJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocPrestoJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocPySparkJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocSparkJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocSparkRJobConfBlock
import com.jetbrains.bigdatatools.dataproc.submit.DataprocSparkSqlJobConfBlock
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileSelectorUtils.withDataprocArchiveValidator
import com.jetbrains.bigdatatools.dataproc.ui.component.DataprocFileSelectorUtils.withDataprocFsValidator
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.util.SparkMessagesBundle

object DataprocUiComponents {
  fun createAddJobBlocks(project: Project,
                         dataprocDataManager: DataprocDataManager,
                         driversProvider: DataprocDriversProvider,
                         rootDisposable: Disposable,
                         clusterNameProvider: () -> String?) = jobTypeCases.associateWith { jobTypeCase ->
    when (jobTypeCase) {
      OrderedJob.JobTypeCase.HADOOP_JOB -> DataprocHadoopJobConfBlock(project, dataprocDataManager, driversProvider, clusterNameProvider)
      OrderedJob.JobTypeCase.SPARK_JOB -> DataprocSparkJobConfBlock(project, dataprocDataManager, driversProvider, clusterNameProvider)
      OrderedJob.JobTypeCase.SPARK_R_JOB -> DataprocSparkRJobConfBlock(project, dataprocDataManager, driversProvider)
      OrderedJob.JobTypeCase.PYSPARK_JOB -> DataprocPySparkJobConfBlock(project, dataprocDataManager, driversProvider)
      OrderedJob.JobTypeCase.HIVE_JOB -> DataprocHiveJobConfBlock(project, dataprocDataManager, driversProvider)
      OrderedJob.JobTypeCase.SPARK_SQL_JOB -> DataprocSparkSqlJobConfBlock(project, dataprocDataManager, driversProvider)
      OrderedJob.JobTypeCase.PIG_JOB -> DataprocPigJobConfBlock(project, dataprocDataManager, driversProvider)
      OrderedJob.JobTypeCase.PRESTO_JOB -> DataprocPrestoJobConfBlock(project, dataprocDataManager, driversProvider)
      OrderedJob.JobTypeCase.JOBTYPE_NOT_SET -> null
    }?.also { jobConfBlock ->
      Disposer.register(rootDisposable, jobConfBlock)
    }
  }

  fun createClusterComboBox(manager: DataprocDataManager, @NlsSafe defaultValue: String?): ComboBox<String> {
    val clusterCombobox = ComboBox(getClustersFromCache(manager).toTypedArray())
    clusterCombobox.isSwingPopup = false
    defaultValue?.let {
      @Suppress("HardCodedStringLiteral")
      clusterCombobox.item = it
    }

    clusterCombobox.isSwingPopup = false
    @Suppress("HardCodedStringLiteral")
    clusterCombobox.prototypeDisplayValue = "Cluster sample name" // Field is set for limiting combobox width.
    clusterCombobox.renderer = CustomListCellRenderer<String> { it }

    return clusterCombobox
  }

  private fun getClustersFromCache(manager: DataprocDataManager) = manager.clusterModel.data
                                                                     ?.filter { !it.isStopped }
                                                                     ?.map { it.name } ?: emptyList()

  fun createMainClassField(project: Project,
                           disposable: Disposable,
                           dataprocDataManager: DataprocDataManager,
                           pathProvider: () -> FilePath?,
                           clusterNameProvider: () -> String?) =
    DataprocMainClassField(project, disposable, dataprocDataManager, pathProvider, clusterNameProvider)

  fun createJarsField(project: Project, disposable: Disposable, driversProvider: DataprocDriversProvider) =
    DataprocFileMultiSelector(DataprocMessagesBundle.message("job.info.spark.jars.title"),
                              DataprocMessagesBundle.message("job.info.spark.jars"),
                              project,
                              driversProvider)
      .withDataprocFsValidator(disposable)
      .also {
        it.toolTipText = DataprocMessagesBundle.message("job.info.spark.jars.hint")
      }

  fun createArchivesField(project: Project, disposable: Disposable, driversProvider: DataprocDriversProvider) =
    DataprocFileMultiSelector(DataprocMessagesBundle.message("job.info.spark.archives.title"),
                              DataprocMessagesBundle.message("job.info.spark.archives"),
                              project,
                              driversProvider)
      .withDataprocArchiveValidator(disposable)
      .withDataprocFsValidator(disposable)
      .also {
        it.toolTipText = DataprocMessagesBundle.message("job.info.spark.archives.hint")
      }

  fun createArgumentsField() = RawCommandLineEditor().also {
    it.textField.toolTipText = SparkMessagesBundle.message("settings.application.hint")
  }


  val OrderedJob.JobTypeCase.title
    get() = when (this) {
      OrderedJob.JobTypeCase.HADOOP_JOB -> DataprocMessagesBundle.message("job.hadoop.title")
      OrderedJob.JobTypeCase.SPARK_JOB -> DataprocMessagesBundle.message("job.spark.title")
      OrderedJob.JobTypeCase.SPARK_R_JOB -> DataprocMessagesBundle.message("job.spark.r.title")
      OrderedJob.JobTypeCase.PYSPARK_JOB -> DataprocMessagesBundle.message("job.pyspark.title")
      OrderedJob.JobTypeCase.HIVE_JOB -> DataprocMessagesBundle.message("job.hive.title")
      OrderedJob.JobTypeCase.SPARK_SQL_JOB -> DataprocMessagesBundle.message("job.spark.sql.title")
      OrderedJob.JobTypeCase.PIG_JOB -> DataprocMessagesBundle.message("job.pig.title")
      OrderedJob.JobTypeCase.PRESTO_JOB -> DataprocMessagesBundle.message("job.presto.title")
      OrderedJob.JobTypeCase.JOBTYPE_NOT_SET -> ""
    }


  val jobTypeCases = listOf(
    OrderedJob.JobTypeCase.SPARK_JOB,
    OrderedJob.JobTypeCase.PYSPARK_JOB,
    OrderedJob.JobTypeCase.SPARK_SQL_JOB,
    OrderedJob.JobTypeCase.SPARK_R_JOB,
    OrderedJob.JobTypeCase.HADOOP_JOB,
    OrderedJob.JobTypeCase.HIVE_JOB,
    OrderedJob.JobTypeCase.PIG_JOB,
    OrderedJob.JobTypeCase.PRESTO_JOB).toTypedArray()
}
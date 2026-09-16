package com.jetbrains.spark.submit.run.ssh.upload

import com.intellij.execution.BeforeRunTask

class UploadBeforeRunTask : BeforeRunTask<UploadBeforeRunTask>(UploadBeforeRunTaskProvider.ID)
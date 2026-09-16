// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

rootProject.name = "Big Data Tools Obsolete Plugins"

include("binaryFilesSupport")

include("metastoreCore")
project(":metastoreCore").projectDir = file("plugins/metastore-core")

include("hiveMetastore")
project(":hiveMetastore").projectDir = file("hive-metastore")

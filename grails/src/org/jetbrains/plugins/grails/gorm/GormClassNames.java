// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.gorm;

public interface GormClassNames {
  String OLD_ENTITY_ANNO = "grails.persistence.Entity";
  String ENTITY_ANNO = "grails.gorm.annotation.Entity";
  String ENTITY_TRAIT = "org.grails.datastore.gorm.GormEntity";
  String QUERY_CREATOR = "org.grails.datastore.mapping.query.QueryCreator";
}

package com.intellij.lang.puppet.psi;


import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// https://docs.puppet.com/puppet/4.7/reference/lang_data_type.html
public interface PuppetDataTypes {
  @NonNls String RESOURCE_TYPE_NAME = "Resource";
  @NonNls String CLASS_TYPE_NAME = "Class";
  @NonNls String TYPE_TYPE_NAME = "Type";
  @NonNls String VARIANT_TYPE_NAME = "Variant";

  List<String> CORE_DATA_TYPES = List.of("String",
                                         "Integer",
                                         "Float",
                                         "Numeric",
                                         "Boolean",
                                         "Array",
                                         "Hash",
                                         "Regexp",
                                         "Undef",
                                         "Default",

                                         // uncategorized for now
                                         "SemVer",
                                         "SemVerRange",
                                         "Timespan",
                                         "Timestamp",
                                         "NotUndef",
                                         "Iterator",
                                         "Iterable",
                                         "Runtime",
                                         "Sensitive");

  List<String> RESOURCE_AND_CLASS_DATATYPES = List.of(RESOURCE_TYPE_NAME,
                                                      CLASS_TYPE_NAME);

  List<String> ABSTRACT_DATA_TYPES = List.of("Scalar",
                                             "Collection",
                                             VARIANT_TYPE_NAME,
                                             "Data",
                                             "Pattern",
                                             "Enum",
                                             "Tuple",
                                             "Struct",
                                             "Optional",
                                             "CatalogEntry",
                                             "Any",
                                             "Callable",
                                             TYPE_TYPE_NAME);

  List<String> ALL_DATA_TYPES = ContainerUtil.concat(CORE_DATA_TYPES, RESOURCE_AND_CLASS_DATATYPES, ABSTRACT_DATA_TYPES);
  Set<String> ALL_LOWERCASED_DATA_TYPES = new HashSet<>(ContainerUtil.map(ALL_DATA_TYPES, StringUtil::toLowerCase));
}

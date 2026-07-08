package com.intellij.lang.puppet.autocompletion;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.lang.puppet.psi.PuppetDataTypes;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public abstract class PuppetCompletionTestCase extends PuppetTestCase {
  protected static final String[] LOWERCASED_PUPPET_DEFINED_LIB_RESOURCE_TYPES = {
    "testlib::resource"
  };

  protected static final String[] CAPITALIZED_PUPPET_DEFINED_LIB_RESOURCE_TYPES =
    Arrays.stream(LOWERCASED_PUPPET_DEFINED_LIB_RESOURCE_TYPES)
      .map(PuppetQualifiedNamesUtil::capitalizePuppetName)
      .toArray(String[]::new);

  protected static final String[] ALL_PUPPET_DEFINED_LIB_RESOURCE_TYPES =
    mergeArrays(LOWERCASED_PUPPET_DEFINED_LIB_RESOURCE_TYPES, CAPITALIZED_PUPPET_DEFINED_LIB_RESOURCE_TYPES);

  protected static final String[] LOWERCASE_LIB_CLASSES = {
    "testlib", "testlib::resource::subclass"
  };

  protected static final String[] CAPITALIZED_LIB_CLASSES =
    Arrays.stream(LOWERCASE_LIB_CLASSES)
      .map(PuppetQualifiedNamesUtil::capitalizePuppetName)
      .toArray(String[]::new);


  protected static final String[] ALL_LIB_CLASSES = mergeArrays(LOWERCASE_LIB_CLASSES, CAPITALIZED_LIB_CLASSES);

  protected static final String[] LOWERCASED_BUILT_IN_RESOURCE_TYPES =
    {
      "augeas", "computer", "cron", "exec", "file", "filebucket", "group", "host", "interface", "k5login", "macauthorization", "mailalias",
      "maillist", "mcx", "mount", "my_file_line", "nagios_command", "nagios_contact", "nagios_contactgroup", "nagios_host",
      "nagios_hostdependency", "nagios_hostescalation", "nagios_hostextinfo", "nagios_hostgroup", "nagios_service",
      "nagios_servicedependency", "nagios_serviceescalation", "nagios_serviceextinfo", "nagios_servicegroup", "nagios_timeperiod", "notify",
      "package", "resources", "router", "schedule", "scheduled_task", "selboolean", "selmodule", "service", "ssh_authorized_key", "sshkey",
      "stage", "tidy", "user", "vlan", "yumrepo", "zfs", "zone", "zpool"
    };

  protected static final String[] CAPITALIZED_BUILT_IN_RESOURCE_TYPES =
    Arrays.stream(LOWERCASED_BUILT_IN_RESOURCE_TYPES)
      .map(PuppetQualifiedNamesUtil::capitalizePuppetName)
      .toArray(String[]::new);

  protected static final String[] ALL_BUILT_IN_RESOURCE_TYPES =
    mergeArrays(LOWERCASED_BUILT_IN_RESOURCE_TYPES, CAPITALIZED_BUILT_IN_RESOURCE_TYPES);

  protected static final String[] ALL_BUILT_IN_AND_LIB_RESOURCE_TYPES =
    mergeArrays(ALL_PUPPET_DEFINED_LIB_RESOURCE_TYPES, ALL_BUILT_IN_RESOURCE_TYPES);

  protected static final String[] ALL_LOWERCASED_RESOURCE_TYPES =
    mergeArrays(LOWERCASED_PUPPET_DEFINED_LIB_RESOURCE_TYPES, LOWERCASED_BUILT_IN_RESOURCE_TYPES);

  protected static final String[] ALL_CAPITALIZED_RESOURCE_TYPES =
    mergeArrays(CAPITALIZED_BUILT_IN_RESOURCE_TYPES, CAPITALIZED_PUPPET_DEFINED_LIB_RESOURCE_TYPES);

  protected static final String[] ALL_DATA_TYPES = ArrayUtilRt.toStringArray(PuppetDataTypes.ALL_DATA_TYPES);

  protected static final String[] LIVE_TEMPLATE_LOOKUP_ELEMENTS = new String[]{
    "case...",
    "class...",
    "define...",
    "function...",
    "if...",
    "node...",
    "unless..."
  };

  protected static final String[] LIVE_TEMPLATES_WITH_CAPITALIZED_RESOURCE_TYPES =
    ArrayUtil.mergeArrays(ALL_CAPITALIZED_RESOURCE_TYPES, LIVE_TEMPLATE_LOOKUP_ELEMENTS);

  protected static final String[] METAPARAMETERS =
    {"alias", "audit", "noop", "notify", "require", "before", "loglevel", "schedule", "stage", "subscribe", "tag", "name", "consume",
      "export", "title"};

  protected static final String[] GLOBAL_FACTS = {
    "aio_agent_version", "augeas", "cloud", "disks", "dmi", "ec2_metadata", "ec2_userdata", "env_windows_installdir", "facterversion",
    "filesystems",
    "gce", "identity", "is_virtual", "kernel", "kernelmajversion", "kernelrelease", "kernelversion", "ldom", "load_averages", "memory",
    "mountpoints", "networking", "os", "partitions", "path", "processors", "ruby", "solaris_zones", "ssh", "system_profiler",
    "system_uptime", "timezone", "virtual", "xen", "zfs_featurenumbers", "zfs_version", "zpool_featurenumbers", "zpool_version",
    "architecture", "augeasversion", "blockdevices", "bios_release_date", "bios_vendor", "bios_version", "boardassettag",
    "boardmanufacturer", "boardproductname", "boardserialnumber", "chassisassettag", "chassistype", "dhcp_servers", "domain", "fqdn", "gid",
    "hardwareisa", "hardwaremodel", "hostname", "id", "interfaces", "ipaddress", "ipaddress6", "lsbdistcodename", "lsbdistdescription",
    "lsbdistid", "lsbdistrelease", "lsbmajdistrelease", "lsbminordistrelease", "lsbrelease", "macaddress", "macosx_buildversion",
    "macosx_productname", "macosx_productversion", "macosx_productversion_major", "macosx_productversion_minor", "manufacturer",
    "memoryfree", "memoryfree_mb", "memorysize", "memorysize_mb", "netmask", "netmask6", "network", "network6", "operatingsystem",
    "operatingsystemmajrelease", "operatingsystemrelease", "osfamily", "physicalprocessorcount", "processorcount", "productname",
    "rubyplatform", "rubysitedir", "rubyversion", "selinux", "selinux_config_mode", "selinux_config_policy", "selinux_current_mode",
    "selinux_enforced", "selinux_policyversion", "serialnumber", "swapencrypted", "swapfree", "swapfree_mb", "swapsize", "swapsize_mb",
    "system32", "uptime", "uptime_days", "uptime_hours", "uptime_seconds", "uuid", "xendomains", "zonename", "zones",

    // built in variables
    "agent_specified_environment", "caller_module_name", "clientcert", "clientnoop", "clientversion", "environment", "facts",
    "module_name", "server_facts", "serverip", "servername", "serverversion", "trusted",

    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
  };

  protected static final String[] STUB_FUNCTIONS = {
    "alert", "contain", "crit", "debug", "defined", "digest", "each", "emerg", "epp", "err", "fail", "file", "filter", "generate", "hiera",
    "include", "info", "lookup", "map", "match", "md5", "notice", "realize", "reduce", "regsubst", "require", "scanf", "sha1", "shellquote",
    "slice", "split", "sprintf", "tag", "tagged", "template", "versioncmp", "warning", "with", "my_abs", "assert_type", "create_resources",
    "dig", "fqdn_rand", "hiera_array", "hiera_hash", "hiera_include", "inline_epp", "inline_template", "lest", "new", "reverse_each",
    "step", "type",
    // 4.10 update
    "binary_file", "break", "find_file", "next", "return", "strftime", "then"
  };

  protected static final String[] LIB_FACTS = {
    "my_pe_version", "my_is_pe", "my_pe_major_version", "my_super_fact", "my_super_fact2", "myfact1", "myfact2", "myfact3", "myfact4",
    "myfact5", "myfact6"
  };

  protected static final String[] LIB_VARS_WITHOUT_NAMES = {
    "testlib::intVar1",
    "testlib::intVar2",
    "testlib::resource::subclass::intVar1",
    "testlib::resource::subclass::intVar2"
  };


  protected static final String[] LIB_VARS = ArrayUtil.mergeArrays(LIB_VARS_WITHOUT_NAMES,
                                                                   "testlib::name",
                                                                   "testlib::title",
                                                                   "testlib::resource::subclass::name",
                                                                   "testlib::resource::subclass::title"
  );


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    setUpLibraries();
  }


  protected void doTestWithMetaparams(String... expectedLookupItems) {
    doTest(mergeArrays(METAPARAMETERS, expectedLookupItems));
  }

  protected void doTestWithTopAndFunctions(String... expectedLookupItems) {
    doTest(mergeArrays(mergeArrays(ALL_LOWERCASED_RESOURCE_TYPES, STUB_FUNCTIONS), expectedLookupItems));
  }

  protected void doTestWithFacts(String... expectedLookupItems) {
    doTest(ArrayUtil
             .mergeArrays(mergeArrays(mergeArrays(GLOBAL_FACTS, LIB_FACTS), LIB_VARS), expectedLookupItems));
  }

  protected void doTest(String... expectedLookupItems) {
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
    boolean oldValue = codeInsightSettings.AUTOCOMPLETE_ON_CODE_COMPLETION;
    codeInsightSettings.AUTOCOMPLETE_ON_CODE_COMPLETION = false;
    try {
      doTestCompletionVariants(getTestFileName(), expectedLookupItems);
    }
    finally {
      codeInsightSettings.AUTOCOMPLETE_ON_CODE_COMPLETION = oldValue;
    }
  }

  protected String getTestFileName() {
    String testName = getTestName(true);
    return testName + ".code";
  }

  protected void doTestCompletionVariants(@NotNull String testFileName, String... expectedItems) {
    configureByManifest(testFileName);
    final LookupElement[] items = myFixture.complete(CompletionType.BASIC);
    assertNotNull("No lookup was shown, probably there was only one lookup element that was inserted automatically", items);
    final List<String> result = myFixture.getLookupElementStrings();
    assertNotNull(result);
    UsefulTestCase.assertSameElements(result, expectedItems);
  }

  protected static <T> T[] mergeArrays(T[]... sources) {
    T[] result = sources[0];
    for (int i = 1; i < sources.length; i++) {
      result = ArrayUtil.mergeArrays(result, sources[i]);
    }
    return result;
  }
}

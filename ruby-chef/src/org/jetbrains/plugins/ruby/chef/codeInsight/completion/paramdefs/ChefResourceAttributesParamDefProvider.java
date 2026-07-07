package org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs;

import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionConvertable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefProviderBase;

import static org.jetbrains.plugins.ruby.chef.ChefUtil.CHEF_RESOURCE;
import static org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs.ChefParamDefUtil.actionRef;
import static org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs.ChefParamDefUtil.nilRef;
import static org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs.ChefParamDefUtil.numberRef;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionUtil.bool;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionUtil.either;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionUtil.hash;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionUtil.oneOfStringsOrSymbols;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionUtil.s;

public final class ChefResourceAttributesParamDefProvider extends ParamDefProviderBase {

  private static final String FILE_RESOURCE = CHEF_RESOURCE + "::File";
  private static final String SERVICE_RESOURCE = CHEF_RESOURCE + "::Service";
  private static final String LINK_RESOURCE = CHEF_RESOURCE + "::Link";
  private static final String CRON_RESOURCE = CHEF_RESOURCE + "::Cron";
  private static final String DEPLOY_RESOURCE = CHEF_RESOURCE + "::Deploy";
  private static final String LOG_RESOURCE = CHEF_RESOURCE + "::Log";
  private static final String MOUNT_RESOURCE = CHEF_RESOURCE + "::Mount";
  private static final String PACKAGE_RESOURCE = CHEF_RESOURCE + "::Package";
  private static final String REGISTRY_KEY_RESOURCE = CHEF_RESOURCE + "::RegistryKey";
  private static final String YUM_PACKAGE_RESOURCE = CHEF_RESOURCE + "::YumPackage";

  @Override
  protected void registerParamDefs() {
    paramDef(CHEF_RESOURCE, "action", either(actionRef(), s("nothing")));

    paramDef(CHEF_RESOURCE, "notifies", oneOfStringsOrSymbols(s("run"), s("restart"), s("reload"), s("create")), oneOfStringsOrSymbols(),
             oneOfStringsOrSymbols(s("immediately"), s("delayed")));
    defineParamsCopy(CHEF_RESOURCE + ".subscribes", CHEF_RESOURCE + ".notifies");

    paramDef(CHEF_RESOURCE, "not_if", oneOfStringsOrSymbols(),
             oneOfStringsOrSymbols(s("user"), s("group"), s("environment"), s("cwd"), s("timeout")));
    defineParamsCopy(CHEF_RESOURCE + ".only_if", CHEF_RESOURCE + ".not_if");

    paramDef(CHEF_RESOURCE, "guard_interpreter",
             oneOfStringsOrSymbols(s("bash"), s("batch"), s("csh"), s("default"), s("perl"), s("powershell_script"), s("python"),
                                   s("ruby")));

    paramDef(PACKAGE_RESOURCE, "options", hash().add(s("env_shebang"), bool()).add(s("force"), bool()).add(s("format_executable"), bool())
      .add(s("ignore_dependencies"), bool()).add(s("prerelease"), bool()).add(s("security_policy"), bool()).add(s("wrappers"), bool()));

    paramDef(YUM_PACKAGE_RESOURCE, "flush_cache", hash().add(s("before"), bool()).add("after", bool()));
    paramDef(REGISTRY_KEY_RESOURCE, "architecture", oneOfStringsOrSymbols(s("i386"), s("x86_64 "), s("machine")));
    paramDef(FILE_RESOURCE, "manage_symlink_source", nilRef());

    paramDef(SERVICE_RESOURCE, "init_command", nilRef());
    paramDef(SERVICE_RESOURCE, "supports", hash().add(s("restart"), bool()).add("reload", bool()).add("status", bool()));

    paramDef(LINK_RESOURCE, "link_type", oneOfStringsOrSymbols(s("symbolic "), s("hard")));
    paramDef(LOG_RESOURCE, "level", oneOfStringsOrSymbols(s("debug "), s("info"), s("warn"), s("error"), s("fatal")));
    paramDef(MOUNT_RESOURCE, "device_type", oneOfStringsOrSymbols(s("device "), s("label"), s("uuid")));
    paramDef(MOUNT_RESOURCE, "supports",
             hash().add(s("mount"), bool()).add(s("umount"), bool()).add(s("remount"), bool()).add(s("enable"), bool())
               .add(s("disable"), bool()));
    paramDef(DEPLOY_RESOURCE, "symlinks", hash().add(s("system"), null).add(s("pids"), null).add(s("log"), null));

    paramDef(CRON_RESOURCE, "home", hash().add(s("HOME"), oneOfStringsOrSymbols()));

    paramDef(CRON_RESOURCE, "day", getCronAttributesValues(1, 31));

    paramDef(CRON_RESOURCE, "hour", getCronAttributesValues(0, 23));
    paramDef(CRON_RESOURCE, "minute", getCronAttributesValues(0, 59));
    paramDef(CRON_RESOURCE, "month", getCronAttributesValues(1, 12));
    paramDef(CRON_RESOURCE, "weekday", either(oneOfStringsOrSymbols(s("sunday"), s("monday"), s("tuesday"), s("wednesday"), s("thursday"), s("friday"),
                                              s("saturday")), getCronAttributesValues(0, 6)));
  }

  private static ParamDefExpressionConvertable getCronAttributesValues(int from, int to) {
    ParamDefExpressionConvertable[] numberParamDefs = new NumberParamDef[to - from + 2];
    numberParamDefs[0] = new NumberParamDef("*", to + 1);
    for (int i = from; i <= to; i++) {
      numberParamDefs[i - from + 1] = numberRef(Integer.toString(i), to - i + 1);
    }
    return either(numberParamDefs);
  }
}
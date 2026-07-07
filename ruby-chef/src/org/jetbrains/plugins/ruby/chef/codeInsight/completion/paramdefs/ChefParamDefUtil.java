package org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs;

import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionConvertable;

public final class ChefParamDefUtil {
  public static ParamDefExpressionConvertable actionRef() {
    return new ActionParamDef();
  }

  public static ParamDefExpressionConvertable nilRef() {
    return new NilParamDef();
  }

  public static ParamDefExpressionConvertable numberRef(String text, int priority) {
    return new NumberParamDef(text, priority);
  }
}

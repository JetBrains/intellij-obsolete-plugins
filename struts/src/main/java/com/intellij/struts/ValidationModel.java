/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.struts.dom.validator.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ValidationModel extends NamedDomModel<FormValidation> {

  /**
   * Returns all Validators (exluding those without underlying class) as modifiable list.
   *
   * @return Validator names.
   */
  @NotNull
  List<Validator> getValidators();

  /**
   * Gets the XmlTag for the Validator with the given name.
   *
   * @param name Name of the Validator.
   * @return null if Validator not found.
   */
  @Nullable
  Validator findValidator(String name);

  /**
   * Returns the associated StrutsModel.
   *
   * @return StrutsModel.
   */
  @NotNull
  StrutsModel getStrutsModel();

}
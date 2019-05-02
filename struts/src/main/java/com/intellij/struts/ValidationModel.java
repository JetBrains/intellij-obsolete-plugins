/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
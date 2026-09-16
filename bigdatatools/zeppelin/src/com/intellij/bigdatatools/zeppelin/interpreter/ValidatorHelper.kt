package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JTextField

fun JTextField.withPortValidator(parentDisposable: Disposable) {
  ComponentValidator(parentDisposable)
    .withValidator(Supplier {

      if (!this.isEnabled || !this.isVisible) {
        return@Supplier null
      }

      if (text.isBlank()) null
      else {
        val port = text.toIntOrNull()
        if (port == null || (port < 0 || port > 65535)) {
          ValidationInfo(ZepMessagesBundle.message("interpreter.settings.validation.port"), this)
        }
        else {
          null
        }
      }
    })
    .andRegisterOnDocumentListener(this)
    .installOn(this)
}

fun JTextField.withNotEmptyValidator(parentDisposable: Disposable, localizedMessageKey: String) {
  ComponentValidator(parentDisposable)
    .withValidator(Supplier {
      if (text.isBlank()) {
        ValidationInfo(ZepMessagesBundle.message(localizedMessageKey), this)
      }
      else {
        null
      }
    })
    .andRegisterOnDocumentListener(this)
    .installOn(this)
}

fun List<JComponent>.getInvalidComponent() = find {
  val optional = ComponentValidator.getInstance(it)
  val validator = if (optional.isPresent) optional.get() else null

  if (validator != null) {
    validator.revalidate()
    if (validator.validationInfo != null) {
      it.requestFocus()
      true
    }
    else {
      false
    }
  }
  else {
    false
  }
}

//fun JComponent.withoutValidation(runnable: () -> Unit) {
//  val optional = ComponentValidator.getInstance(this)
//  val validator = if (optional.isPresent) optional.get() else null
//  if (validator != null) {
//    this.putClientProperty("JComponent.componentValidator", null)
//  }
//
//  try {
//    runnable()
//  }
//  finally {
//    if (validator != null) {
//      this.putClientProperty("JComponent.componentValidator", validator)
//    }
//  }
//}

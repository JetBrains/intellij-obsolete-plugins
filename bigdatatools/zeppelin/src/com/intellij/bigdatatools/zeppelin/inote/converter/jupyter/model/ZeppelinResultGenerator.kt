package com.intellij.bigdatatools.zeppelin.inote.converter.jupyter.model

internal object ZeppelinResultGenerator {
  fun toBase64ImageHtmlElement(image: String) =
    "<div style='width:auto;height:auto'><img src=data:image/png;base64,$image style='width=auto;height:auto'/></div>"

  fun toLatex(latexCode: String) = "<div><div>$latexCode</div></div>"

  fun toJavascript(javascriptCode: String) = "<script type='application/javascript'>$javascriptCode</script>"
}
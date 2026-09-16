package com.intellij.bigdatatools.zeppelin.api.remote

import com.intellij.util.net.ssl.CertificateUtil
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ZeppelinConnectionUtil {
  val x509TrustAllManager = object : X509TrustManager {
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
  }

  fun getTrustAllSocketFactory(): SSLSocketFactory {
    // Create a trust manager that does not validate certificate chains
    val trustAllCerts: Array<TrustManager> = arrayOf(
      x509TrustAllManager
    )
    val sc: SSLContext = SSLContext.getInstance(CertificateUtil.TLS)
    sc.init(null, trustAllCerts, SecureRandom())
    return sc.socketFactory
  }
}
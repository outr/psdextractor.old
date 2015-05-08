package com.outr.outrutility

import com.outr.outrutility.psd.PSDPreviewPage
import org.hyperscala.realtime.Realtime
import org.hyperscala.web._
import com.outr.net.http.jetty.JettyApplication

object OUTRUtilitySite extends BasicWebsite with JettyApplication {
  Realtime

  val preview = page(new PSDPreviewPage, Scope.Page, "/preview.html")
}

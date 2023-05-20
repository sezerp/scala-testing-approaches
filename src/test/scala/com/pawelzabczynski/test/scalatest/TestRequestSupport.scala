package com.pawelzabczynski.test.scalatest

import sttp.client3.SttpBackend
import zio.Task

trait TestRequestSupport {

  protected def basePath: String
  def backend: SttpBackend[Task, Any]

}

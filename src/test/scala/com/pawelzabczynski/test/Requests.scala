package com.pawelzabczynski.test

import sttp.client3.SttpBackend
import zio.Task

class Requests(override val backend: SttpBackend[Task, Any])
    extends TestRequestSupport
    with TestSupport
    with UserRequests {
  override protected val basePath: String = s"http://localhost:${TestConfig.api.port}/api/v1"
}

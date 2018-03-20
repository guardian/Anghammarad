package com.gu.anghammarad

import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import scala.util.Try


class Lambda extends RequestHandler[SNSEvent, Unit] {
  override def handleRequest(input: SNSEvent, context: Context): Unit = {
    // parse raw notification
    val rawNotification: RawNotification = ???
    val result = Anghammarad.run(rawNotification)
    // log error
  }
}

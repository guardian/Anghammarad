package com.gu.anghammarad

import com.gu.anghammarad.models.RawNotification


class DevMain {
  def main(args: Array[String]): Unit = {
    // parse raw notification
    val rawNotification: RawNotification = ???
    val result = Anghammarad.run(rawNotification)
    // log error
  }
}

package com.gu.anghammarad.models

import com.vladsch.flexmark.ast.Node


sealed trait Target
case class Stack(stack: String) extends Target
case class Stage(stage: String) extends Target
case class App(app: String) extends Target
case class AwsAccount(awsAccount: String) extends Target


sealed trait Contact
case class Email(address: String) extends Contact
case class HangoutsChat(webhook: String) extends Contact


case class Mapping(
  mappings: List[(Target, Contact)]
)

case class Message(
  subject: String,
  contents: String
)


case class Notification(
  sourceSystem: String,
  target: List[Target],
  subject: String,
  message: Node
)


case class RawNotification(
  sourceSystem: String,
  target: String,
  subject: String,
  message: String
)
case class RawTarget(
  key: String,
  value: String
)
case class RawContact(
  channel: String,
  identifier: String
)
case class RawMapping(
  target: List[RawTarget],
  contacts: List[RawContact]
)
case class RawConfig(
  mappings: List[(RawTarget, RawContact)]
)

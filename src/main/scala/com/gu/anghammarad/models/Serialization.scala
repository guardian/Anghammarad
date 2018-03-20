package com.gu.anghammarad.models

import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

import scala.util.{Failure, Success, Try}
import com.gu.anghammarad.Enrichments._


object Serialization {
  implicit val rawNotificationDecoder: Decoder[RawNotification] = deriveDecoder[RawNotification]
  implicit val rawTargetDecoder: Decoder[RawTarget] = deriveDecoder[RawTarget]
  implicit val rawContactDecoder: Decoder[RawContact] = deriveDecoder[RawContact]
  implicit val rawMappingDecoder: Decoder[RawConfig] = deriveDecoder[RawConfig]

  def parseRawNotification(string: String): Try[RawNotification] = {
    parse(string).flatMap(_.as[RawNotification]).fold(
      err => Failure(err),
      rn => Success(rn)
    )
  }

  def parseMapping(string: String): Try[RawConfig] = {
    parse(string).flatMap(_.as[RawConfig]).fold(
      err => Failure(err),
      rn => Success(rn)
    )
  }

  def parseRawMapping(rawMapping: RawConfig): Try[Mapping] = {
    rawMapping.mappings.traverse { case (rawTarget, rawContact) =>
      for {
        target <- parseRawTarget(rawTarget)
        contact <- parseRawContact(rawContact)
      } yield (target, contact)
    }.map(Mapping)
  }

  def parseRawTarget(rawTarget: RawTarget): Try[Target] = {
    rawTarget.key match {
      case "Stack" => Success(Stack(rawTarget.value))
      case "App" => Success(App(rawTarget.value))
      case "Stage" => Success(Stage(rawTarget.value))
      case "AwsAccount" => Success(AwsAccount(rawTarget.value))
      case _ => Failure(throw new RuntimeException(s"Invalid target ${rawTarget.key}"))
    }
  }

  def parseRawContact(rawContact: RawContact): Try[Contact] = {
    rawContact.channel match {
      case "email" => Success(Email(rawContact.identifier))
      case "hangouts" => Success(HangoutsChat(rawContact.identifier))
      case _ => Failure(throw new RuntimeException(s"Invalid channel ${rawContact.channel}"))
    }
  }
}

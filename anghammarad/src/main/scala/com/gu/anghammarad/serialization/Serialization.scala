package com.gu.anghammarad.serialization

import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.gu.anghammarad.AnghammaradException.Fail
import com.gu.anghammarad.Enrichments._
import com.gu.anghammarad.models._
import io.circe.Decoder.Result
import io.circe._
import io.circe.parser._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object Serialization {
  def parseConfig(config: String): Try[Configuration] = {
    for {
      emailDomain <- parseEmailDomain(config)
      emailSender <- parseEmailSender(config)
      mappings <- parseAllMappings(config)
    } yield Configuration(emailDomain, emailSender, mappings)
  }

  private def parseEmailDomain(jsonStr: String): Try[String] = {
    val emailDomain = for {
      json <- parse(jsonStr)
      sender <- json.hcursor.downField("emailDomain").as[String]
    } yield sender

    emailDomain.fold(
      err => Failure(err),
      mappings => Success(mappings)
    )
  }

  private def parseEmailSender(jsonStr: String): Try[String] = {
    val emailSender = for {
      json <- parse(jsonStr)
      sender <- json.hcursor.downField("emailSender").as[String]
    } yield sender

    emailSender.fold(
      err => Failure(err),
      mappings => Success(mappings)
    )
  }

  def parseNotification(snsEvent: SNSEvent): Try[Notification] = {
    val maybeSns = snsEvent.getRecords.asScala.toList match {
      case singleRecord :: Nil => Success(singleRecord.getSNS)
      case _ => Fail(s"Found multiple SNSRecords")
    }

    for {
      sns <- maybeSns
      subject = sns.getSubject
      message = sns.getMessage
      jsonMsg <- parse(message).toTry
      notification <- generateNotification(subject, jsonMsg)
    } yield notification
  }

  def generateNotification(subject: String, content: Json): Try[Notification] = {
    val hCursor = content.hcursor
    val parsingResult = for {
      sourceSystem <- hCursor.downField("sender").as[String]
      rawTargets <- hCursor.downField("target").as[Json]
      rawChannel <- hCursor.downField("channel").as[String]
      rawActions <- hCursor.downField("actions").as[List[Json]]
      message <- hCursor.downField("message").as[String]
      channel <- parseChannel(rawChannel).toEither
      targets = parseTargets(rawTargets)
      actions <- rawActions.traverseT(parseAction).toEither
    } yield Notification(sourceSystem, channel, targets, subject, message, actions)

    parsingResult.fold(
      err => Failure(err),
      notification => Success(notification)
    )
  }

  private[serialization] def parseChannel(channel: String): Try[RequestedChannel] = {
    channel match {
      case "email" => Success(Email)
      case "hangouts" => Success(HangoutsChat)
      case "all" => Success(All)
      case _ => Fail(s"Cannot parse RequestedChannel")
    }
  }

  private[serialization] def parseAction(json: Json): Try[Action] = {
    val hCursor = json.hcursor
    val parsingResult = for {
      cta <- hCursor.downField("cta").as[String]
      url <- hCursor.downField("url").as[String]
    } yield Action(cta, url)

    parsingResult.fold(
      err => Failure(err),
      action => Success(action)
    )
  }

  def parseAllMappings(jsonStr: String): Try[List[Mapping]] = {
    val allMappings = for {
      json <- parse(jsonStr)
      rawMappings <- json.hcursor.downField("mappings").as[List[Json]]
      mappings <- rawMappings.traverseE(parseMapping)
    } yield mappings

    allMappings.fold(
      err => Failure(err),
      mappings => Success(mappings)
    )
  }

  private[serialization] def parseMapping(json: Json): Result[Mapping] = {
    val hCursor = json.hcursor
    for {
      rawTargets <- hCursor.downField("target").as[Json]
      rawContacts <- hCursor.downField("contacts").as[Json]
      targets = parseTargets(rawTargets)
      contacts = parseContacts(rawContacts)
    } yield Mapping(targets, contacts)
  }

  private[serialization] def parseTargets(jsonTargets: Json): List[Target] = {
    def parseTarget(key: String, value: String): Option[Target] = {
      key match {
        case "Stack" => Some(Stack(value))
        case "Stage" => Some(Stage(value))
        case "App" => Some(App(value))
        case "AwsAccount" => Some(AwsAccount(value))
        case _ => None
      }
    }

    val c: HCursor = jsonTargets.hcursor
    // TODO: do we want to return an empty list, or throw a error here?
    val keys: List[String] = c.keys.map(k => k.toList).getOrElse(List.empty)
    keys.flatMap { key =>
      val address = c.downField(key).as[String].getOrElse("")
      parseTarget(key, address)
    }
  }

  private[serialization] def parseContacts(jsonContacts: Json): List[Contact] = {
    def parseContact(key: String, value: String): Option[Contact] = {
      key match {
        case "email" => Some(EmailAddress(value))
        case "hangouts" => Some(HangoutsRoom(value))
        case _ => None
      }
    }

    val c: HCursor = jsonContacts.hcursor
    // TODO: do we want to return an empty list, or throw a error here?
    val keys: List[String] = c.keys.map(k => k.toList).getOrElse(List.empty)

    keys.flatMap { key =>
      val address = c.downField(key).as[String].getOrElse("")
      parseContact(key, address)
    }
  }
}
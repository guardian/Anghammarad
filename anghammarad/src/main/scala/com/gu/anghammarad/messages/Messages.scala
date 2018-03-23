package com.gu.anghammarad.messages

import com.gu.anghammarad.models._
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import io.circe.Json

import scala.collection.JavaConverters._


object Messages {
  private[anghammarad] val mdOptions = new MutableDataSet()
    .set(Parser.EXTENSIONS, List(TablesExtension.create(), StrikethroughExtension.create()).asJava)
  private[anghammarad] val mdParser = Parser.builder(mdOptions).build
  private[anghammarad] val mdRenderer = HtmlRenderer.builder(mdOptions).build()

  def channelMessages(notification: Notification): List[(Channel, Message)] = {
    notification.channel match {
      case Email =>
        List(
          Email -> emailMessage(notification)
        )
      case HangoutsChat =>
        List(
          HangoutsChat -> hangoutMessage(notification)
        )
      case All =>
        List(
          Email -> emailMessage(notification),
          HangoutsChat -> hangoutMessage(notification)
        )
    }
  }

  def emailMessage(notification: Notification): EmailMessage = {
    val (markdown, plaintext) =
      if (notification.actions.isEmpty) {
        (notification.message, notification.message)
      } else {
        val actionPrefix =
          s"""
             |_____________________
             |
             |""".stripMargin

        val htmlActions = notification.actions.map { action =>
          s"[${action.cta}](${action.url})"
        }.mkString("\n\n")
        val plainTextActions = notification.actions.map { action =>
          s"${action.cta} - ${action.url}"
        }.mkString("\n\n")

        (notification.message + actionPrefix + htmlActions, notification.message + actionPrefix + plainTextActions)
      }

    val md = mdParser.parse(markdown)
    val html = mdRenderer.render(md)

    EmailMessage(
      notification.subject,
      plaintext,
      html
    )
  }

  def hangoutMessage(notification: Notification): HangoutMessage = {
    val md = mdParser.parse(notification.message)
    val html = mdRenderer.render(md)
      // hangouts chat supports a subset of tags that differs from the flexmark-generated HTML
      .replace("<strong>", "<b>").replace("</strong>", "</b>")
      .replace("<em>", "<i>").replace("</em>", "</i>")
      .replace("<p>", "").replace("</p>", "<br>")

    val json =
      s"""
         |{
         |  "cards": [
         |    {
         |      "sections": [
         |        {
         |          "header": ${Json.fromString(notification.subject).noSpaces},
         |          "widgets": [
         |            {
         |              "textParagraph": {
         |                "text": ${Json.fromString(html).noSpaces}
         |              }
         |            }
         |          ]
         |        },
         |        {
         |          "widgets": [
         |            {
         |              "buttons": [
         |                ${notification.actions.map(textButtonJson).mkString(",")}
         |              ]
         |            }
         |          ]
         |        }
         |      ]
         |    }
         |  ]
         |}
         |""".stripMargin
    HangoutMessage(json)
  }

  private def textButtonJson(action: Action): String = {
    s"""
       |{
       |  "textButton": {
       |    "text": ${Json.fromString(action.cta).noSpaces},
       |    "onClick": {
       |      "openLink": {
       |        "url": ${Json.fromString(action.url).noSpaces}
       |      }
       |    }
       |  }
       |}
       |""".stripMargin
  }
}

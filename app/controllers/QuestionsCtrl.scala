package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

import play.api.libs.json._

// Akka imports
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{Props, ActorSystem}
import scala.language.postfixOps

import models._
import models.QuestionsActor.{QuestionRetrieve}
import models.QuestionResultStatus._

import globals._
import utils._
     
object QuestionsCtrl extends Controller with QuestionsJSONTrait {
    implicit val timeout = Timeout(5 seconds)
    implicit lazy val system = ActorSystem()
    implicit lazy val qActor = system.actorOf(Props[QuestionsActor])

    def retrieve(id: Long) = Action {
        var retOpt = None: Option[QuestionResult]

        val future = qActor ? QuestionRetrieve(id)
        retOpt = Await.result(future, timeout.duration)
                    .asInstanceOf[Option[QuestionResult]]

        if (retOpt.isDefined) {
            val questionResult = retOpt.get
            questionResult.status match {
                case Some(200) => {
                    Status(QUESTION_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(questionResult.status.get)
                }
            }
        } else {
            InternalServerError
        }
    }

    def actionHandler(
        handlerOpt: Option[String],
        jsonOpt: Option[JsValue]): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]

        var questionOptParseFromJSON = None: Option[Question]
        jsonOpt.foreach { json =>
            // validate the json to scala object
            json.validate[Question] match {
                case s: JsSuccess[Question] => {
                    questionOptParseFromJSON = Option(s.get)
                }
                case e: JsError => {
                    questionOptParseFromJSON = None
                }
            }
        }

        questionOptParseFromJSON.foreach { question =>
            val request = QuestionRequest(handlerOpt)
            retOpt = request.send(question)
        }

        retOpt
    }

    def actionAsync = Action { request =>
        val reqJSON = request.body.asJson.get

        val handlerOpt = (reqJSON \ "handler").asOpt[String]
        val questionJSONOpt = (reqJSON \ "question").asOpt[JsValue]

        questionJSONOpt.foreach { questionJSON =>
            val msgPack = new MsgWrapper(
                handlerOpt, Option(questionJSON.toString))
            val msgPackBytes = msgPack.toMsgPack

            val mqRequestHandler = HandlerMQFactory(
                Option("whipper.questions.mq"))
            mqRequestHandler.send(Option(msgPackBytes))
        }

        Status(200)
    }

    def action = Action { request =>
        var retOpt = None: Option[QuestionResult]

        val reqJSON = request.body.asJson.get
        val handlerOpt = (reqJSON \ "handler").asOpt[String]
        val questionJSONOpt = (reqJSON \ "question").asOpt[JsValue]

        retOpt = actionHandler(handlerOpt, questionJSONOpt)

        if (retOpt.isDefined) {
            val questionResult = retOpt.get
            questionResult.status match {
                case Some(200) => {
                    Status(QUESTION_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(questionResult.status.get)
                }
            }
        } else {
            InternalServerError
        }
    }
}
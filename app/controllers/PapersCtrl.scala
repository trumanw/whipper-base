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

import net.liftweb.json._
import net.liftweb.json.Serialization.write

import models._
import models.PapersActor.{PaperQuery, PaperRetrieve}
import models.PaperResultStatus._

import globals._
import utils._

object PapersCtrl extends Controller 
	with PapersJSONTrait with CompositesJSONTrait with QuestionsJSONTrait {
	implicit val timeout = Timeout(5 seconds)
	implicit lazy val system = ActorSystem()
	implicit lazy val pActor = system.actorOf(Props[PapersActor])

	def retrieve(id: Long) = Action {
		var retOpt = None: Option[PaperResult]

		val future = pActor ? PaperRetrieve(id)
		retOpt = Await.result(future, timeout.duration)
					.asInstanceOf[Option[PaperResult]]

		if (retOpt.isDefined) {
			val paperResult = retOpt.get
			paperResult.status match {
				case Some(200) => {
					Status(PAPER_OK.get)(Json.toJson(retOpt.get))
				}
				case _ => {
					Status(paperResult.status.get)
				}
			}
		} else {
			InternalServerError
		}
	}

	def query(
		page: Option[Int], 
		size: Option[Int]) = Action {
		var retOpt = None: Option[PaperListResult]

        val future = pActor ? PaperQuery(
                page.getOrElse(0), 
                size.getOrElse(10))
        retOpt = Await.result(future, timeout.duration)
                .asInstanceOf[Option[PaperListResult]]

		if (retOpt.isDefined) {
            val paperResult = retOpt.get
            paperResult.status match {
                case Some(200) => {
                    // return paper list result
                    Status(PAPER_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(paperResult.status.get)
                }
            }
        } else {
            InternalServerError
        }
	}

    def struct(id: Long) = Action {
        var retStructUnfoldPaper = None: Option[List[StructElemEx]]
        implicit val formats = DefaultFormats

        val paperFuture = pActor ? PaperRetrieve(id)
        val paperResult = Await.result(paperFuture, timeout.duration)
                            .asInstanceOf[Option[PaperResult]]
        if (paperResult.isDefined) {
            paperResult.get.status match {
                case Some(200) => {
                    val paper = paperResult.get.paper.get
                    if (paper.struct.isDefined) {
                        val structListPaper = paper.struct.get
                        retStructUnfoldPaper = StructElem.unfoldListIterate(structListPaper)
                    }
                }
                case _ => {}
            }
        }

        if (retStructUnfoldPaper.isDefined) {
            val retJSON = write(retStructUnfoldPaper.get)
            Status(200)(Json.parse(retJSON.toString))
        } else {
            InternalServerError
        }
    }

    def actionHandler(
        handlerOpt: Option[String],
        jsonOpt: Option[JsValue]): Option[PaperResult] = {
        var retOpt = None: Option[PaperResult]

        var paperOptParseFromJSON = None: Option[Paper]
        jsonOpt.foreach { json =>
            // validate the json to scala object
            json.validate[Paper] match {
                case s: JsSuccess[Paper] => {
                    paperOptParseFromJSON = Option(s.get)
                }
                case e: JsError => {
                    paperOptParseFromJSON = None
                }
            }
        }

        paperOptParseFromJSON.foreach { paper =>
            val request = PaperRequest(handlerOpt)
            retOpt = request.send(paper)
        }

        retOpt
    }

    def actionAsync = Action { request =>
        val reqJSON = request.body.asJson.get

        val handlerOpt = (reqJSON \ "handler").asOpt[String]
        val paperJSONOpt = (reqJSON \ "paper").asOpt[JsValue]

        paperJSONOpt.foreach { paperJSON =>
            val msgPack = new MsgWrapper(
                handlerOpt, Option(paperJSONOpt.toString))
            val msgPackBytes = msgPack.toMsgPack

            val mqRequestHandler = HandlerMQFactory(
                Option("whipper.papers.mq"))
            mqRequestHandler.send(Option(msgPackBytes))
        }

        Status(200)
    }

	def action = Action { request =>
        var retOpt = None: Option[PaperResult]

        val reqJSON = request.body.asJson.get
        val handlerOpt = (reqJSON \ "handler").asOpt[String]
        val paperJSONOpt = (reqJSON \ "paper").asOpt[JsValue]

        retOpt = actionHandler(handlerOpt, paperJSONOpt)

        if (retOpt.isDefined) {
            val paperResult = retOpt.get
            paperResult.status match {
                case Some(200) => {
                    Status(PAPER_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(paperResult.status.get)
                }
            }
        } else {
            InternalServerError
        }
    }
}
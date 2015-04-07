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

import models._
import models.PapersActor.{PaperQuery, PaperRetrieve, PaperAdd, PaperDelete, PaperUpdate}
import models.PapersActor.{PaperStructAppend, PaperStructRemove, PaperStructClean}
import models.PapersActor.{PaperAttrsAppend, PaperAttrsRemove, PaperAttrsClean}
import models.PapersActor.{PaperStatusProcess, PaperStatusReset}
import models.PaperResultStatus._

import globals._

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

	def action = Action { request =>
        var retOpt = None: Option[PaperResult]

        val reqJSON = request.body.asJson.get
        // parse the handler
        val handler = (reqJSON \ "handler").asOpt[String]
        val paperJSON = (reqJSON \ "paper").asOpt[JsValue]
        if (handler.isDefined && paperJSON.isDefined) {
        	var paperOptParseFromJSON = None: Option[Paper]
        	// validate the json data to scala class
        	paperJSON.get.validate[Paper] match {
        		case s: JsSuccess[Paper] => {
        			paperOptParseFromJSON = Option(s.get)
        		}
        		case e: JsError => {
        			// error handling flow
        		}
        	}

        	// match the handler with the different actor methods
        	if (paperOptParseFromJSON.isDefined) {
        		val paper = paperOptParseFromJSON.get

        		handler match {
        			case Some("whipper.papers.add") => {
        				val future = pActor ? PaperAdd(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.update") => {
        				val future = pActor ? PaperUpdate(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.delete") => {
        				val future = pActor ? PaperDelete(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.struct.append") => {
        				val future = pActor ? PaperStructAppend(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.struct.remove") => {
        				val future = pActor ? PaperStructRemove(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.struct.clean") => {
        				val future = pActor ? PaperStructClean(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.attrs.append") => {
        				val future = pActor ? PaperAttrsAppend(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.attrs.remove") => {
        				val future = pActor ? PaperAttrsRemove(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.papers.attrs.clean") => {
        				val future = pActor ? PaperAttrsClean(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.paper.status.process") => {
        				val future = pActor ? PaperStatusProcess(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case Some("whipper.paper.status.reset") => {
        				val future = pActor ? PaperStatusReset(
        							paper)
        				retOpt = Await.result(future, timeout.duration)
        						.asInstanceOf[Option[PaperResult]]
        			}
        			case _ => {}
        		}
        	}
        }

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
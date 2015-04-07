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
import models.ExamsActor.{ExamQuery, ExamRetrieve, ExamPublish, ExamRevoke}
import models.PapersActor.{PaperRetrieve}
import models.ExamResultStatus._

import globals._

object ExamsCtrl extends Controller 
	with ExamJSONTrait with PapersJSONTrait {
	implicit val timeout = Timeout(5 seconds)
	implicit lazy val system = ActorSystem()
	implicit lazy val eActor = system.actorOf(Props[ExamsActor])
	implicit lazy val pActor = system.actorOf(Props[PapersActor])

	def retrieve(id: Long) = Action { 
		var retOpt = None: Option[ExamResult]

		val future = eActor ? ExamsActor.ExamRetrieve(
					id)
		retOpt = Await.result(future, timeout.duration)
						.asInstanceOf[Option[ExamResult]]

		if (retOpt.isDefined) {
			val paperResult = retOpt.get
			paperResult.status match {
				case Some(200) => {
					Status(EXAM_OK.get)(Json.toJson(retOpt.get))
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
		var retOpt = None: Option[ExamListResult]

		val future = eActor ? ExamsActor.ExamQuery(
					page.getOrElse(0), size.getOrElse(10))
		retOpt = Await.result(future, timeout.duration)
					.asInstanceOf[Option[ExamListResult]]

		if (retOpt.isDefined) {
            val paperResult = retOpt.get
            paperResult.status match {
                case Some(200) => {
                    Status(EXAM_OK.get)(Json.toJson(retOpt.get))
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
		var retOpt = None: Option[ExamResult]

		val reqJSON = request.body.asJson.get
		// parse the handler
		val handler = (reqJSON \ "handler").asOpt[String]
		val examJSON = (reqJSON \ "exam").asOpt[JsValue]
		if (handler.isDefined && examJSON.isDefined) {
			var examOptParseFromJSON = None: Option[Exam]
			// validate the json data to scala class
			examJSON.get.validate[Exam] match {
				case s: JsSuccess[Exam] => {
					examOptParseFromJSON = Option(s.get)
				}
				case e: JsError => {
					// error handling flow
				}
			}

			// match the handler with the different actor methods
			if (examOptParseFromJSON.isDefined) {
				val exam = examOptParseFromJSON.get

				handler match {
					case Some("whipper.exams.publish.from.paper") => {
						val pidOpt = (reqJSON \ "pid").asOpt[Long]
						if (pidOpt.isDefined) {
							val pid = pidOpt.get
							// ask paper struct and attrs from the paper
							val paperFuture = pActor ? PaperRetrieve(pid)
							val paperRetOpt = Await.result(paperFuture, timeout.duration)
												.asInstanceOf[Option[PaperResult]]
							if (paperRetOpt.isDefined) {
								val paperRet = paperRetOpt.get
								paperRet.status match {
									case Some(200) => {
										exam.struct = paperRet.paper.get.struct
										exam.attrs = paperRet.paper.get.attrs
										// allocate a new exam with the paper data
										val future = eActor ? ExamPublish(
												exam)
										retOpt = Await.result(future, timeout.duration)
												.asInstanceOf[Option[ExamResult]]
									}
									case _ => {}
								}
							}
						}
					}
					case Some("whipper.exams.revoke") => {
						val future = eActor ? ExamRevoke(
									exam)
						retOpt = Await.result(future, timeout.duration)
								.asInstanceOf[Option[ExamResult]]
					}
					case _ => {}
				}
			}
		}

		if (retOpt.isDefined) {
			val examResult = retOpt.get
			examResult.status match {
				case Some(200) => {
					Status(EXAM_OK.get)(Json.toJson(retOpt.get))
				}
				case _ => {
					Status(examResult.status.get)
				}
			}
		} else {
			InternalServerError
		}
	}

}
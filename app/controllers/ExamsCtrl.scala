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
import models.ExamsActor.{ExamQuery, ExamRetrieve}
import models.ExamResultStatus._

import globals._
import utils._

object ExamsCtrl extends Controller 
	with ExamJSONTrait with PapersJSONTrait {
	implicit val timeout = Timeout(5 seconds)
	implicit lazy val system = ActorSystem()
	implicit lazy val eActor = system.actorOf(Props[ExamsActor])

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

	def struct(id: Long) = Action {
		var retStructUnfoldExam = None: Option[List[StructElemEx]]
		implicit val formats = DefaultFormats

		val examFuture = eActor ? ExamsActor.ExamRetrieve(id)
		val examResult = Await.result(examFuture, timeout.duration)
							.asInstanceOf[Option[ExamResult]]
		if (examResult.isDefined) {
			examResult.get.status match {
				case Some(200) => {
					val exam = examResult.get.exam.get
					if (exam.struct.isDefined) {
						val structListExam = exam.struct.get
						retStructUnfoldExam = StructElem.unfoldListIterate(structListExam)
					}
				}
				case _ => {}
			}
		}

		if (retStructUnfoldExam.isDefined) {
			val retJSON = write(retStructUnfoldExam.get)
			Status(200)(Json.parse(retJSON.toString))
		} else {
			InternalServerError
		}
	}

	def actionHandler(json: JsValue): Option[ExamResult] = {
		var retOpt = None: Option[ExamResult]

		// parse the handler
		val handler = (json \ "handler").asOpt[String]
		val examJSON = (json \ "exam").asOpt[JsValue]
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
				val request = ExamRequest(handler)
				retOpt = request.send(exam)	
			}
		}

		retOpt
	}

	def actionAsync = Action { request =>
    	val reqJSON = request.body.asJson.get

        val mqRequestHandler = HandlerMQFactory(
            Option("whipper.exams.mq"))
        mqRequestHandler.send(Option(reqJSON))

        Status(200)
    }

	def action = Action { request =>
		var retOpt = None: Option[ExamResult]

		val reqJSON = request.body.asJson.get
		retOpt = actionHandler(reqJSON)

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
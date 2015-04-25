package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

import play.api.libs.json._
import play.api.libs.functional.syntax._

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

	def query(
		page: Option[Int], 
		size: Option[Int]) = Action {
		var retOpt = None: Option[ExamListResult]

		
		val future = eActor ? ExamsActor.ExamQuery(
				page.getOrElse(0), size.getOrElse(10))
		retOpt = Await.result(future, timeout.duration)
					.asInstanceOf[Option[ExamListResult]]	

		if (retOpt.isDefined) {
            val examListResult = retOpt.get
            examListResult.status match {
                case Some(200) => {
                	// return exam list result
                    Status(EXAM_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(examListResult.status.get)
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

	def actionHandler(
		handlerOpt: Option[String],
		jsonOpt: Option[JsValue]): Option[ExamResult] = {
		var retOpt = None: Option[ExamResult]

		var examOptParseFromJSON = None: Option[Exam]
		jsonOpt.foreach { json =>
			// validate the json to scala object
			json.validate[Exam] match {
				case s: JsSuccess[Exam] => {
					examOptParseFromJSON = Option(s.get)
				}
				case e: JsError => {
					examOptParseFromJSON = None
				}
			}
		}

		examOptParseFromJSON.foreach { exam =>
			val request = ExamRequest(handlerOpt)
			retOpt = request.send(exam)
		}

		retOpt
	}

	def actionAsync = Action { request =>
    	val reqJSON = request.body.asJson.get

    	val handlerOpt = (reqJSON \ "handler").asOpt[String]
    	val examJSONOpt = (reqJSON \ "exam").asOpt[JsValue]

    	examJSONOpt.foreach { examJSON =>
    		val msgPack = new MsgWrapper(
    			handlerOpt, Option(examJSON.toString))
    		val msgPackBytes = msgPack.toMsgPack

    		val mqRequestHandler = HandlerMQFactory(
	            Option("whipper.exams.mq"))
    		mqRequestHandler.send(Option(msgPackBytes))
    	}

        Status(200)
    }

	def action = Action { request =>
		var retOpt = None: Option[ExamResult]

		val reqJSON = request.body.asJson.get
		val handlerOpt = (reqJSON \ "handler").asOpt[String]
		val examJSONOpt = (reqJSON \ "exam").asOpt[JsValue]

		retOpt = actionHandler(handlerOpt, examJSONOpt)

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
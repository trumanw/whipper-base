package models

import play.api.Logger
// Akka imports
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{Props, ActorSystem}
import scala.language.postfixOps

import models._
import models.ExamsActor.{ExamQuery, ExamRetrieve, ExamPublish, ExamRevoke}
import models.PapersActor.{PaperRetrieve}

trait ExamRequest {
	def send(exam: Exam): Option[ExamResult]
}

object ExamRequest {

	implicit val timeout = Timeout(5 seconds)
	implicit lazy val system = ActorSystem()
	implicit lazy val eActor = system.actorOf(Props[ExamsActor])
	implicit lazy val pActor = system.actorOf(Props[PapersActor])

	private class EXAM_NULL extends ExamRequest {
		override def send(exam: Exam): Option[ExamResult] = {
			return None
		}
	}

	private class EXAM_PUBLISH_FROM_PAPER extends ExamRequest {
		override def send(exam: Exam): Option[ExamResult] = {
			var retOpt = None: Option[ExamResult]
			val pidOpt = exam.pid
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
			retOpt
		}
	}

	private class EXAM_REVOKE extends ExamRequest {
		override def send(exam: Exam): Option[ExamResult] = {
			val future = eActor ? ExamRevoke(exam)
			Await.result(future, timeout.duration)
				.asInstanceOf[Option[ExamResult]]
		}
	}

	def apply(handler: Option[String]): ExamRequest = {
		handler match {
			case Some("whipper.exams.publish.from.paper") => {
				return new EXAM_PUBLISH_FROM_PAPER
			}
			case Some("whipper.exams.revoke") => {
				return new EXAM_REVOKE
			}
			case _ => {
				Logger.warn("Invalid handler - " + handler.getOrElse(""))
				return new EXAM_NULL
			}
		}
	}
}
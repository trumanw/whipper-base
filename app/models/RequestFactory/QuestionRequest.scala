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
import models.QuestionsActor.{QuestionRetrieve, QuestionAdd, QuestionUpdate, QuestionDelete}
import models.QuestionsActor.{QuestionAttrsAppend, QuestionAttrsRemove, QuestionAttrsClean}

// this is a factory method for 
// the handlers of question requests
trait QuestionRequest {
	def send(question: Question): Option[QuestionResult]
}

object QuestionRequest {

	implicit val timeout = Timeout(5 seconds)
    implicit lazy val system = ActorSystem()
    implicit lazy val qActor = system.actorOf(Props[QuestionsActor])

    private class QUESTION_NULL extends QuestionRequest {
    	override def send(
    		question: Question): Option[QuestionResult] = {

    		// nothing will happen
    		return None

    	}
    }

	private class QUESTION_ADD extends QuestionRequest {
		override def send(
			question: Question): Option[QuestionResult] = {

			val future = qActor ? QuestionAdd(question)
            Await.result(future, timeout.duration)
        		.asInstanceOf[Option[QuestionResult]]

		}
	}

	private class QUESTION_UPDATE extends QuestionRequest {
		override def send(
			question: Question): Option[QuestionResult] = {

			val future = qActor ? QuestionUpdate(question)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[QuestionResult]]

		}
	}

	private class QUESTION_DELETE extends QuestionRequest {
		override def send(
			question: Question): Option[QuestionResult] = {

			val future = qActor ? QuestionDelete(question)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[QuestionResult]]

		}
	}

	private class QUESTION_ATTRS_APPEND extends QuestionRequest {
		override def send(
    		question: Question): Option[QuestionResult] = {

    		val future = qActor ? QuestionAttrsAppend(question)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[QuestionResult]]

    	}
	}

	private class QUESTION_ATTRS_REMOVE extends QuestionRequest {
		override def send(
    		question: Question): Option[QuestionResult] = {

    		val future = qActor ? QuestionAttrsRemove(question)
            Await.result(future, timeout.duration)
                .asInstanceOf[Option[QuestionResult]]

    	}
	}

	private class QUESTION_ATTRS_CLEAN extends QuestionRequest {
		override def send(
    		question: Question): Option[QuestionResult] = {
    		
    		val future = qActor ? QuestionAttrsClean(question)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[QuestionResult]]

    	}
	}

	def apply(handler: Option[String]): QuestionRequest = {
		handler match {
			case Some("whipper.question.add") => {
				return new QUESTION_ADD
			}
			case Some("whipper.question.update") => {
				return new QUESTION_UPDATE
			}
			case Some("whipper.question.delete") => {
				return new QUESTION_DELETE
			}
			case Some("whipper.questions.attrs.append") => {
				return new QUESTION_ATTRS_APPEND
			}
			case Some("whipper.questions.attrs.remove") => {
				return new QUESTION_ATTRS_REMOVE
			}
			case Some("whipper.questions.attrs.clean") => {
				return new QUESTION_ATTRS_CLEAN
			}
			case _ => {
				Logger.warn("Invalid handler - " + handler.getOrElse(""))
				return new QUESTION_NULL
			}
		}
	}
}
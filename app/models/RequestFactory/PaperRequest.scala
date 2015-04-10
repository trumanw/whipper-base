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
import models.PapersActor.{PaperQuery, PaperRetrieve, PaperAdd, PaperDelete, PaperUpdate}
import models.PapersActor.{PaperStructAppend, PaperStructRemove, PaperStructClean}
import models.PapersActor.{PaperAttrsAppend, PaperAttrsRemove, PaperAttrsClean}
import models.PapersActor.{PaperStatusProcess, PaperStatusReset}

trait PaperRequest {
	def send(paper: Paper): Option[PaperResult]
}

object PaperRequest {

	implicit val timeout = Timeout(5 seconds)
	implicit lazy val system = ActorSystem()
	implicit lazy val pActor = system.actorOf(Props[PapersActor])

	private class PAPER_NULL extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			// nothing will happen
			return None
		}
	}

	private class PAPER_ADD extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperAdd(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_UPDATE extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperUpdate(paper)
			Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_DELETE extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperDelete(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_STRUCT_APPEND extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperStructAppend(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_STRUCT_REMOVE extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperStructRemove(paper)
        	Await.result(future, timeout.duration)
    			.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_STRUCT_CLEAN extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperStructClean(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_ATTRS_APPEND extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperAttrsAppend(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_ATTRS_REMOVE extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperAttrsRemove(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_ATTRS_CLEAN extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperAttrsClean(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_STATUS_PROCESS extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperStatusProcess(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	private class PAPER_STATUS_RESET extends PaperRequest {
		override def send(paper: Paper): Option[PaperResult] = {
			val future = pActor ? PaperStatusReset(paper)
        	Await.result(future, timeout.duration)
				.asInstanceOf[Option[PaperResult]]
		}
	}

	def apply(handler: Option[String]): PaperRequest = {
		handler match {
			case Some("whipper.papers.add") => {
				return new PAPER_ADD
			}
			case Some("whipper.papers.update") => {
				return new PAPER_UPDATE
			}
			case Some("whipper.papers.delete") => {
				return new PAPER_DELETE
			}
			case Some("whipper.papers.struct.append") => {
				return new PAPER_STRUCT_APPEND
			}
			case Some("whipper.papers.struct.remove") => {
				return new PAPER_STRUCT_REMOVE
			}
			case Some("whipper.papers.struct.clean") => {
				return new PAPER_STRUCT_CLEAN
			}
			case Some("whipper.papers.attrs.append") => {
				return new PAPER_ATTRS_APPEND
			}
			case Some("whipper.papers.attrs.remove") => {
				return new PAPER_ATTRS_REMOVE
			}
			case Some("whipper.papers.attrs.clean") => {
				return new PAPER_ATTRS_CLEAN
			}
			case Some("whipper.paper.status.process") => {
				return new PAPER_STATUS_PROCESS
			}
			case Some("whipper.paper.status.reset") => {
				return new PAPER_STATUS_RESET
			}
			case _ => {
				Logger.warn("Invalid handler - " + handler.getOrElse(""))
				return new PAPER_NULL
			}
		}
	}
}
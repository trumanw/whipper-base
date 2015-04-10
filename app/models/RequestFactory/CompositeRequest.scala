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
import models.CompositesActor.{CompositeRetrieve, CompositeAdd, CompositeUpdate, CompositeDelete}
import models.CompositesActor.{CompositeStructAppend, CompositeStructRemove, CompositeStructClean}

// interface of composite handler
trait CompositeRequest {
	def send(composite: Composite): Option[CompositeResult]
}

object CompositeRequest {

	implicit val timeout = Timeout(5 seconds)
    implicit lazy val system = ActorSystem()
    implicit lazy val compositeActor = system.actorOf(Props[CompositesActor])

    private class COMPOSITE_NULL extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		// nothing will happen
    		return None

    	}
    }

    private class COMPOSITE_ADD extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		val future = compositeActor ? CompositeAdd(composite)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[CompositeResult]]
    	}
    }

    private class COMPOSITE_UPDATE extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		val future = compositeActor ? CompositeUpdate(composite)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[CompositeResult]]
    	}
    }

    private class COMPOSITE_DELETE extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		val future = compositeActor ? CompositeDelete(composite)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[CompositeResult]]
    	}
    }

    private class COMPOSITE_STRUCT_APPEND extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		val future = compositeActor ? CompositeStructAppend(composite)
            Await.result(future, timeout.duration)
                .asInstanceOf[Option[CompositeResult]]
    	}
    }

    private class COMPOSITE_STRUCT_REMOVE extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		val future = compositeActor ? CompositeStructRemove(composite)
            Await.result(future, timeout.duration)
            	.asInstanceOf[Option[CompositeResult]]
    	}
    }

    private class COMPOSITE_STRUCT_CLEAN extends CompositeRequest {
    	override def send(
    		composite: Composite): Option[CompositeResult] = {

    		val future = compositeActor ? CompositeStructClean(composite)
            Await.result(future, timeout.duration)
                .asInstanceOf[Option[CompositeResult]]
    	}
    }

    def apply(handler: Option[String]): CompositeRequest = {
    	handler match {
    		case Some("whipper.composites.add") => {
    			return new COMPOSITE_ADD
    		}
    		case Some("whipper.composites.update") => {
    			return new COMPOSITE_UPDATE
    		}
    		case Some("whipper.composites.delete") => {
    			return new COMPOSITE_DELETE
    		}
    		case Some("whipper.composites.struct.append") => {
    			return new COMPOSITE_STRUCT_APPEND
    		}
    		case Some("whipper.composites.struct.remove") => {
    			return new COMPOSITE_STRUCT_REMOVE
    		}
    		case Some("whipper.composites.struct.clean") => {
    			return new COMPOSITE_STRUCT_CLEAN
    		}
    		case _ => {
    			Logger.warn("Invalid handler - " + handler.getOrElse(""))
				return new COMPOSITE_NULL
    		}
    	}
    }
}
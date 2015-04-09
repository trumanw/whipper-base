package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import scala.language.postfixOps

import play.api.libs.json._

// Akka imports
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{Props, ActorSystem}

import net.liftweb.json._
import net.liftweb.json.Serialization.write

import models._
import models.CompositesActor.{CompositeRetrieve, CompositeAdd, CompositeUpdate, CompositeDelete}
import models.CompositesActor.{CompositeStructAppend, CompositeStructRemove, CompositeStructClean}
import models.CompositeResultStatus._

import globals._

object CompositesCtrl extends Controller 
    with CompositesJSONTrait with QuestionsJSONTrait {

    implicit val timeout = Timeout(5 seconds)
    implicit lazy val system = ActorSystem()
    implicit lazy val compositeActor = system.actorOf(Props[CompositesActor])
    implicit lazy val questionActor = system.actorOf(Props[QuestionsActor])

    def retrieve(id: Long) = Action {
        var retOpt = None: Option[CompositeResult]

        val future = compositeActor ? CompositeRetrieve(id)
        retOpt = Await.result(future, timeout.duration)
                    .asInstanceOf[Option[CompositeResult]]

        if (retOpt.isDefined) {
            val compositeResult = retOpt.get
            compositeResult.status match {
                case Some(200) => {
                    Status(COMPOSITE_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(compositeResult.status.get)
                }
            }
        } else {
            InternalServerError
        }
    }

    def struct(id: Long) = Action {
        var retStructUnfoldComposite = None: Option[List[StructElemEx]]
        implicit val formats = DefaultFormats

        val compositeFuture = compositeActor ? CompositeRetrieve(id)
        val compositeResult = Await.result(compositeFuture, timeout.duration)
                                .asInstanceOf[Option[CompositeResult]]
        if (compositeResult.isDefined) {
            compositeResult.get.status match {
                case Some(200) => {
                    val composite = compositeResult.get.composite.get
                    if (composite.struct.isDefined) {
                        val structListComposite = composite.struct.get
                        retStructUnfoldComposite = StructElem.unfoldListIterate(structListComposite)
                    }
                }
                case _ => {}
            }
        }

        if (retStructUnfoldComposite.isDefined) {
            val retJSON = write(retStructUnfoldComposite.get)
            Status(200)(Json.parse(retJSON.toString))
        } else {
            InternalServerError
        }
    }

    def action = Action { request =>
        var retOpt = None: Option[CompositeResult]

        val reqJSON = request.body.asJson.get
        // parse the handler
        val handler = (reqJSON \ "handler").asOpt[String]
        val compositeJSON = (reqJSON \ "composite").asOpt[JsValue]
        if (handler.isDefined && compositeJSON.isDefined) {
            var compositeOptParseFromJSON = None: Option[Composite]
            // validate the json data to scala class
            compositeJSON.get.validate[Composite] match {
                case s: JsSuccess[Composite] => {
                    compositeOptParseFromJSON = Option(s.get)
                }
                case e: JsError => {
                    // error handling flow
                }
            }

            // match the handler with the different actor methods
            if (compositeOptParseFromJSON.isDefined) {
                val composite = compositeOptParseFromJSON.get

                handler match {
                    case Some("whipper.composites.add") => {
                        val future = compositeActor ? CompositeAdd(
                            composite)
                        retOpt = Await.result(future, timeout.duration)
                                    .asInstanceOf[Option[CompositeResult]]
                    }
                    case Some("whipper.composites.update") => {
                        val future = compositeActor ? CompositeUpdate(
                            composite)
                        retOpt = Await.result(future, timeout.duration)
                                    .asInstanceOf[Option[CompositeResult]]
                    }
                    case Some("whipper.composites.delete") => {
                        val future = compositeActor ? CompositeDelete(
                            composite)
                        retOpt = Await.result(future, timeout.duration)
                                    .asInstanceOf[Option[CompositeResult]]
                    }
                    case Some("whipper.composites.struct.append") => {
                        val future = compositeActor ? CompositeStructAppend(
                            composite)
                        retOpt = Await.result(future, timeout.duration)
                                    .asInstanceOf[Option[CompositeResult]]
                    }
                    case Some("whipper.composites.struct.remove") => {
                        val future = compositeActor ? CompositeStructRemove(
                            composite)
                        retOpt = Await.result(future, timeout.duration)
                                    .asInstanceOf[Option[CompositeResult]]
                    }
                    case Some("whipper.composites.struct.clean") => {
                        val future = compositeActor ? CompositeStructClean(
                            composite)
                        retOpt = Await.result(future, timeout.duration)
                                    .asInstanceOf[Option[CompositeResult]]
                    }
                    case _ => {}
                }
            }
        }

        if (retOpt.isDefined) {
            val compositeResult = retOpt.get
            compositeResult.status match {
                case Some(200) => {
                    Status(COMPOSITE_OK.get)(Json.toJson(retOpt.get))
                }
                case _ => {
                    Status(compositeResult.status.get)
                }
            }
        } else {
            InternalServerError
        }
    }
}
package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

import play.api.libs.json._

// Play cached support 
import play.api.cache.Cached
import play.api.cache.Cache

import scala.collection.mutable.ListBuffer

// Akka imports
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.{Props, ActorSystem}

import models._
import models.CatalogsActor.{CatalogQuery, CatalogRetrieve, CatalogAdd, CatalogDelete, CatalogUpdate}
import models.CatalogsActor.{CatalogElemsAppend, CatalogElemsRemove, CatalogElemsClean}
import models.CatalogResultStatus._

import globals._
import utils._

object CatalogCtrl extends Controller with CatalogJSONTrait {
	implicit val timeout = Timeout(5 seconds)
	implicit lazy val system = ActorSystem()
	implicit lazy val catalogActor = system.actorOf(Props[CatalogsActor])

	def retrieve(id: Long) = Action {
		var retOpt = None: Option[CatalogResult]

		val future = catalogActor ? CatalogRetrieve(id)
		retOpt = Await.result(future, timeout.duration)
					.asInstanceOf[Option[CatalogResult]]

		if (retOpt.isDefined) {
			val catalogResult = retOpt.get
			catalogResult.status match {
				case Some(200) => {
					Status(CATALOG_OK.get)(Json.toJson(retOpt.get))
				}
				case _ => {
					Status(catalogResult.status.get)
				}
			}
		} else {
			InternalServerError
		}
	}

	def query(
		page: Option[Int], 
		size: Option[Int]) = Action {
		var retOpt = None: Option[CatalogListResult]

		val future = catalogActor ? CatalogQuery(
					page.getOrElse(0),
					size.getOrElse(10))
		retOpt = Await.result(future, timeout.duration)
				.asInstanceOf[Option[CatalogListResult]]

		if (retOpt.isDefined) {
			val catalogListResult = retOpt.get
			catalogListResult.status match {
				case Some(200) => {
					Status(CATALOG_OK.get)(Json.toJson(retOpt.get))
				}
				case _ => {
					Status(catalogListResult.status.get)
				}
			}
		} else {
			InternalServerError
		}
	}

	def action = Action { request =>
		var retOpt = None: Option[CatalogResult]

		val reqJSON = request.body.asJson.get
		// parse the handler
		val handler = (reqJSON \ "handler").asOpt[String]
		val catalogJSON = (reqJSON \ "catalog").asOpt[JsValue]
		if (handler.isDefined && catalogJSON.isDefined) {
			var catalogOptParseFromJSON = None: Option[Catalog]
			// validate the json data to scala class
			catalogJSON.get.validate[Catalog] match {
				case s: JsSuccess[Catalog] => {
					catalogOptParseFromJSON = Option(s.get)
				}
				case e: JsError => {
					// error handling flow
				}
			}

			// match the handler with the different actor methods
			if (catalogOptParseFromJSON.isDefined) {
				val catalog = catalogOptParseFromJSON.get

				handler match {
					case Some("whipper.catalog.add") => {
						val future = catalogActor ? CatalogAdd(catalog)
						retOpt = Await.result(future, timeout.duration)
									.asInstanceOf[Option[CatalogResult]]
					}
					case Some("whipper.catalog.update") => {
						val future = catalogActor ? CatalogUpdate(catalog)
						retOpt = Await.result(future, timeout.duration)
									.asInstanceOf[Option[CatalogResult]]
					}
					case Some("whipper.catalog.delete") => {
						val future = catalogActor ? CatalogDelete(catalog)
						retOpt = Await.result(future, timeout.duration)
									.asInstanceOf[Option[CatalogResult]]
					}
					case Some("whipper.catalog.elems.append") => {
						val future = catalogActor ? CatalogElemsAppend(catalog)
						retOpt = Await.result(future, timeout.duration)
									.asInstanceOf[Option[CatalogResult]]
					}
					case Some("whipper.catalog.elems.remove") => {
						val future = catalogActor ? CatalogElemsRemove(catalog)
						retOpt = Await.result(future, timeout.duration)
									.asInstanceOf[Option[CatalogResult]]
					}
					case Some("whipper.catalog.elems.clean") => {
						val future = catalogActor ? CatalogElemsClean(catalog)
						retOpt = Await.result(future, timeout.duration)
									.asInstanceOf[Option[CatalogResult]]
					}
					case _ => {}
				}
			}
		}

		if (retOpt.isDefined) {
			val catalogResult = retOpt.get
			catalogResult.status match {
				case Some(200) => {
					Status(CATALOG_OK.get)(Json.toJson(retOpt.get))
				}
				case _ => {
					Status(catalogResult.status.get)
				}
			}
		} else {
			InternalServerError
		}
	}
}
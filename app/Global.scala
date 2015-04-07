package globals
import play.api._
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import play.filters.gzip.GzipFilter

import play.api.db.DB
import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.json._

import filters._

object Global extends WithFilters(
	LoggingFilter, CORSFilter, new GzipFilter()) with GlobalSettings {
	
	implicit lazy val db = Database.forDataSource(DB.getDataSource("default"))

	def wrap(raw: Option[JsValue]): Option[String] = {
		var wrapper = None: Option[String]
		if (raw.isDefined) {
			wrapper = Option(raw.get.toString)
		}
		wrapper
	}

	override def onStart(app: Application) {
		Logger.info("Whipper-Base service has started.")
	}

	override def onStop(app: Application) {
		Logger.info("Whipper-Base service has stopped.")
	}
}
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

import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}
import com.rabbitmq.client.{DefaultConsumer, Envelope}
import com.rabbitmq.client.AMQP

import utils._
import filters._

object Global extends WithFilters(
	LoggingFilter, CORSFilter, new GzipFilter()) with GlobalSettings {
	
	implicit lazy val db = Database.forDataSource(DB.getDataSource("default"))

	override def onStart(app: Application) {
		UpdateConsumer.start
		Logger.info("Whipper-Base service has started.")
	}

	override def onStop(app: Application) {
		UpdateConsumer.stop
		Logger.info("Whipper-Base service has stopped.")
	}
}

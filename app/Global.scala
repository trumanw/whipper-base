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

import filters._

object Global extends WithFilters(
	LoggingFilter, CORSFilter, new GzipFilter()) with GlobalSettings {
	
	implicit lazy val db = Database.forDataSource(DB.getDataSource("default"))

	// RabbitMQ
	private val EXCAHNGE_NAME = "UpdateExchange"

	private val factory: ConnectionFactory = new ConnectionFactory()
	// factory.setHost("112.124.20.51")
	// factory.setUsername("admin")
 //    factory.setPassword("039NpFalhcDs")
 	factory.setHost(Play.current.configuration.getString("mq.default.url").getOrElse(""))
 	factory.setUsername(Play.current.configuration.getString("mq.default.user").getOrElse(""))
 	factory.setPassword(Play.current.configuration.getString("mq.default.password").getOrElse(""))
    private val QUEUE_NAME = "UpdateExchange"

    val connection = factory.newConnection()
    val channel = connection.createChannel()

	override def onStart(app: Application) {
		Logger.info("Whipper-Base service has started.")

	    channel.queueDeclare(QUEUE_NAME, false, false, false, null)

	    // val consumer = new QueueingConsumer(channel)
	    val consumer = new DefaultConsumer(channel) {
	    	override def handleDelivery(
	    		consumerTag: String, envelope: Envelope, 
	    		properties: AMQP.BasicProperties, body: Array[Byte]) {
				println("received: " +  new String(body, "UTF-8"))
	    	}
	    }

	    channel.basicConsume(QUEUE_NAME, true, consumer)
	}

	override def onStop(app: Application) {
		Logger.info("RabbitMQ consumer channel is closing...")
		channel.close()
		Logger.info("RabbitMQ consumer channel has closed!")
		Logger.info("RabbitMQ consumer connection is closing...")
		connection.close()
		Logger.info("RabbitMQ consumer connection has closed!")
		Logger.info("Whipper-Base service has stopped.")
	}
}
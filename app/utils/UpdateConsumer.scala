package utils

import play.api._
import play.api.Play
import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}
import com.rabbitmq.client.{DefaultConsumer, Envelope}
import com.rabbitmq.client.AMQP

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.duration._
import scala.language.postfixOps

object UpdateConsumer {
	private val rbmqFactory: ConnectionFactory = new ConnectionFactory()
 	rbmqFactory.setHost(Play.current.configuration.getString("mq.default.url").getOrElse(""))
 	rbmqFactory.setUsername(Play.current.configuration.getString("mq.default.user").getOrElse(""))
 	rbmqFactory.setPassword(Play.current.configuration.getString("mq.default.password").getOrElse(""))
    private val QUEUE_NAME = "UpdateExchange"

    val rbmqConnection = rbmqFactory.newConnection()
    val rbmqChannel = rbmqConnection.createChannel()

    def start = {
    	rbmqChannel.queueDeclare(QUEUE_NAME, false, false, false, null)

	    val consumer = new DefaultConsumer(rbmqChannel) {
	    	override def handleDelivery(
	    		consumerTag: String, envelope: Envelope, 
	    		properties: AMQP.BasicProperties, body: Array[Byte]) {
				println("received: " +  new String(body, "UTF-8"))
	    	}
	    }

	    rbmqChannel.basicConsume(QUEUE_NAME, true, consumer)
    }

    def stop = {
    	Logger.info("RabbitMQ consumer channel is closing...")
		rbmqChannel.close()
		Logger.info("RabbitMQ consumer channel has closed!")
		Logger.info("RabbitMQ consumer connection is closing...")
		rbmqConnection.close()
		Logger.info("RabbitMQ consumer connection has closed!")
    }
}
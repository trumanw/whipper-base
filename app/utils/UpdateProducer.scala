package utils

import play.api.Play
import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.duration._
import scala.language.postfixOps

object UpdateProducer {

    private val factory: ConnectionFactory = new ConnectionFactory()
    factory.setHost(Play.current.configuration.getString("mq.default.url").getOrElse(""))
    factory.setUsername(Play.current.configuration.getString("mq.default.user").getOrElse(""))
    factory.setPassword(Play.current.configuration.getString("mq.default.password").getOrElse(""))
    private val QUEUE_NAME = "UpdateExchange"

    def send = {
        val connection = factory.newConnection()
        val channel = connection.createChannel()

        channel.queueDeclare(QUEUE_NAME, false, false, false, null)
        val message = "Hey rabbitmq!"
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"))

        channel.close()
        connection.close()
    }
}

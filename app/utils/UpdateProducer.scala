package utils

import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.duration._
import scala.language.postfixOps

object UpdateProducer {

    private val factory = new ConnectionFactory()
    factory.setHost("112.124.20.51")
    factory.setUsername("admin")
    factory.setPassword("039NpFalhcDs")
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

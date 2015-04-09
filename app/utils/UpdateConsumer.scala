package utils

import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}
import com.rabbitmq.client.{DefaultConsumer, Envelope}
import com.rabbitmq.client.AMQP

object UpdateConsumer {
	private val EXCAHNGE_NAME = "UpdateExchange"

	private val factory: ConnectionFactory = new ConnectionFactory()
	factory.setHost("112.124.20.51")
	factory.setUsername("admin")
    factory.setPassword("039NpFalhcDs")
    private val QUEUE_NAME = "UpdateExchange"
	
	def recv = {
		val connection = factory.newConnection()
        val channel = connection.createChannel()

        channel.queueDeclare(QUEUE_NAME, false, false, false, null)

        // val consumer = new QueueingConsumer(channel)
        val consumer = new DefaultConsumer(channel) {
        	override def handleDelivery(
        		consumerTag: String, envelope: Envelope, 
        		properties: AMQP.BasicProperties, body: Array[Byte]) {
    			println("received: " + fromBytes(body))
        	}
        }

        channel.basicConsume(QUEUE_NAME, true, consumer)
	}

	def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")
}
package utils

import play.api._
import play.api.libs.json._
import play.api.Play

import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}
import com.rabbitmq.client.{DefaultConsumer, Envelope}
import com.rabbitmq.client.AMQP

import models._
import controllers._

trait HandlerMQ {
	def init
	def stop
	def send(msg: Option[JsValue])
}

object HandlerMQFactory {

	private val factory: ConnectionFactory = new ConnectionFactory()
	factory.setHost(Play.current.configuration.getString("mq.default.url").getOrElse(""))
 	factory.setUsername(Play.current.configuration.getString("mq.default.user").getOrElse(""))
 	factory.setPassword(Play.current.configuration.getString("mq.default.password").getOrElse(""))

	private class HandlerMQ_NULL extends HandlerMQ {
		override def init = {}
		override def stop = {}
		override def send(msg: Option[JsValue]) {} 
	}
	private class QuestionHandlerMQ extends HandlerMQ with QuestionsJSONTrait {
		private val QUEUE_NAME = "QuestionHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init = {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
					// parse json to object
					val messageString = new String(body, "UTF-8")
					val messageJSON = Json.parse(messageString)

					QuestionsCtrl.actionHandler(messageJSON)
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop = {
			channel.close()
			connection.close()
		}
		override def send(msg: Option[JsValue]) {

			if (msg.isDefined) {
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				val msgString = msg.toString
				channel.basicPublish("", QUEUE_NAME, null, msgString.getBytes("UTF-8"))
			}

			this.stop
		}
	}

	private class CompositeHandlerMQ extends HandlerMQ {
		private val QUEUE_NAME = "CompositeHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init = {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
					// parse json to object
					val messageString = new String(body, "UTF-8")
					val messageJSON = Json.parse(messageString)

					CompositesCtrl.actionHandler(messageJSON)
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop = {
			channel.close()
			connection.close()
		}
		override def send(msg: Option[JsValue]) {

			if (msg.isDefined) {
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				val msgString = msg.toString
				channel.basicPublish("", QUEUE_NAME, null, msgString.getBytes("UTF-8"))
			}

			this.stop
		}
	}

	private class PaperHandlerMQ extends HandlerMQ {
		private val QUEUE_NAME = "PaperHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init = {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
					// parse json to object
					val messageString = new String(body, "UTF-8")
					val messageJSON = Json.parse(messageString)

					PapersCtrl.actionHandler(messageJSON)
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop = {
			channel.close()
			connection.close()
		}
		override def send(msg: Option[JsValue]) {

			if (msg.isDefined) {
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				val msgString = msg.toString
				channel.basicPublish("", QUEUE_NAME, null, msgString.getBytes("UTF-8"))
			}

			this.stop
		}
	}

	private class ExamHandlerMQ extends HandlerMQ {
		private val QUEUE_NAME = "ExamHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init = {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
					// parse json to object
					val messageString = new String(body, "UTF-8")
					val messageJSON = Json.parse(messageString)

					ExamsCtrl.actionHandler(messageJSON)
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop = {
			channel.close()
			connection.close()
		}
		override def send(msg: Option[JsValue]) {

			if (msg.isDefined) {
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				val msgString = msg.toString
				channel.basicPublish("", QUEUE_NAME, null, msgString.getBytes("UTF-8"))
			}

			this.stop
		}
	}

	def apply(handlerMQType: Option[String]): HandlerMQ = {
		handlerMQType match {
			case Some("whipper.questions.mq") => {
				return new QuestionHandlerMQ
			}
			case Some("whipper.composites.mq") => {
				return new CompositeHandlerMQ
			}
			case Some("whipper.papers.mq") => {
				return new PaperHandlerMQ
			}
			case Some("whipper.exams.mq") => {
				return new ExamHandlerMQ
			}
			case _ => {
				Logger.warn("Invalid handlerMQType - " + handlerMQType.getOrElse(""))
				return new HandlerMQ_NULL
			}
		}
	}
}
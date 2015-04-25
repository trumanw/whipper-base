package utils

import play.api._
import play.api.libs.json._
import play.api.Play

import com.rabbitmq.client.{ConnectionFactory, Connection, Channel}
import com.rabbitmq.client.{DefaultConsumer, Envelope}
import com.rabbitmq.client.AMQP

import models._
import controllers._

// msgpack
import org.msgpack.annotation.Message
import org.msgpack.ScalaMessagePack

@Message
class MsgWrapper(
	var handler: Option[String],
	var msgbody: Option[String]) {
	def this() = this(None, None)
	def toMsgPack = ScalaMessagePack.write(this)
	def fromMsgPack(bai: Array[Byte]): Unit = {
		val restore = ScalaMessagePack.read[MsgWrapper](bai)
		this.handler = restore.handler
		this.msgbody = restore.msgbody
	}
}

trait HandlerMQ {
	def init {}
	def stop {}
	// def send(msg: Option[JsValue])
	def send(bytesOpt: Option[Array[Byte]]) {}
}

object HandlerMQFactory {

	private val factory: ConnectionFactory = new ConnectionFactory()
	factory.setHost(Play.current.configuration.getString("mq.default.url").getOrElse(""))
 	factory.setUsername(Play.current.configuration.getString("mq.default.user").getOrElse(""))
 	factory.setPassword(Play.current.configuration.getString("mq.default.password").getOrElse(""))

	private class HandlerMQ_NULL extends HandlerMQ {}

	private class QuestionHandlerMQ extends HandlerMQ with QuestionsJSONTrait {
		private val QUEUE_NAME = "QuestionHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
		    		// message unpack via MsgPack
		    		val recvMsgPack = new MsgWrapper(None, None)
		    		recvMsgPack.fromMsgPack(body)

		    		val handlerOpt = recvMsgPack.handler
		    		recvMsgPack.msgbody.foreach { msgbodyString =>
		    			val questionJSON = Json.parse(msgbodyString)
		    			val questionJSONOpt = Some(questionJSON)

		    			QuestionsCtrl.actionHandler(handlerOpt, questionJSONOpt)
		    		}
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop {
			channel.close()
			connection.close()
		}
		override def send(bytesOpt: Option[Array[Byte]]) {
			bytesOpt.foreach { bytes =>
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				channel.basicPublish("", QUEUE_NAME, null, bytes)
			}

			this.stop
		}
	}

	private class CompositeHandlerMQ extends HandlerMQ {
		private val QUEUE_NAME = "CompositeHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
					// message unpack via MsgPack
					val recvMsgPack = new MsgWrapper(None, None)
					recvMsgPack.fromMsgPack(body)

					val handlerOpt = recvMsgPack.handler
					recvMsgPack.msgbody.foreach { msgbodyString =>
						val compositesJSON = Json.parse(msgbodyString)
						val compositesJSONOpt = Some(compositesJSON)

						CompositesCtrl.actionHandler(handlerOpt, compositesJSONOpt)
					}
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop {
			channel.close()
			connection.close()
		}
		override def send(bytesOpt: Option[Array[Byte]]) {
			bytesOpt.foreach { bytes =>
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				channel.basicPublish("", QUEUE_NAME, null, bytes)
			}

			this.stop
		}
	}

	private class PaperHandlerMQ extends HandlerMQ {
		private val QUEUE_NAME = "PaperHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
					// message unpack via MsgPack
					val recvMsgPack = new MsgWrapper(None, None)
					recvMsgPack.fromMsgPack(body)

					val handlerOpt = recvMsgPack.handler
					recvMsgPack.msgbody.foreach { msgbodyString =>
						val paperJSON = Json.parse(msgbodyString)
						val paperJSONOpt = Some(paperJSON)

						PapersCtrl.actionHandler(handlerOpt, paperJSONOpt)
					}
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop {
			channel.close()
			connection.close()
		}
		override def send(bytesOpt: Option[Array[Byte]]) {
			bytesOpt.foreach { bytes =>
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				channel.basicPublish("", QUEUE_NAME, null, bytes)
			}

			this.stop
		}
	}

	private class ExamHandlerMQ extends HandlerMQ {
		private val QUEUE_NAME = "ExamHandlerMQ"
		private var connection = factory.newConnection()
		private var channel = connection.createChannel()
		override def init {
			channel.queueDeclare(QUEUE_NAME, false, false, false, null)

			val consumer = new DefaultConsumer(channel) {
		    	override def handleDelivery(
		    		consumerTag: String, envelope: Envelope, 
		    		properties: AMQP.BasicProperties, body: Array[Byte]) {
		    		// message unpack via MsgPack
		    		val recvMsgPack = new MsgWrapper(None, None)
		    		recvMsgPack.fromMsgPack(body)

		    		val handlerOpt = recvMsgPack.handler
		    		recvMsgPack.msgbody.foreach { msgbodyString =>
		    			val examJSON = Json.parse(msgbodyString)
		    			val examJSONOpt = Some(examJSON)

		    			ExamsCtrl.actionHandler(handlerOpt, examJSONOpt)
		    		}
		    	}
		    }

		    channel.basicConsume(QUEUE_NAME, true, consumer)
		}
		override def stop {
			channel.close()
			connection.close()
		}
		override def send(bytesOpt: Option[Array[Byte]]) {
			bytesOpt.foreach { bytes =>
				channel.queueDeclare(QUEUE_NAME, false, false, false, null)
				channel.basicPublish("", QUEUE_NAME, null, bytes)
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
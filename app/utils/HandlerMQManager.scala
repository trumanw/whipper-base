package utils

import play.api._

import scala.collection.mutable.ListBuffer

object HandlerMQManager {

	private lazy val consumers = new ListBuffer[HandlerMQ]()

    def start = {
    	Logger.info("RabbitMQ consumers are starting...")

    	val questionConsumer = HandlerMQFactory(Option("whipper.questions.mq"))
    	questionConsumer.init
    	consumers += questionConsumer

    	val compositeConsumer = HandlerMQFactory(Option("whipper.composites.mq"))
    	compositeConsumer.init
    	consumers += compositeConsumer

    	val paperConsumer = HandlerMQFactory(Option("whipper.papers.mq"))
    	paperConsumer.init
    	consumers += paperConsumer

    	val examConsumer = HandlerMQFactory(Option("whipper.exams.mq"))
    	examConsumer.init
    	consumers += examConsumer

    	Logger.info("RabbitMQ consumers are running.")
    }

    def stop = {
    	Logger.info("RabbitMQ consumers are closing...")
		
		consumers.toList.foreach((consumer: HandlerMQ) => {
			consumer.stop
		})

		Logger.info("RabbitMQ consumers have been closed.")
    }
}

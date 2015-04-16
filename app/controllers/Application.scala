package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

import scala.io.Source
import utils._

import org.sedis._
import com.typesafe.plugin.RedisPlugin

import eu.henkelmann.actuarius.ActuariusTransformer

object Application extends Controller {

	def index = Action {
		
		val transformer = new ActuariusTransformer()
		val mainMDFile = Play.getFile("public/markdowns/main.md")
		val mainMDFileSource = Source.fromFile(mainMDFile)
		val mainMDFileContent = try mainMDFileSource.getLines().mkString("\n") finally mainMDFileSource.close()
		val mkParseOutput = transformer(mainMDFileContent)

		Ok(views.html.md(mkParseOutput))
	}

	def get = Action {
		var retOpt = None: Option[String]
		val pool = Play.application.plugin[RedisPlugin]
						.getOrElse(throw new RuntimeException("MyPlugin not loaded"))
						.sedisPool
		retOpt = pool.withJedisClient[Option[String]] { client =>
			Dress.up(client).get("test")
		}

		if (retOpt.isDefined) {
			Status(200)(retOpt.get)
		} else {
			InternalServerError
		}
	}

	def set(value: String) = Action {
		val pool = Play.application.plugin[RedisPlugin]
						.getOrElse(throw new RuntimeException("MyPlugin not loaded"))
						.sedisPool
		pool.withJedisClient { client =>
			Dress.up(client).set("test", value)
		}

		Status(200)
	}
}

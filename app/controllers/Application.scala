package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

import scala.io.Source
import utils._

import eu.henkelmann.actuarius.ActuariusTransformer

object Application extends Controller {

	def index = Action {
		val transformer = new ActuariusTransformer()
		val mainMDFile = Play.getFile("public/markdowns/main.md")
		val mainMDFileContent = Source.fromFile(mainMDFile).getLines().mkString("\n")
		val mkParseOutput = transformer(mainMDFileContent)
		Ok(views.html.md(mkParseOutput))
	}

}

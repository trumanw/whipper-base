package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import utils._

object LocationsCtrl extends Controller with LocationFormat {

	def provinces = Action {
		val provinces = LocationManager.provinces
		Ok(Json.toJson(provinces))
	}

}
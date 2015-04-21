package utils

import play.api.Play
import play.api.Play.current

import play.api.libs.json._

import scala.io.Source

import scala.collection.concurrent.TrieMap

case class Location(
	var id: Long, 
	var code: Long, 
	var name: String)

case class LocationView(
	var locations: Seq[Location],
	var count: Int) extends Serializable

trait LocationFormat {
	implicit val LocationFormat = Json.format[Location]
	implicit val LocationViewFormat = Json.format[LocationView]
}

object LocationManager extends LocationFormat { 
	private val locTrieMap = TrieMap[Long, Location]()
	private var provincesView: Option[LocationView] = None

	def locsMap = { locTrieMap }

	def init(filename: String) = {
		val filepath = "public/jsons/" + filename
		val file = Play.getFile(filepath)
		val fileSource = Source.fromFile(file)
		val fileContent = {
			try 
				fileSource.getLines().mkString("\n") 
			finally 
				fileSource.close()
		}

		val fileJSON = Json.parse(fileContent)
		val jsArrayOpt = fileJSON.asOpt[JsArray]
		scanAreaArray(jsArrayOpt, locTrieMap)

		// init provinces
		initProvinces
	}

	def reload(filename: String) = {
		init(filename)
	}

	def provinces: LocationView = {
		var retProvinces = LocationView(Seq(), 0)
		provincesView.foreach { provinces =>
			retProvinces = provinces
		}
		retProvinces
	}

	def initProvinces: LocationView = {
		val locations = locTrieMap.filterKeys(_ % 1000 == 0)
								.filterKeys(_ != 419000)
								.filterKeys(_ != 429000)
								.filterKeys(_ != 469000)
								.filterKeys(_ != 659000)
								.toList.sortBy(_._1).map(_._2)
		val size = locations.length
		val provinces = LocationView(locations.toSeq, size)
		provincesView = Option(provinces)
		provinces
	}

	private def scanAreaArray(
		areaJsArrayOpt: Option[JsArray], 
		areaTrieMap: TrieMap[Long, Location]): Unit = {
		areaJsArrayOpt.foreach { areaJsArray =>
			for (areaJsValue <- areaJsArray.value) {
				val childrenOpt = (areaJsValue \ "children").asOpt[JsArray]
				areaJsValue.validate[Location] match {
					case s: JsSuccess[Location] => {
						val areaItem = s.get
						areaTrieMap += ((areaItem.id, areaItem))
					}
					case e: JsError => {

					}
				}
				scanAreaArray(childrenOpt, areaTrieMap)
			}
		}
	}
}
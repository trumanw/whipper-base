package models

import play.api.Play.current
// redis cache plugin
import play.api.cache.Cache

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

import akka.actor.Actor

import utils._

object ExamResultStatus {
	def EXAM_NOT_FOUND = { Option(404) }
	def EXAM_PARAMS_NOT_FOUND = { Option(601) }
	def EXAM_OK = { Option(200) }
}

case class ExamPOJO(
	var name: Option[String] = None,
	var attrs: Option[String] = None,
	var struct: Option[String] = None,
	var pid: Option[Long] = None,
	var uppertime: Option[Long] = Option(0),
	var lowertime: Option[Long] = Option(0),
	var duration: Option[Long] = Option(0),
	var tombstone: Option[Int] = Option(0),
	var updtime: Option[Long] = Option(0),
	var inittime: Option[Long] = Option(0),
	var id: Option[Long] = None)

case class Exam(
	var name: Option[String] = None,
	var attrs: Option[List[AttrsElem]] = None,
	var struct: Option[List[StructElem]] = None,
	var pid: Option[Long] = None,
	var uppertime: Option[Long] = Option(0),
	var lowertime: Option[Long] = Option(0),
	var duration: Option[Long] = Option(0),
	var tombstone: Option[Int] = Option(0),
	var updtime: Option[Long] = Option(0),
	var inittime: Option[Long] = Option(0),
	var id: Option[Long] = None)

case class ExamResult(
	val status: Option[Int] = None,
	val exam: Option[Exam] = None)

case class ExamListResult(
	var status: Option[Int] = None,
	var exams: Option[List[Exam]],
	var count: Option[Int]) extends Serializable

class Exams(tag: Tag)
	extends Table[ExamPOJO](tag, "exam") {
	
	def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
	def attrs = column[Option[String]]("attrs")
	def name = column[Option[String]]("name")
	def struct = column[Option[String]]("struct")
	def pid = column[Option[Long]]("pid")
	def uppertime = column[Option[Long]]("uppertime")
	def lowertime = column[Option[Long]]("lowertime")
	def duration = column[Option[Long]]("duration")
	def tombstone = column[Option[Int]]("tombstone")
	def inittime = column[Option[Long]]("init_time")
	def updtime = column[Option[Long]]("update_time")

	def * = (name, attrs, struct, pid,
			uppertime, lowertime, duration, 
			tombstone, updtime, inittime, id) <> 
			(ExamPOJO.tupled, ExamPOJO.unapply _)
}

trait ExamJSONTrait extends PapersJSONTrait {
	implicit val ExamPOJOJSONFormat = Json.format[ExamPOJO]
	implicit val ExamJSONFormat = Json.format[Exam]
	implicit val ExamResultFormat = Json.format[ExamResult]
	implicit val ExamListResultFormat = Json.format[ExamListResult]
}

object Exams extends ExamJSONTrait {
	val table = TableQuery[Exams]
	implicit lazy val examListCacheKey = "exam:list"

	private def getPOJOFromClass(exam: Exam): ExamPOJO = {
		var attrsJSONCmpString = None: Option[String]
		var structJSONCmpString = None: Option[String]
		var updtime = Option(System.currentTimeMillis()/1000L)
		var inittime = Option(System.currentTimeMillis()/1000L)
		var tombstone = Option(0)

		if (exam.attrs.isDefined) {
			attrsJSONCmpString = Snoopy.comp(
				Option(Json.toJson(exam.attrs.get).toString))
		}

		if (exam.struct.isDefined) {
			structJSONCmpString = Snoopy.comp(
				Option(Json.toJson(exam.struct.get).toString))
		}

		if (exam.updtime.isDefined) {
			updtime = exam.updtime
		}

		if (exam.inittime.isDefined) {
			inittime = exam.inittime
		}

		if (exam.tombstone.isDefined) {
			tombstone = exam.tombstone
		}

		val examPOJO = ExamPOJO(
			exam.name,
			attrsJSONCmpString,
			structJSONCmpString,
			exam.pid,
			exam.uppertime,
			exam.lowertime,
			exam.duration,
			tombstone,
			updtime,
			inittime,
			exam.id)
		examPOJO
	}

	private def getClassFromPOJO(
		ePOJO: ExamPOJO): Exam = {
		var attrsListOpt = None: Option[List[AttrsElem]]
		if (ePOJO.attrs.isDefined) {
			val attrsListJSON = Json.parse(Snoopy.decomp(ePOJO.attrs).get)
			attrsListJSON.validate[List[AttrsElem]] match {
				case s: JsSuccess[List[AttrsElem]] => {
					attrsListOpt = Option(s.get)
				}
				case e: JsError => {
					// error handling flow
				}
			}
		}

		var structListOpt = None: Option[List[StructElem]]
		if (ePOJO.struct.isDefined) {
			val structListJSON = Json.parse(Snoopy.decomp(ePOJO.struct).get)
			structListJSON.validate[List[StructElem]] match {
				case s: JsSuccess[List[StructElem]] => {
					structListOpt = Option(s.get)
				}
				case e: JsError => {
					// error handling flow
				}
			}
		}

		val exam = Exam(
				ePOJO.name,
				attrsListOpt,
				structListOpt,
				ePOJO.pid,
				ePOJO.uppertime,
				ePOJO.lowertime,
				ePOJO.duration,
				ePOJO.tombstone,
				ePOJO.updtime,
				ePOJO.inittime,
				ePOJO.id)
		exam
	}

	def retrieve(id: Long)
		(implicit session: Session): Option[ExamResult] = {
		var retOpt = None: Option[ExamResult]

		val ePOJOOpt = table.filter(_.id === id)
							.filter(_.tombstone === 0)
							.take(1).firstOption
		if (ePOJOOpt.isDefined) {
			val ePOJO = ePOJOOpt.get

			val examResult = ExamResult(
					ExamResultStatus.EXAM_OK,
					Option(getClassFromPOJO(ePOJO)))
			retOpt = Option(examResult)
		} else {
			// exam doesn't exist
			val examResult = ExamResult(
					ExamResultStatus.EXAM_NOT_FOUND,
					None)
			retOpt = Option(examResult)
		}

		retOpt
	}

	def query(page: Int, size: Int)
		(implicit session: Session): Option[ExamListResult] = {
		var retOpt = None: Option[ExamListResult]

		// get exam list from cache
		val examListResultOptCached = Cache.getAs[ExamListResult](examListCacheKey)
		if (examListResultOptCached.isDefined) {
			val examListResult = examListResultOptCached.get
			val examListOpt = examListResult.exams
			if (examListOpt.isDefined) {
				val examList = examListOpt.get
				val count = examList.size
				val examListWithPaging = examList
							.drop(page * size)
							.take(size)
				retOpt = Option(ExamListResult(
						ExamResultStatus.EXAM_OK,
						Option(examListWithPaging),
						Option(count)))

			}
		}

		if (!retOpt.isDefined) {
			val count = table.filter(_.tombstone === 0)
							.length.run
			val queryExamPOJOList = table.filter(_.tombstone === 0)
										.sortBy(_.updtime.desc)
										.list
			val examListResultCached = ExamListResult(
						ExamResultStatus.EXAM_OK,
						None,
						Option(count))
			var queryExamResultList = List[Exam]()
			if (count > 1000) {
				// size is large than 1000, use parallel collection
				queryExamResultList = queryExamPOJOList.par.map(
							(ePOJO) => getClassFromPOJO(ePOJO)).toList
			} else {
				queryExamResultList = queryExamPOJOList.map(
							(ePOJO) => getClassFromPOJO(ePOJO))
			}
			examListResultCached.exams = Option(queryExamResultList)
			Cache.set(examListCacheKey, examListResultCached)

			// return exam list result with paging
			val examListWithPaging = queryExamResultList
						.drop(page * size)
						.take(size)
			val examListResult = ExamListResult(
						ExamResultStatus.EXAM_OK,
						Option(examListWithPaging),
						Option(count))
			retOpt = Option(examListResult)
		}

		retOpt
	}

	def revoke(exam: Exam)
		(implicit session: Session): Option[ExamResult] = {
		var retOpt = None: Option[ExamResult]
		val updtime = Option(System.currentTimeMillis()/1000L)

		if (exam.id.isDefined) {
			val id = exam.id.get
			val queryPOJOOpt = table.filter(_.id === id)
									.filter(_.tombstone === 0)
									.take(1).firstOption
			if (queryPOJOOpt.isDefined) {
				val queryPOJO = queryPOJOOpt.get
				table.filter(_.id === id)
					.map(row => (row.tombstone, row.updtime))
					.update((Option(1), updtime))
				queryPOJO.tombstone = Option(1)
				queryPOJO.updtime = updtime

				val examResult = ExamResult(
						ExamResultStatus.EXAM_OK,
						Option(getClassFromPOJO(queryPOJO)))
				retOpt = Option(examResult)

				// remove cached
				Cache.remove(examListCacheKey)
			} else {
				val examResult = ExamResult(
						ExamResultStatus.EXAM_NOT_FOUND,
						None)
				retOpt = Option(examResult)
			}
		} else {
			val examResult = ExamResult(
					ExamResultStatus.EXAM_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(examResult)
		}

		retOpt
	}

	def publish(exam: Exam)
		(implicit session: Session): Option[ExamResult] = {
		var retOpt = None: Option[ExamResult]

		val ePOJO = getPOJOFromClass(exam)
		// allocate a new exam
		val id = (table returning table.map(_.id)) += ePOJO
		exam.id = id

		// wrap the retrieved result
		val examResult = ExamResult(
				ExamResultStatus.EXAM_OK,
				Option(exam))
		retOpt = Option(examResult)

		// remove cached
		Cache.remove(examListCacheKey)

		retOpt
	}

}

import models.ExamsActor.{ExamQuery, ExamRetrieve, ExamPublish, ExamRevoke}

object ExamsActor {
	case class ExamRetrieve(id: Long)
	case class ExamQuery(page: Int, size: Int)
	case class ExamPublish(exam: Exam)
	case class ExamRevoke(exam: Exam)	
}

class ExamsActor extends Actor {
	def receive: Receive = {
		case ExamRetrieve(id: Long) => {
			DB.withTransaction { implicit session =>
				sender ! Exams.retrieve(id)
			}
		}
		case ExamQuery(page: Int, size: Int) => {
			DB.withTransaction { implicit session =>
				sender ! Exams.query(page, size)
			}
		}
		case ExamPublish(exam: Exam) => {
			DB.withTransaction { implicit session =>
				sender ! Exams.publish(exam)
			}
		}
		case ExamRevoke(exam: Exam) => {
			DB.withTransaction { implicit session =>
				sender ! Exams.revoke(exam)
			}
		}
	}
}
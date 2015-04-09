package models

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

// Akka imports
import akka.actor.Actor

import scala.collection.mutable.ListBuffer

import utils._
import globals._

object PaperStatus {
	val EDITING = 0
	val PREPARING = 1
	val REVIEWING = 2
	val PUBLIC = 3
}

object PaperResultStatus {
	def PAPER_NOT_FOUND = { Option(404) }
	def PAPER_PARAMS_NOT_FOUND = { Option(601) }
	def PAPER_OK = { Option(200) }
}

case class PaperPOJO(
	var name: Option[String] = None,
	var attrs: Option[String] = None,
	var struct: Option[String] = None,
	var status: Option[Int] = Option(PaperStatus.EDITING),
	var tombstone: Option[Int] = Option(0),
	var updtime: Option[Long] = Option(0),
	var inittime: Option[Long] = Option(0),
	var id: Option[Long] = None)

case class Paper(
	var name: Option[String] = None,
	var attrs: Option[List[AttrsElem]] = None,
	var struct: Option[List[StructElem]] = None,
	var status: Option[Int] = Option(PaperStatus.EDITING),
	var tombstone: Option[Int] = Option(0),
	var updtime: Option[Long] = Option(0),
	var inittime: Option[Long] = Option(0),
	var id: Option[Long] = None)

case class PaperResult(
	val status: Option[Int] = None,
	val paper: Option[Paper] = None)

case class PaperListResult(
	val status: Option[Int] = None,
	val papers: Option[List[Paper]],
	val count: Option[Int])

class Papers(tag: Tag)
	extends Table[PaperPOJO](tag, "paper") {
	def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
	def name = column[Option[String]]("name")
	def attrs = column[Option[String]]("attrs")
	def struct = column[Option[String]]("struct")
	def status = column[Option[Int]]("review_status")
	def tombstone = column[Option[Int]]("tombstone")
	def updtime = column[Option[Long]]("update_time")
	def inittime = column[Option[Long]]("init_time")

	def * = (name, attrs, struct, status, 
			tombstone, updtime, inittime, id) <> 
			(PaperPOJO.tupled, PaperPOJO.unapply _)
}

trait PapersJSONTrait 
	extends AttrsElemJSONTrait 
	with StructElemJSONTrait {
	// JSON default formats
	implicit val PaperPOJOFormat = Json.format[PaperPOJO]
	implicit val PaperFormat = Json.format[Paper]
	implicit val PaperResultFormat = Json.format[PaperResult]
	implicit val PaperListResultFormat = Json.format[PaperListResult]
}

object Papers extends PapersJSONTrait {
	// ORM table of Paper
	val table = TableQuery[Papers]

	private def getPOJOFromClass(
		paper: Paper): PaperPOJO = {
		var attrsJSONCmpString = None: Option[String]
		var structJSONCmpString = None: Option[String]
		var updtime = Option(System.currentTimeMillis()/1000L)
		var inittime = Option(System.currentTimeMillis()/1000L)
		var tombstone = Option(0)

		if (paper.attrs.isDefined) {
			attrsJSONCmpString = Snoopy.comp(
				Option(Json.toJson(paper.attrs.get).toString))
		}

		if (paper.struct.isDefined) {
			structJSONCmpString = Snoopy.comp(
				Option(Json.toJson(paper.struct.get).toString))
		}

		if (paper.updtime.isDefined) {
			updtime = paper.updtime
		}

		if (paper.inittime.isDefined) {
			inittime = paper.inittime
		}

		if (paper.tombstone.isDefined) {
			tombstone = paper.tombstone
		}

		val paperPOJO = PaperPOJO(
			paper.name,
			attrsJSONCmpString,
			structJSONCmpString,
			paper.status,
			tombstone,
			updtime,
			inittime,
			paper.id)
		paperPOJO
	}

	private def getClassFromPOJO(
		pPOJO: PaperPOJO): Paper = {
		var attrsListOpt = None: Option[List[AttrsElem]]
		if (pPOJO.attrs.isDefined) {
			val attrsListJSON = Json.parse(Snoopy.decomp(pPOJO.attrs).get)
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
		if (pPOJO.struct.isDefined) {
			val structListJSON = Json.parse(Snoopy.decomp(pPOJO.struct).get)
			structListJSON.validate[List[StructElem]] match {
				case s: JsSuccess[List[StructElem]] => {
					structListOpt = Option(s.get)
				}
				case e: JsError => {
					// error handling flow
				}
			}
		}

		val paper = Paper(
				pPOJO.name,
				attrsListOpt,
				structListOpt,
				pPOJO.status,
				pPOJO.tombstone,
				pPOJO.updtime,
				pPOJO.inittime,
				pPOJO.id)
		paper
	}

	private def duplicateIfNotNone(
		srcPOJO: PaperPOJO, dstPOJO: PaperPOJO) = {
		if (srcPOJO.name.isDefined) {
			dstPOJO.name = srcPOJO.name
		}
		if (srcPOJO.attrs.isDefined) {
			dstPOJO.attrs = srcPOJO.attrs
		}
		if (srcPOJO.struct.isDefined) {
			dstPOJO.struct = srcPOJO.struct
		}
	}

	def add(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]

		val pPOJO = getPOJOFromClass(paper)
		// allocate a new paper
		val id = (table returning table.map(_.id)) += pPOJO
		paper.id = id

		// wrap the retrieved result
		val paperResult = PaperResult(
			PaperResultStatus.PAPER_OK,
			Option(paper))
		retOpt = Option(paperResult)

		retOpt
	}

	def retrieve(id: Long)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]

		val pPOJOOpt = table.filter(_.id === id)
							.filter(_.tombstone === 0)
							.take(1).firstOption
		if (pPOJOOpt.isDefined) {
			val pPOJO = pPOJOOpt.get

			val paperResult = PaperResult(
					PaperResultStatus.PAPER_OK,
					Option(getClassFromPOJO(pPOJO)))
			retOpt = Option(paperResult)
		} else {
			// paper doesn't exist
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_NOT_FOUND,
					None)
			retOpt = Option(paperResult)
		}

		retOpt
	}

	def update(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
		val updtime = Option(System.currentTimeMillis()/1000L)

		if (paper.id.isDefined) {
			val id = paper.id.get
			val queryPOJOOpt = table.filter(_.id === id)
									.filter(_.tombstone === 0)
									.take(1).firstOption
			if (queryPOJOOpt.isDefined) {
				val queryPOJO = queryPOJOOpt.get
				val updPOJO = getPOJOFromClass(paper)
				updPOJO.updtime = updtime
				updPOJO.inittime = queryPOJO.inittime

				// update the queryPOJO with updPOJO
				duplicateIfNotNone(updPOJO, queryPOJO)

				table.filter(_.id === queryPOJO.id.get)
					.filter(_.tombstone === 0)
					.map(row => (row.name, row.attrs, row.struct, row.updtime))
					.update((queryPOJO.name, queryPOJO.attrs, 
							queryPOJO.struct, updtime))

				val paperResult = PaperResult(
						PaperResultStatus.PAPER_OK,
						Option(getClassFromPOJO(queryPOJO)))
				retOpt = Option(paperResult)
			} else {
				// paper doesn't exist
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)				
			}
		} else {
			// invalid id
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)
		}

		retOpt
	}

	def delete(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
		val updtime = Option(System.currentTimeMillis()/1000L)

		if (paper.id.isDefined) {
			val id = paper.id.get
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

				// OK
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_OK,
						Option(getClassFromPOJO(queryPOJO)))
				retOpt = Option(paperResult)
			} else {
				// paper doesn't exists
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)
			}
		} else {
			// invalid id
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)				
		}

		retOpt
	}

	def attrsAppend(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (paper.attrs.isDefined && paper.id.isDefined) {
        	val id = paper.id.get
        	val attrsListUpdated = paper.attrs.get
        	// query paper from db
        	val queryPOJOOpt = table.filter(_.id === id)
        						.filter(_.tombstone === 0)
        						.take(1).firstOption
        	if (queryPOJOOpt.isDefined) {
    			val queryPOJO = queryPOJOOpt.get
    			val queryPaper = getClassFromPOJO(queryPOJO)

    			var attrsListUpdatedFinal = None: Option[List[AttrsElem]]
    			if (queryPaper.attrs.isDefined) {
    				val queryPaperAttrsList = queryPaper.attrs.get
    				attrsListUpdatedFinal = AttrsElem.attrsListUnion(
    						queryPaperAttrsList, attrsListUpdated)
				} else {
					val queryPaperAttrsList = List[AttrsElem]()
					attrsListUpdatedFinal = AttrsElem.attrsListUnion(
							queryPaperAttrsList, attrsListUpdated)        					
				}

				// commit the update to database
				if (attrsListUpdatedFinal.isDefined) {
					val attrsJSONUpdateCmpString = Snoopy.comp(
						Option(Json.toJson(attrsListUpdatedFinal.get).toString))
					queryPOJO.attrs = attrsJSONUpdateCmpString
					queryPOJO.updtime = updtime

					table.filter(_.id === id)
    					.filter(_.tombstone === 0)
    					.map(row => (row.attrs, row.updtime))
    					.update((queryPOJO.attrs, queryPOJO.updtime))

    				val paperResult = PaperResult(
    						PaperResultStatus.PAPER_OK,
    						Option(getClassFromPOJO(queryPOJO)))
    				retOpt = Option(paperResult)
				}
    		} else {
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)       			
    		}
    	} else {
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)      		
    	}

        retOpt
	}

	def attrsRemove(paper: Paper)
        (implicit session: Session): Option[PaperResult] = {
        var retOpt = None: Option[PaperResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (paper.attrs.isDefined && paper.id.isDefined) {
        	val id = paper.id.get
        	val attrsListRemoved = paper.attrs.get
        	val queryPOJOOpt = table.filter(_.id === id)
        						.filter(_.tombstone === 0)
        						.take(1).firstOption
        	if (queryPOJOOpt.isDefined) {
    			val queryPOJO = queryPOJOOpt.get
    			val queryPaper = getClassFromPOJO(queryPOJO)

    			var attrsListUpdatedFinal = None: Option[List[AttrsElem]]
    			if (queryPaper.attrs.isDefined) {
    				val queryPaperAttrsList = queryPaper.attrs.get
    				attrsListUpdatedFinal = AttrsElem.attrsListDiff(
    						queryPaperAttrsList, attrsListRemoved)
    			}

    			// commit the update to database
				if (attrsListUpdatedFinal.isDefined) {
					val attrsJSONUpdateCmpString = Snoopy.comp(
						Option(Json.toJson(attrsListUpdatedFinal.get).toString))
					queryPOJO.attrs = attrsJSONUpdateCmpString
					queryPOJO.updtime = updtime

					table.filter(_.id === id)
    					.filter(_.tombstone === 0)
    					.map(row => (row.attrs, row.updtime))
    					.update((queryPOJO.attrs, queryPOJO.updtime))

    				val paperResult = PaperResult(
    						PaperResultStatus.PAPER_OK,
    						Option(getClassFromPOJO(queryPOJO)))
    				retOpt = Option(paperResult)
				}
    		} else {
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)         			
    		}
    	} else {
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)        		
    	}
        retOpt
    }

    def attrsClean(paper: Paper)
    	(implicit session: Session): Option[PaperResult] = {
    	var retOpt = None: Option[PaperResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (paper.id.isDefined) {
        	val id = paper.id.get
        	val queryPOJOOpt = table.filter(_.id === id)
        						.filter(_.tombstone === 0)
        						.take(1).firstOption
        	if (queryPOJOOpt.isDefined) {
    			val queryPOJO = queryPOJOOpt.get
    			val attrsJSONCleanCmpString = Snoopy.comp(
    					Option(JsArray(Seq()).toString))
    			table.filter(_.id === id)
    				.filter(_.tombstone === 0)
    				.map(row => (row.attrs, row.updtime))
    				.update((attrsJSONCleanCmpString, updtime))

    			queryPOJO.attrs = attrsJSONCleanCmpString
    			queryPOJO.updtime = updtime
    			val paperResult = PaperResult(
						PaperResultStatus.PAPER_OK,
						Option(getClassFromPOJO(queryPOJO)))
				retOpt = Option(paperResult)
    		} else {
    			val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)
    		}
    	} else {
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)        		
    	}

        retOpt
	}

	def structAppend(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (paper.struct.isDefined && paper.id.isDefined) {
        	val id = paper.id.get
        	val structUpdatedList = paper.struct.get

        	val queryPOJOOpt = table.filter(_.id === id)
        							.filter(_.tombstone === 0)
        							.take(1).firstOption
        	if (queryPOJOOpt.isDefined) {
        		val queryPOJO = queryPOJOOpt.get
    			val queryPaper = getClassFromPOJO(queryPOJO)

    			var structListUpdatedFinal = None: Option[List[StructElem]]
    			if (queryPaper.struct.isDefined) {
    				val queryPaperStructList = queryPaper.struct.get
    				structListUpdatedFinal = StructElem.structListUnion(
    						queryPaperStructList, structUpdatedList)
				} else {
					val queryPaperStructList = List[StructElem]()
					structListUpdatedFinal = StructElem.structListUnion(
							queryPaperStructList, structUpdatedList)        					
				}

				// commit the update to database
				if (structListUpdatedFinal.isDefined) {
					val structJSONUpdateCmpString = Snoopy.comp(
						Option(Json.toJson(structListUpdatedFinal.get).toString))
					queryPOJO.struct = structJSONUpdateCmpString
					queryPOJO.updtime = updtime

					table.filter(_.id === id)
    					.filter(_.tombstone === 0)
    					.map(row => (row.struct, row.updtime))
    					.update((queryPOJO.struct, queryPOJO.updtime))

    				val paperResult = PaperResult(
    						PaperResultStatus.PAPER_OK,
    						Option(getClassFromPOJO(queryPOJO)))
    				retOpt = Option(paperResult)
				}
    		} else {
				val paperResult = PaperResult(
					PaperResultStatus.PAPER_NOT_FOUND,
					None)
				retOpt = Option(paperResult)        			
    		}
    	} else {
    		val paperResult = PaperResult(
    			PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
    			None)
    		retOpt = Option(paperResult)
    	}

        retOpt
	}

	def structRemove(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (paper.struct.isDefined && paper.id.isDefined) {
        	val id = paper.id.get
        	val structRemovedList = paper.struct.get

        	val queryPOJOOpt = table.filter(_.id === id)
        							.filter(_.tombstone === 0)
        							.take(1).firstOption
        	if (queryPOJOOpt.isDefined) {
        		val queryPOJO = queryPOJOOpt.get
    			val queryPaper = getClassFromPOJO(queryPOJO)

    			var structListUpdatedFinal = None: Option[List[StructElem]]
    			if (queryPOJO.struct.isDefined) {
    				val queryPaperStructList = queryPaper.struct.get
    				structListUpdatedFinal = StructElem.structListDiff(
    						queryPaperStructList, structRemovedList)
    			}

    			// commit the update to database
				if (structListUpdatedFinal.isDefined) {
					val structJSONUpdateCmpString = Snoopy.comp(
						Option(Json.toJson(structListUpdatedFinal.get).toString))
					queryPOJO.struct = structJSONUpdateCmpString
					queryPOJO.updtime = updtime

					table.filter(_.id === id)
    					.filter(_.tombstone === 0)
    					.map(row => (row.struct, row.updtime))
    					.update((queryPOJO.struct, queryPOJO.updtime))

    				val paperResult = PaperResult(
    						PaperResultStatus.PAPER_OK,
    						Option(getClassFromPOJO(queryPOJO)))
    				retOpt = Option(paperResult)
				}
    		} else {
				val paperResult = PaperResult(
					PaperResultStatus.PAPER_NOT_FOUND,
					None)
				retOpt = Option(paperResult)        			
    		}
    	} else {
    		val paperResult = PaperResult(
    			PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
    			None)
    		retOpt = Option(paperResult)
    	}

        retOpt
	}

	def structClean(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (paper.struct.isDefined && paper.id.isDefined) {
        	val id = paper.id.get

        	val queryPOJOOpt = table.filter(_.id === id)
        							.filter(_.tombstone === 0)
        							.take(1).firstOption
        	if (queryPOJOOpt.isDefined) {
        		val queryPOJO = queryPOJOOpt.get
    			val structJSONCleanCmpString = Snoopy.comp(
                        Option(JsArray(Seq()).toString))

    			table.filter(_.id === id)
    				.filter(_.tombstone === 0)
    				.map(row => (row.struct, row.updtime))
    				.update((structJSONCleanCmpString, updtime))

    			queryPOJO.struct = structJSONCleanCmpString
    			queryPOJO.updtime = updtime
    			val paperResult = PaperResult(
    					PaperResultStatus.PAPER_OK,
    					Option(getClassFromPOJO(queryPOJO)))
    			retOpt = Option(paperResult)
    		} else {
				val paperResult = PaperResult(
					PaperResultStatus.PAPER_NOT_FOUND,
					None)
				retOpt = Option(paperResult)        			
    		}
    	} else {
    		val paperResult = PaperResult(
    			PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
    			None)
    		retOpt = Option(paperResult)
    	}

        retOpt			
	}

	def statusProcess(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
		val updtime = Option(System.currentTimeMillis()/1000L)

		if (paper.id.isDefined) {
			val id = paper.id.get
			val queryPOJOOpt = table.filter(_.id === id)
									.filter(_.tombstone === 0)
									.take(1).firstOption
			if (queryPOJOOpt.isDefined) {
				val queryPOJO = queryPOJOOpt.get
				var processStatus = queryPOJO.status
				processStatus match {
					case Some(PaperStatus.EDITING) => {
						processStatus = Option(PaperStatus.PREPARING)
					}
					case Some(PaperStatus.PREPARING) => {
						processStatus = Option(PaperStatus.REVIEWING)
					}
					case Some(PaperStatus.REVIEWING) => {
						processStatus = Option(PaperStatus.PUBLIC)
					}
					case Some(PaperStatus.PUBLIC) => {
						// The current status cannot be processed
					}
					case _ => ;
				}
				table.filter(_.id === id)
					.filter(_.tombstone === 0)
					.map(row => (row.status, row.updtime))
					.update((processStatus, updtime))
				// wrap the retrieved result
				queryPOJO.status = processStatus
				queryPOJO.updtime = updtime

				val paperResult = PaperResult(
					PaperResultStatus.PAPER_OK,
					Option(getClassFromPOJO(queryPOJO)))
				retOpt = Option(paperResult)
			} else {
				// paper doesn't exists
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)			
			}
		} else {
			// invalid id
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)					
		}
        retOpt			
	}

	def statusReset(paper: Paper)
		(implicit session: Session): Option[PaperResult] = {
		var retOpt = None: Option[PaperResult]
		val updtime = Option(System.currentTimeMillis()/1000L)

		if (paper.id.isDefined) {
			val id = paper.id.get
			val queryPOJOOpt = table.filter(_.id === id)
									.filter(_.tombstone === 0)
									.take(1).firstOption
			if (queryPOJOOpt.isDefined) {
				val queryPOJO = queryPOJOOpt.get

				val processStatus = Option(PaperStatus.EDITING)
				table.filter(_.id === id)
					.filter(_.tombstone === 0)
					.map(row => (row.status, row.updtime))
					.update((processStatus, updtime))
				// wrap the retrieved result
				queryPOJO.status = processStatus
				queryPOJO.updtime = updtime

				val paperResult = PaperResult(
					PaperResultStatus.PAPER_OK,
					Option(getClassFromPOJO(queryPOJO)))
				retOpt = Option(paperResult)
			} else {
				// paper doesn't exists
				val paperResult = PaperResult(
						PaperResultStatus.PAPER_NOT_FOUND,
						None)
				retOpt = Option(paperResult)			
			}
		} else {
			// invalid id
			val paperResult = PaperResult(
					PaperResultStatus.PAPER_PARAMS_NOT_FOUND,
					None)
			retOpt = Option(paperResult)					
		}
        retOpt		
	}

	def query(page: Int, size: Int)
		(implicit session: Session): Option[PaperListResult] = {
		var retOpt = None: Option[PaperListResult]

		val count = table.filter(_.tombstone === 0)
						.length.run
		val queryPaperPOJOList = table.filter(_.tombstone === 0)
									.sortBy(_.id.asc.nullsFirst)
									.drop(page * size).take(size).list
		val queryPaperResultList = queryPaperPOJOList.map(
									(pPOJO) => getClassFromPOJO(pPOJO))
		val paperListResult = PaperListResult(
				PaperResultStatus.PAPER_OK,
				Option(queryPaperResultList),
				Option(count))
		retOpt = Option(paperListResult)
		retOpt
	}
	
}

import models.PapersActor.{PaperQuery, PaperRetrieve, PaperAdd, PaperDelete, PaperUpdate}
import models.PapersActor.{PaperStructAppend, PaperStructRemove, PaperStructClean}
import models.PapersActor.{PaperAttrsAppend, PaperAttrsRemove, PaperAttrsClean}
import models.PapersActor.{PaperStatusProcess, PaperStatusReset}

object PapersActor {
	case class PaperQuery(page: Int, size: Int)
	case class PaperRetrieve(id: Long)
	case class PaperAdd(paper: Paper)
	case class PaperDelete(paper: Paper)
	case class PaperUpdate(paper: Paper)

	case class PaperStructAppend(paper: Paper)
	case class PaperStructRemove(paper: Paper)
	case class PaperStructClean(paper: Paper)

	case class PaperAttrsAppend(paper: Paper)
	case class PaperAttrsRemove(paper: Paper)
	case class PaperAttrsClean(paper: Paper)

	case class PaperStatusProcess(paper: Paper)
	case class PaperStatusReset(paper: Paper)
}

class PapersActor extends Actor {
	def receive: Receive = {
		case PaperQuery(page: Int, size: Int) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.query(page, size)
			}
		}
		case PaperRetrieve(id: Long) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.retrieve(id)
			}
		}
		case PaperAdd(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.add(paper)
			}
		}
		case PaperDelete(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.delete(paper)
			}
		}
		case PaperUpdate(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.update(paper)
			}
		}
		case PaperStructAppend(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.structAppend(paper)
			}
		}
		case PaperStructRemove(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.structRemove(paper)
			}
		}
		case PaperStructClean(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.structClean(paper)
			}
		}
		case PaperAttrsAppend(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.attrsAppend(paper)
			}
		}
		case PaperAttrsRemove(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.attrsRemove(paper)
			}
		}
		case PaperAttrsClean(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.attrsClean(paper)
			}
		}
		case PaperStatusProcess(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.statusProcess(paper)
			}
		}
		case PaperStatusReset(paper: Paper) => {
			DB.withTransaction { implicit session =>
				sender ! Papers.statusReset(paper)
			}
		}
	}
}
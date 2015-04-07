package models

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.language.postfixOps

import akka.actor.Actor

import scala.collection.mutable.ListBuffer

import utils._
import globals._


// tag :
// - Some("Question") : the elem represents a Question object
// - Some("Composite") : the elem represents a Composite object
case class CompositeStructElem(
    var id: Option[Long] = None,
    var tag: Option[String] = None,
    var index: Option[Int] = None)

case class CompositePOJO(
    var content: Option[String] = None,
    var struct: Option[String] = None,
    var tombstone: Option[Int] = Option(0),
    var updtime: Option[Long] = Option(0),
    var inittime: Option[Long] = Option(0),
    var id: Option[Long] = None) 

case class Composite (
    var content: Option[String] = None,
    var struct: Option[List[CompositeStructElem]] = None,
    var tombstone: Option[Int] = Option(0),
    var updtime: Option[Long] = Option(0),
    var inittime: Option[Long] = Option(0),
    var id: Option[Long] = None)

object CompositeResultStatus {
    def COMPOSITE_NOT_FOUND = { Option(404) }
    def COMPOSITE_PARAMS_NOT_FOUND = { Option(601) }
    def COMPOSITE_OK = { Option(200) }
}

case class CompositeResult (
    val status: Option[Int] = None,
    val composite: Option[Composite] = None)

class Composites(tag: Tag) 
    extends Table[CompositePOJO](tag, "composite") {
    def id = column[Option[Long]]("id" , O.PrimaryKey, O.AutoInc)
    def content = column[Option[String]]("content")
    def struct = column[Option[String]]("struct")
    def tombstone = column[Option[Int]]("tombstone")
    def updtime = column[Option[Long]]("update_time")
    def inittime = column[Option[Long]]("init_time")

    def * = (content, struct, tombstone, 
            updtime, inittime, id) <> 
            (CompositePOJO.tupled, CompositePOJO.unapply _)
}

trait CompositesJSONTrait {
    // JSON default formats
    implicit val CompositeStructElemFormat = Json.format[CompositeStructElem]
    implicit val CompositePOJOFormat = Json.format[CompositePOJO]
    implicit val CompositeFormat = Json.format[Composite]
    implicit val CompositeResultFormat = Json.format[CompositeResult]
}

object Composites extends CompositesJSONTrait with QuestionsJSONTrait {
    // ORM table of Composite
    val table = TableQuery[Composites]

    private def getPOJOFromClass(
        composite: Composite): CompositePOJO = {
        var structJSONCmpString = None: Option[String]
        var updtime = Option(System.currentTimeMillis()/1000L)
        var inittime = Option(System.currentTimeMillis()/1000L)
        var tombstone = Option(0)

        if (composite.struct.isDefined) {
            structJSONCmpString = Snoopy.comp(
                Option(Json.toJson(composite.struct.get).toString))
        }

        if (composite.updtime.isDefined) {
            updtime = composite.updtime
        }

        if (composite.inittime.isDefined) {
            inittime = composite.inittime
        }

        if (composite.tombstone.isDefined) {
            tombstone = composite.tombstone
        }

        val compositePOJO = CompositePOJO(
            composite.content,
            structJSONCmpString,
            tombstone,
            updtime,
            inittime,
            composite.id)
        compositePOJO
    }

    private def getClassFromPOJO(
        cPOJO: CompositePOJO): Composite = {
        var structListOpt = None: Option[List[CompositeStructElem]]
        if (cPOJO.struct.isDefined) {
            val structListJSON = Json.parse(Snoopy.decomp(cPOJO.struct).get)
            structListJSON.validate[List[CompositeStructElem]] match {
                case s: JsSuccess[List[CompositeStructElem]] => {
                    structListOpt = Option(s.get)
                }
                case e: JsError => {
                    // error handling flow
                }
            }
        }

        val composite = Composite(
                cPOJO.content,
                structListOpt,
                cPOJO.tombstone,
                cPOJO.updtime,
                cPOJO.inittime,
                cPOJO.id)
        composite
    }

    private def duplicateIfNotNone(
        srcPOJO: CompositePOJO, dstPOJO: CompositePOJO) = {
        if (srcPOJO.content.isDefined) {
            dstPOJO.content = srcPOJO.content
        }
        if (srcPOJO.struct.isDefined) {
            dstPOJO.struct = srcPOJO.struct
        }
        if (srcPOJO.updtime.isDefined) {
            dstPOJO.updtime = srcPOJO.updtime
        }
    }

    private def structListDiff(
        srcStructList: List[CompositeStructElem],
        rmvStructList: List[CompositeStructElem]): Option[List[CompositeStructElem]] = {
        var dstStructListOpt = None: Option[List[CompositeStructElem]]

        val structDiffListBuffer = new ListBuffer[CompositeStructElem]()
        val structDiffIDListBuffer = new ListBuffer[Long]()

        // add all id of srcStructList to structDiffIDListBuffer
        for (structElem <- srcStructList) {
            structDiffIDListBuffer += structElem.id.get
        }
        structDiffListBuffer ++= srcStructList

        for (structElemRemoved <- rmvStructList) {
            val structElemRemovedIDOpt = structElemRemoved.id
            if (structElemRemovedIDOpt.isDefined) {
                structDiffIDListBuffer.indexOf(structElemRemovedIDOpt.get) match {
                    case -1 => {
                        // do nothing
                    }
                    case a: Int => {
                        structDiffListBuffer -= srcStructList(a)
                    }
                }
            }
        }
        dstStructListOpt = Option(structDiffListBuffer.toList)

        dstStructListOpt
    }

    private def structListUnion(
        srcStructList: List[CompositeStructElem],
        addStructList: List[CompositeStructElem]): Option[List[CompositeStructElem]] = {
        var dstStructListOpt = None: Option[List[CompositeStructElem]]

        val structUnionIDListBuffer = new ListBuffer[Long]()
        val dstStructListBuffer = new ListBuffer[CompositeStructElem]()

        // add all id of srcStructList to structUnionIDListBuffer
        for (structElem <- srcStructList) {
            structUnionIDListBuffer += structElem.id.get
        }
        dstStructListBuffer ++= srcStructList

        for (structElemUnion <- addStructList) {
            val structElemUnionIDOpt = structElemUnion.id
            if (structElemUnionIDOpt.isDefined) {
                structUnionIDListBuffer.indexOf(structElemUnionIDOpt.get) match {
                    case -1 => {
                        // append to dstStructListBuffer
                        structElemUnion.index = None
                        dstStructListBuffer += structElemUnion
                    }
                    case a: Int => {
                        // remove the original
                        dstStructListBuffer.remove(a)
                        
                        if (structElemUnion.index.isDefined) {
                            // add the new structElemUnion with its new index
                            val indexOfStructElemUion = structElemUnion.index.get
                            dstStructListBuffer.insert(indexOfStructElemUion, structElemUnion)
                        } else {
                            // otherwise, append it to the tail
                            structElemUnion.index = None
                            dstStructListBuffer += structElemUnion      
                        }
                    }
                }
            }
        }
        dstStructListOpt = Option(dstStructListBuffer.toList)

        dstStructListOpt
    } 

    def add(composite: Composite)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]

        val cPOJO = getPOJOFromClass(composite)
        // add a new composte and 
        // returned itself with allocated id
        val id = (table returning table.map(_.id)) += cPOJO
        composite.id = id

        // wrap the retrieved result
        val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_OK,
                Option(composite))
        retOpt = Option(compositeResult)

        retOpt          
    }

    def retrieve(id: Long)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]

        val cPOJOOpt = table.filter(_.id === id)
                        .filter(_.tombstone === 0)
                        .take(1).firstOption
        if (cPOJOOpt.isDefined) {
            val cPOJO = cPOJOOpt.get

            val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_OK,
                Option(getClassFromPOJO(cPOJO)))
            retOpt = Option(compositeResult)
        } else {
            // composite doesn't exist
            val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_NOT_FOUND,
                None)
            retOpt = Option(compositeResult)
        }

        retOpt  
    }

    def update(composite: Composite)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (composite.id.isDefined) {
            val id = composite.id.get
            val queryPOJOOpt = table.filter(_.id === id)
                                    .filter(_.tombstone === 0)
                                    .take(1).firstOption
            if (queryPOJOOpt.isDefined) {
                val queryPOJO = queryPOJOOpt.get
                val updPOJO = getPOJOFromClass(composite)
                updPOJO.updtime = updtime
                updPOJO.inittime = queryPOJO.inittime

                // update the queryPOJO with updPOJO
                duplicateIfNotNone(updPOJO, queryPOJO)

                // update the cPOJO with updPOJO
                table.filter(_.id === queryPOJO.id.get)
                    .filter(_.tombstone === 0)
                    .map(row => (row.content, row.struct, row.updtime))
                    .update((queryPOJO.content, queryPOJO.struct, queryPOJO.updtime))

                // OK
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_OK,
                    Option(getClassFromPOJO(queryPOJO)))
                retOpt = Option(compositeResult)
            } else {
                // composite doesn't exist
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_NOT_FOUND,
                    None)
                retOpt = Option(compositeResult)
            }
        }

        retOpt  
    }

    def delete(composite: Composite)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (composite.id.isDefined) {
            val id = composite.id.get
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
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_OK,
                    Option(getClassFromPOJO(queryPOJO)))
            } else {
                // composite doesn't exist
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_NOT_FOUND,
                    None)
                retOpt = Option(compositeResult)
            }
        } else {
            // invalid input params
            val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_PARAMS_NOT_FOUND,
                None)
            retOpt = Option(compositeResult)
        }

        retOpt  
    }

    def structAppend(composite: Composite)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (composite.struct.isDefined && composite.id.isDefined) {
            val id = composite.id.get
            val structUpdatedList = composite.struct.get

            val queryPOJOOpt = table.filter(_.id === id)
                                    .filter(_.tombstone === 0)
                                    .take(1).firstOption
            if (queryPOJOOpt.isDefined) {
                val queryPOJO = queryPOJOOpt.get
                val queryComposite = getClassFromPOJO(queryPOJO)

                var structListUpdatedFinal = None: Option[List[CompositeStructElem]]
                if (queryComposite.struct.isDefined) {
                    val queryCompositeStructList = queryComposite.struct.get
                    structListUpdatedFinal = structListUnion(
                            queryCompositeStructList, structUpdatedList)
                } else {
                    val queryCompositeStructList = List[CompositeStructElem]()
                    structListUpdatedFinal = structListUnion(
                            queryCompositeStructList, structUpdatedList)                
                }

                if (structListUpdatedFinal.isDefined) {
                    val structJSONUpdateCmpString = Snoopy.comp(
                        Option(Json.toJson(structListUpdatedFinal.get).toString))
                    queryPOJO.struct = structJSONUpdateCmpString
                    queryPOJO.updtime = updtime

                    table.filter(_.id === id)
                        .filter(_.tombstone === 0)
                        .map(row => (row.struct, row.updtime))
                        .update((queryPOJO.struct, queryPOJO.updtime))

                    val compositeResult = CompositeResult(
                            CompositeResultStatus.COMPOSITE_OK,
                            Option(getClassFromPOJO(queryPOJO)))
                    retOpt = Option(compositeResult)
                }
            } else {
                // composite doesn't exist
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_NOT_FOUND,
                    None)
                retOpt = Option(compositeResult)
            }
        } else {
            // invalid input params
            val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_PARAMS_NOT_FOUND,
                None)
            retOpt = Option(compositeResult)
        }

        retOpt
    }

    def structRemove(composite: Composite)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (composite.struct.isDefined && composite.id.isDefined) {
            val id = composite.id.get
            val structRemovedList = composite.struct.get

            val queryPOJOOpt = table.filter(_.id === id)
                                    .filter(_.tombstone === 0)
                                    .take(1).firstOption
            if (queryPOJOOpt.isDefined) {
                val queryPOJO = queryPOJOOpt.get
                val queryComposite = getClassFromPOJO(queryPOJO)

                var structListUpdatedFinal = None: Option[List[CompositeStructElem]]
                if (queryPOJO.struct.isDefined) {
                    val queryCompositeStructList = queryComposite.struct.get
                    structListUpdatedFinal = structListDiff(
                            queryCompositeStructList, structRemovedList)
                }

                if (structListUpdatedFinal.isDefined) {
                    val structJSONUpdateCmpString = Snoopy.comp(
                        Option(Json.toJson(structListUpdatedFinal.get).toString))
                    queryPOJO.struct = structJSONUpdateCmpString
                    queryPOJO.updtime = updtime

                    table.filter(_.id === id)
                        .filter(_.tombstone === 0)
                        .map(row => (row.struct, row.updtime))
                        .update((queryPOJO.struct, queryPOJO.updtime))

                    val compositeResult = CompositeResult(
                            CompositeResultStatus.COMPOSITE_OK,
                            Option(getClassFromPOJO(queryPOJO)))
                    retOpt = Option(compositeResult)
                }
            } else {
                // composite doesn't exist
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_NOT_FOUND,
                    None)
                retOpt = Option(compositeResult)
            }
        } else {
            // invalid input params
            val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_PARAMS_NOT_FOUND,
                None)
            retOpt = Option(compositeResult)
        }

        retOpt
    }

    def structClean(composite: Composite)
        (implicit session: Session): Option[CompositeResult] = {
        var retOpt = None: Option[CompositeResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (composite.struct.isDefined && composite.id.isDefined) {
            val id = composite.id.get

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
                val compositeResult = CompositeResult(
                        CompositeResultStatus.COMPOSITE_OK,
                        Option(getClassFromPOJO(queryPOJO)))
                retOpt = Option(compositeResult)
            } else {
                // composite doesn't exist
                val compositeResult = CompositeResult(
                    CompositeResultStatus.COMPOSITE_NOT_FOUND,
                    None)
                retOpt = Option(compositeResult)
            }
        } else {
            // invalid input params
            val compositeResult = CompositeResult(
                CompositeResultStatus.COMPOSITE_PARAMS_NOT_FOUND,
                None)
            retOpt = Option(compositeResult)
        }

        retOpt
    }

}

import models.CompositesActor.{CompositeRetrieve, CompositeAdd, CompositeUpdate, CompositeDelete}
import models.CompositesActor.{CompositeStructAppend, CompositeStructRemove, CompositeStructClean}

object CompositesActor {
    case class CompositeRetrieve(id: Long)
    case class CompositeAdd(composite: Composite)
    case class CompositeUpdate(composite: Composite)
    case class CompositeDelete(composite: Composite)
    case class CompositeStructAppend(composite: Composite)
    case class CompositeStructRemove(composite: Composite)
    case class CompositeStructClean(composite: Composite)
}

class CompositesActor extends Actor {
    def receive: Receive = {
        case CompositeRetrieve(id: Long) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.retrieve(id)
            }
        }
        case CompositeAdd(composite: Composite) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.add(composite)
            }
        }
        case CompositeUpdate(composite: Composite) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.update(composite)
            }
        }
        case CompositeDelete(composite: Composite) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.delete(composite)
            }
        }
        case CompositeStructAppend(composite: Composite) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.structAppend(composite)
            }
        }
        case CompositeStructRemove(composite: Composite) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.structRemove(composite)
            }
        }
        case CompositeStructClean(composite: Composite) => {
            DB.withTransaction { implicit session =>
                sender ! Composites.structClean(composite)
            }
        }
    }
}
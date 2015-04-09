package models

import net.liftweb.json._
import net.liftweb.json.Serialization.write
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

// Akka imports
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.{Props, ActorSystem}

trait StructElemJSONTrait {
	implicit val StructElemJSONFormat = Json.format[StructElem]
}

// tag :
// - Some("Question") : the elem represents a Question object
// - Some("Composite") : the elem represents a Composite object
case class StructElem(
	var id: Option[Long] = None,
    var tag: Option[String] = None,
    var index: Option[Int] = None)

case class StructElemEx(
	var id: Option[Long] = None,
	var tag: Option[String] = None,
	var index: Option[Int] = None,
	var sublist: Option[List[StructElemEx]] = None)

object StructElem {
	implicit val timeout = Timeout(5 seconds)
    implicit lazy val system = ActorSystem()
    implicit lazy val compositeActor = system.actorOf(Props[CompositesActor])

	def unfoldListIterate(structElemList: List[StructElem]): 
		Option[List[StructElemEx]] = {
		val retStructElemListBuffer = new ListBuffer[StructElemEx]()

		for (structElem <- structElemList) {
			structElem.tag match {
				case Some("Question") => {
					val questionStructElem = StructElemEx(
							structElem.id, structElem.tag,
							structElem.index, None)
					retStructElemListBuffer += questionStructElem
				}
				case Some("Composite") => {
					// retrieve composite struct
					if (structElem.id.isDefined) {
						val structElemId = structElem.id.get
						val futureComposite = compositeActor ? CompositesActor.CompositeRetrieve(structElemId)
						val resultComposite = Await.result(futureComposite, timeout.duration)
												.asInstanceOf[Option[CompositeResult]]
						if (resultComposite.isDefined) {
							resultComposite.get.status match {
								case Some(200) => {
									val composite = resultComposite.get.composite.get

									if (composite.struct.isDefined) {
										val structComposite = composite.struct.get
										val unfoldStructListCompositeOpt = unfoldListIterate(structComposite)
										if (unfoldStructListCompositeOpt.isDefined) {
											val unfoldStructListComposite = unfoldStructListCompositeOpt.get
											// iterate add unfold composite object into list buffer
											val compositeStructElem = StructElemEx(
													structElem.id, structElem.tag,
													structElem.index,
													unfoldStructListCompositeOpt
												)
											retStructElemListBuffer += compositeStructElem
										}
									}
								}
								case _ => {}
							}
						}
					}
					
				}
				case _ => {}
			}
		}

		val retStructElemList = retStructElemListBuffer.toList
		if (!retStructElemList.isEmpty) {
			Option(retStructElemList)
		} else {
			None
		}
	}
	def structListDiff(
        srcStructList: List[StructElem],
        rmvStructList: List[StructElem]): Option[List[StructElem]] = {
        var dstStructListOpt = None: Option[List[StructElem]]

        val structDiffListBuffer = new ListBuffer[StructElem]()
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

    def structListUnion(
        srcStructList: List[StructElem],
        addStructList: List[StructElem]): Option[List[StructElem]] = {
        var dstStructListOpt = None: Option[List[StructElem]]

        val structUnionIDListBuffer = new ListBuffer[Long]()
        val dstStructListBuffer = new ListBuffer[StructElem]()

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
}
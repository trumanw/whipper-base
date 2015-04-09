package models

import scala.collection.mutable.ListBuffer

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

trait AttrsElemJSONTrait {
	implicit val AttrsElemJSONFormat = Json.format[AttrsElem]
}

case class AttrsElem(
	var name: Option[String] = None,
	var value: Option[String] = None)

object AttrsElem {
	def attrsListDiff(
        srcAttrsList: List[AttrsElem],
        rmvAttrsList: List[AttrsElem]): Option[List[AttrsElem]] = {
        var dstAttrsListOpt = None: Option[List[AttrsElem]]

        val attrsDiffListBuffer = new ListBuffer[AttrsElem]()
        val attrsDiffNameListBuffer = new ListBuffer[String]()

        // add all names of srcAttrsList to attrsDiffNameListBuffer
        for (attrsElem <- srcAttrsList) {
            attrsDiffNameListBuffer += attrsElem.name.get
        }
        attrsDiffListBuffer ++= srcAttrsList

        for (attrsElemRemoved <- rmvAttrsList) {
            val attrsElemRemovedNameOpt = attrsElemRemoved.name
            if (attrsElemRemovedNameOpt.isDefined) {
                attrsDiffNameListBuffer.indexOf(attrsElemRemovedNameOpt.get) match {
                    case -1 => {
                        // do nothing
                    }
                    case a: Int => {
                        attrsDiffListBuffer -= srcAttrsList(a)
                    }
                }
            }
        }
        dstAttrsListOpt = Option(attrsDiffListBuffer.toList)

        dstAttrsListOpt
    }

    def attrsListUnion(
        srcAttrsList: List[AttrsElem], 
        addAttrsList: List[AttrsElem]): Option[List[AttrsElem]] = {
        var dstAttrsListOpt = None: Option[List[AttrsElem]]

        val attrsUnionListBuffer = new ListBuffer[AttrsElem]()
        val attrsUnionNameListBuffer = new ListBuffer[String]()

        // add all names os srcAttrsList to attrsUnionNameListBuffer
        for (attrsElem <- srcAttrsList) {
            attrsUnionNameListBuffer += attrsElem.name.get
        }
        attrsUnionListBuffer ++= srcAttrsList

        for (attrsElemUpdated <- addAttrsList) {
            val attrsElemUpdatedNameOpt = attrsElemUpdated.name
            val attrsElemUpdatedValueOpt = attrsElemUpdated.value
            if (attrsElemUpdatedNameOpt.isDefined) {
                attrsUnionNameListBuffer.indexOf(attrsElemUpdatedNameOpt.get) match {
                    case -1 => {
                        // add to the end of the attrs list buffer
                        attrsUnionListBuffer += attrsElemUpdated
                    }
                    case a: Int => {
                        // modify the value of the existed name in the list buffer
                        val attrsElemInListBuffer = attrsUnionListBuffer(a)
                        attrsElemInListBuffer.value = attrsElemUpdatedValueOpt
                    }
                }
            }
        }
        dstAttrsListOpt = Option(attrsUnionListBuffer.toList)
            
        dstAttrsListOpt
    }
}
package models

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.collection.mutable.ListBuffer

import akka.actor.Actor

import utils._

case class QuestionChoicesElem(
    var choice: Option[String] = None,
    var bingo: Option[Int] = None)

case class QuestionPOJO (
    var content: Option[String] = None,
    var choices: Option[String] = None,
    var attrs: Option[String] = None,
    var score: Option[Double] = Option(0),
    var tombstone: Option[Int] = Option(0),
    var updtime: Option[Long] = Option(0),
    var inittime: Option[Long] = Option(0),
    var id: Option[Long] = None)

case class Question (
    var content: Option[String] = None,
    var choices: Option[List[QuestionChoicesElem]] = None,
    var attrs: Option[List[AttrsElem]] = None,
    var score: Option[Double] = Option(0),
    var tombstone: Option[Int] = Option(0),
    var updtime: Option[Long] = Option(0),
    var inittime: Option[Long] = Option(0),
    var id: Option[Long] = None)

object QuestionResultStatus {
    def QUESTION_NOT_FOUND = { Option(404) }
    def QUESTION_PARAMS_NOT_FOUND = { Option(601) }
    def QUESTION_OK = { Option(200) }
}

case class QuestionResult (
    val status: Option[Int] = None,
    val question: Option[Question] = None)

class Questions(tag: Tag)
    extends Table[QuestionPOJO](tag, "question") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def content = column[Option[String]]("content")
    def choices = column[Option[String]]("choices")
    def attrs = column[Option[String]]("attrs")
    def score = column[Option[Double]]("score")
    def tombstone = column[Option[Int]]("tombstone")
    def updtime = column[Option[Long]]("update_time")
    def inittime = column[Option[Long]]("init_time")

    def * = (content, choices, attrs, score, 
            tombstone, updtime, inittime, id) <> 
            (QuestionPOJO.tupled, QuestionPOJO.unapply _)
}

trait QuestionsJSONTrait extends AttrsElemJSONTrait {
    // JSON default formats
    implicit val QuestionChoicesElemFormat = Json.format[QuestionChoicesElem]
    implicit val QuestionPOJOFormat = Json.format[QuestionPOJO]
    implicit val QuestionFormat = Json.format[Question]
    implicit val QuestionResultFormat = Json.format[QuestionResult]
}

object Questions 
    extends QuestionsJSONTrait 
    with AttrsElemJSONTrait {
    // ORM table of Question
    val table = TableQuery[Questions]

    private def attrsJSON2Object(
        attrsJsArray: JsArray): Option[List[AttrsElem]] = {
        var attrsListOpt = None: Option[List[AttrsElem]]
        attrsJsArray.validate[List[AttrsElem]] match {
            case s: JsSuccess[List[AttrsElem]] => {
                attrsListOpt = Option(s.get)
            }
            case e: JsError => {
                // error handling flow
            }
        }
        attrsListOpt
    }

    private def getPOJOFromClass(
        question: Question): QuestionPOJO = {
        var choicesJSONCmpString = None: Option[String]
        var attrsJSONCmpString = None: Option[String]
        var updtime = Option(System.currentTimeMillis()/1000L)
        var inittime = Option(System.currentTimeMillis()/1000L)
        var tombstone = Option(0)

        if (question.choices.isDefined) {
            choicesJSONCmpString = Snoopy.comp(
                Option(Json.toJson(question.choices.get).toString))
        }

        if (question.attrs.isDefined) {
            attrsJSONCmpString = Snoopy.comp(
                Option(Json.toJson(question.attrs.get).toString))
        }

        if (question.updtime.isDefined) {
            updtime = question.updtime
        }

        if (question.inittime.isDefined) {
            inittime = question.inittime
        }

        if (question.tombstone.isDefined) {
            tombstone = question.tombstone
        }

        val questionPOJO = QuestionPOJO(
            question.content,
            choicesJSONCmpString,
            attrsJSONCmpString,
            question.score,
            tombstone,
            updtime,
            inittime,
            question.id)
        questionPOJO
    }

    private def getClassFromPOJO(
        qPOJO: QuestionPOJO): Question = {
        var choicesListOpt = None: Option[List[QuestionChoicesElem]]
        if (qPOJO.choices.isDefined) {
            val choicesListJSON = Json.parse(Snoopy.decomp(qPOJO.choices).get)
            choicesListJSON.validate[List[QuestionChoicesElem]] match {
                case s: JsSuccess[List[QuestionChoicesElem]] => {
                    choicesListOpt = Option(s.get)
                }
                case e: JsError => {
                    // error handling flow
                }
            }
        }

        var attrsListOpt = None: Option[List[AttrsElem]]
        if (qPOJO.attrs.isDefined) {
            val attrsListJSON = Json.parse(Snoopy.decomp(qPOJO.attrs).get)
            attrsListJSON.validate[List[AttrsElem]] match {
                case s: JsSuccess[List[AttrsElem]] => {
                    attrsListOpt = Option(s.get)
                }
                case e: JsError => {
                    // error handling flow
                }
            }
        }

        val question = Question(
                qPOJO.content,
                choicesListOpt,
                attrsListOpt,
                qPOJO.score,
                qPOJO.tombstone,
                qPOJO.updtime,
                qPOJO.inittime,
                qPOJO.id)
        question 
    }

    private def duplicateIfNotNone(
        srcPOJO: QuestionPOJO, dstPOJO: QuestionPOJO) = {
        if (srcPOJO.content.isDefined) {
            dstPOJO.content = srcPOJO.content
        }
        if (srcPOJO.choices.isDefined) {
            dstPOJO.choices = srcPOJO.choices
        }
        if (srcPOJO.attrs.isDefined) {
            dstPOJO.attrs = srcPOJO.attrs
        }
        if (srcPOJO.score.isDefined) {
            dstPOJO.score = srcPOJO.score
        }
        if (srcPOJO.updtime.isDefined) {
            dstPOJO.updtime = srcPOJO.updtime
        }
    }

    def add(question: Question)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]

        val qPOJO = getPOJOFromClass(question)
        // allocate a new question
        val id = (table returning table.map(_.id)) += qPOJO
        question.id = id

        // wrap the retrieved result
        val questionResult = QuestionResult(
                QuestionResultStatus.QUESTION_OK,
                Option(question))
        retOpt = Option(questionResult)

        retOpt
    }

    def retrieve(id: Long)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]

        val qPOJOOpt = table.filter(_.id === id)
                        .filter(_.tombstone === 0)
                        .take(1).firstOption
        if (qPOJOOpt.isDefined) {
            val qPOJO = qPOJOOpt.get

            val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_OK,
                    Option(getClassFromPOJO(qPOJO)))
            retOpt = Option(questionResult)
        } else {
            // question doesn't exist
            val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_NOT_FOUND,
                        None)
            retOpt = Option(questionResult)
        }

        retOpt
    }

    def update(question: Question)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (question.id.isDefined) {
            val id = question.id.get
            val queryPOJOOpt = table.filter(_.id === id)
                                    .filter(_.tombstone === 0)
                                    .take(1).firstOption
            if (queryPOJOOpt.isDefined) {
                val queryPOJO = queryPOJOOpt.get
                val updPOJO = getPOJOFromClass(question)
                updPOJO.updtime = updtime
                updPOJO.inittime = queryPOJO.inittime

                // update the queryPOJO with updPOJO
                duplicateIfNotNone(updPOJO, queryPOJO)

                table.filter(_.id === queryPOJO.id.get)
                    .filter(_.tombstone === 0)
                    .map(row => (row.content, row.choices, row.attrs,
                                row.score, row.updtime))
                    .update((queryPOJO.content, queryPOJO.choices, queryPOJO.attrs,
                            queryPOJO.score, queryPOJO.updtime))

                // OK
                val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_OK,
                    Option(getClassFromPOJO(queryPOJO)))
                retOpt = Option(questionResult)
            } else {
                // question doesn't exist
                val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_NOT_FOUND,
                        None)
                retOpt = Option(questionResult)
            }
        } else {
            // invalid id
            val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_PARAMS_NOT_FOUND,
                        None)
            retOpt = Option(questionResult)         
        }

        retOpt   
    }

    def delete(question: Question)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (question.id.isDefined) {
            val id = question.id.get
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
                val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_OK,
                    Option(getClassFromPOJO(queryPOJO)))
                retOpt = Option(questionResult)
            } else {
                // question doesn't exists
                val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_NOT_FOUND,
                        None)
                retOpt = Option(questionResult)
            }
        } else {
            // invalid id
            val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_PARAMS_NOT_FOUND,
                        None)
            retOpt = Option(questionResult)         
        }

        retOpt
    }

    def attrsAppend(question: Question)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (question.attrs.isDefined && question.id.isDefined) {
            val id = question.id.get
            val attrsListUpdated = question.attrs.get
            // query question from db
            val questionOpt = table.filter(_.id === id)
                                .filter(_.tombstone === 0)
                                .take(1).firstOption
            if (questionOpt.isDefined) {
                val queryQuestion = questionOpt.get

                var attrsListUpdatedFinal = None: Option[List[AttrsElem]]
                if (queryQuestion.attrs.isDefined) {
                    // attrs is not null
                    // update the original attrs with attrsListUpdated
                    val queryQuestionAttrsJsArray = Json.parse(
                        Snoopy.decomp(queryQuestion.attrs).get).asOpt[JsArray].get
                    val queryQuestionAttrsListOpt = attrsJSON2Object(queryQuestionAttrsJsArray)
                    if (queryQuestionAttrsListOpt.isDefined) {
                        val queryQuestionAttrsList = queryQuestionAttrsListOpt.get
                        attrsListUpdatedFinal = AttrsElem.attrsListUnion(
                            queryQuestionAttrsList, attrsListUpdated)
                    }
                } else {
                    // attrs is null
                    // init attrs with attrsListUpdated
                    val queryQuestionAttrsList = List[AttrsElem]()
                    attrsListUpdatedFinal = AttrsElem.attrsListUnion(
                            queryQuestionAttrsList, attrsListUpdated)
                }

                // update the list to database
                if (attrsListUpdatedFinal.isDefined) {
                    val attrsListUpdatedFinalJSON = Json.toJson(attrsListUpdatedFinal.get)
                    val attrsListUpdatedFinalContent = Snoopy.comp(
                        Option(attrsListUpdatedFinalJSON.toString))
                    table.filter(_.id === id)
                        .filter(_.tombstone === 0)
                        .map(row => (row.attrs, row.updtime))
                        .update((attrsListUpdatedFinalContent, updtime))

                    queryQuestion.attrs = attrsListUpdatedFinalContent
                    queryQuestion.updtime = updtime

                    // OK
                    val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_OK,
                        Option(getClassFromPOJO(queryQuestion)))
                    retOpt = Option(questionResult)
                }
            } else {
                // question doesn't exists
                val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_NOT_FOUND,
                        None)
                retOpt = Option(questionResult)
            }
        } else {
            // invalid input question params
            val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_PARAMS_NOT_FOUND,
                    None)
            retOpt = Option(questionResult)
        }

        retOpt            
    }

    def attrsRemove(question: Question)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (question.attrs.isDefined && question.id.isDefined) {
            val id = question.id.get
            val attrsListRemoved = question.attrs.get
            val questionOpt = table.filter(_.id === id)
                                .filter(_.tombstone === 0)
                                .take(1).firstOption
            if (questionOpt.isDefined) {
                val queryQuestion = questionOpt.get
                var attrsListUpdatedFinal = None: Option[List[AttrsElem]]
                if (queryQuestion.attrs.isDefined) {
                    // only remove attrs elems when attrs is not null
                    val queryQuestionAttrsJsArray = Json.parse(
                        Snoopy.decomp(queryQuestion.attrs).get).asOpt[JsArray].get
                    val queryQuestionAttrsListOpt = attrsJSON2Object(queryQuestionAttrsJsArray)
                    if (queryQuestionAttrsListOpt.isDefined) {
                        val queryQuestionAttrsList = queryQuestionAttrsListOpt.get
                        attrsListUpdatedFinal = AttrsElem.attrsListDiff(
                            queryQuestionAttrsList, attrsListRemoved)
                    }
                }

                // update the list to database
                if (attrsListUpdatedFinal.isDefined) {
                    val attrsListUpdatedFinalJSON = Json.toJson(attrsListUpdatedFinal.get)
                    val attrsListUpdatedFinalContent = Snoopy.comp(
                        Option(attrsListUpdatedFinalJSON.toString))
                    table.filter(_.id === id)
                        .filter(_.tombstone === 0)
                        .map(row => (row.attrs, row.updtime))
                        .update((attrsListUpdatedFinalContent, updtime))

                    queryQuestion.attrs = attrsListUpdatedFinalContent
                    queryQuestion.updtime = updtime

                    // OK
                    val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_OK,
                        Option(getClassFromPOJO(queryQuestion)))
                    retOpt = Option(questionResult)
                }
            } else {
                // question doesn't exists
                val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_NOT_FOUND,
                        None)
                retOpt = Option(questionResult)
            }
        } else {
            // invalid input question params
            val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_PARAMS_NOT_FOUND,
                    None)
            retOpt = Option(questionResult)
        }

        retOpt
    }

    def attrsClean(question: Question)
        (implicit session: Session): Option[QuestionResult] = {
        var retOpt = None: Option[QuestionResult]
        val updtime = Option(System.currentTimeMillis()/1000L)

        if (question.id.isDefined) {
            val id = question.id.get
            val questionOpt = table.filter(_.id === id)
                            .filter(_.tombstone === 0)
                            .take(1).firstOption
            if (questionOpt.isDefined) {
                val queryPOJO = questionOpt.get
                val attrsJSONCleanCmpString = Snoopy.comp(
                        Option(JsArray(Seq()).toString))
                table.filter(_.id === id)
                    .filter(_.tombstone === 0)
                    .map(row => (row.attrs, row.updtime))
                    .update((attrsJSONCleanCmpString, updtime))

                queryPOJO.attrs = attrsJSONCleanCmpString
                queryPOJO.updtime = updtime

                // OK
                val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_OK,
                    Option(getClassFromPOJO(queryPOJO)))
                retOpt = Option(questionResult)
            } else {
                // question doesn't exists
                val questionResult = QuestionResult(
                        QuestionResultStatus.QUESTION_NOT_FOUND,
                        None)
                retOpt = Option(questionResult)
            }
        } else {
            // invalid input question params
            val questionResult = QuestionResult(
                    QuestionResultStatus.QUESTION_PARAMS_NOT_FOUND,
                    None)
            retOpt = Option(questionResult)             
        }

        retOpt          
    }
}

import models.QuestionsActor.{QuestionRetrieve, QuestionAdd, QuestionUpdate, QuestionDelete}
import models.QuestionsActor.{QuestionAttrsAppend, QuestionAttrsRemove, QuestionAttrsClean}

object QuestionsActor {  
    case class QuestionRetrieve(id: Long)
    case class QuestionAdd(question: Question)
    case class QuestionUpdate(question: Question) 
    case class QuestionDelete(question: Question)
    case class QuestionAttrsAppend(question: Question)
    case class QuestionAttrsRemove(question: Question)
    case class QuestionAttrsClean(question: Question)   
}

class QuestionsActor extends Actor {
    def receive: Receive = {
        case QuestionRetrieve(id: Long) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.retrieve(id)
            }
        }
        case QuestionAdd(question: Question) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.add(question)
            }
        }
        case QuestionUpdate(question: Question) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.update(question)
            }
        }
        case QuestionDelete(question: Question) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.delete(question)
            }
        }
        case QuestionAttrsAppend(question: Question) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.attrsAppend(question)
            }
        }
        case QuestionAttrsRemove(question: Question) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.attrsRemove(question)
            }
        }
        case QuestionAttrsClean(question: Question) => {
            DB.withTransaction { implicit session =>
                sender ! Questions.attrsClean(question)
            }
        }
    }
}
package controllers

import factories.util.{InstanceBase, FactoryParameters, FactoryBase, CollabDashParameters}
import play.api.Play.current
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import databases._
import utilities._
import play.api.libs.Files

object Application extends Controller {

  def index = Action {
    //Ok(views.html.index("Your new application is ready."))
    val cont = views.html.main("title")(FactoryRegister)
    Ok(cont)
  }

  def purge = Action { request =>
    CollabDB.purge()
    Redirect(routes.Application.index())
  }

  def createDataSourceFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("data source factories",
      FactoryRegister.dataSourceFactories(index), index, routes.Application.buildDataSource(_)))
  }

  def createTopicModelFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("topic model factories",
      FactoryRegister.topicModelFactories(index), index, routes.Application.buildTopicModel(_)))
  }

  def createCollabFilterModelFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("collab filter model factories",
      FactoryRegister.collabFilterModelFactories(index), index, routes.Application.buildCollabFilterModel(_)))
  }

  def createEvaluationFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("collab filter model factories",
      FactoryRegister.evaluationFactories(index), index, routes.Application.buildEvaluation(_)))
  }


  def buildDataSource(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.dataSourceFactories(index)
    val parameters = bindRequestToParams(factory, request)
    val datasource = factory.build(parameters, new CollabDashParameters)
    datasource onSuccess {
      case ds => CollabDB.addInstance(ds)
    }

    datasource onFailure {
      case th => throw th
    }

    Redirect(routes.Application.buildDataSource(index))
  }

  def fetchDataSource(index: Int) = Action {
    val datasource = CollabDB.getDataSource(index)

    Ok(datasource.getDescription + "\n" + datasource.getNumItems)
  }


  def buildTopicModel(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.topicModelFactories(index)
    val parameters = bindRequestToParams(factory, request)
    val topicModel = factory.build(parameters, new CollabDashParameters)
    topicModel onSuccess {
      case tm => CollabDB.addInstance(tm)
    }

    topicModel onFailure {
      case th => throw th
    }


    Redirect(routes.Application.buildTopicModel(index))
  }

  def fetchTopicModel(index: Int) = Action {
    val topicModel = CollabDB.getTopicModel(index)
    val dataSource = topicModel.dataStore

    Ok(topicModel.getDescription + "\n" + topicModel.numTopics + "\nData source: "+dataSource.getDescription)
  }

  def buildCollabFilterModel(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.collabFilterModelFactories(index)
    val parameters = explodeRequestToParams(factory, request)
    def building (fp: FactoryParameters): Unit = {

      val collabFilter = factory.build(fp, new CollabDashParameters)

      collabFilter onSuccess {
        case cf => CollabDB.addInstance(cf)
      }
      collabFilter onFailure {
        case th => throw th
      }
    }

    parameters.foreach (building(_))


    Redirect(routes.Application.buildCollabFilterModel(index))
  }


  def fetchCollabFilterModel(index: Int) = Action {
    val cfModel = CollabDB.getCollabFilterModel(index)
    Ok(cfModel.getDescription + "\n")
  }

  def buildEvaluation(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.evaluationFactories(index)
    val parameters = bindRequestToParams(factory, request)
    val evaluations = factory.build(parameters, new CollabDashParameters)

    evaluations onSuccess {
      case eval => CollabDB.addResultGroup(eval)
    }
    evaluations onFailure {
      case eval => throw eval
    }
    Redirect(routes.Application.buildEvaluation(index))
  }

  private def bindRequestToParams(factory: FactoryBase[InstanceBase], request: Request[MultipartFormData[Files.TemporaryFile]]): FactoryParameters = {
    val parameters = factory.parameters()
    val paramIndexes = Array.range(0, parameters.size()).map(_.toString)
    val paramValues = paramIndexes.map(request.body.dataParts.get(_).head.head)
    val paramNames = parameters.getParams.map(_.getName)
    var whatHappened = paramNames.zip(paramValues)
      .map(x => x._1 + " : " + x._2 + "\n")
      .reduceOption(_+_) match {
      case st: Some[String] => st.get
      case _ => ""
    }
    parameters.getParams.zip(paramValues)
      .map(x => x._1.setValue(x._2))
    parameters
  }

  private def explodeRequestToParams(factory: FactoryBase[InstanceBase], request: Request[MultipartFormData[Files.TemporaryFile]]): List[FactoryParameters] = {
    val results = new ListBuffer[FactoryParameters]()
    val numParams = factory.parameters().size()
    val paramIndexes = Array.range(0, numParams).map(_.toString)
    val paramValues = paramIndexes.map(request.body.dataParts.get(_).head.head.split(" "))

    var ongoing = mutable.Set(factory.parameters())

    def ongoingCopy() = ongoing.map(_.copy())
    def nOngoing(n: Int) = if (n > 1) {List.range(0, n).map(x => ongoingCopy())} else {List(ongoing)}
    def oneOngoing(l: List[mutable.Set[FactoryParameters]]) = if (l.size > 1) {
      l.reduce(_++_)
    } else {
      l(0)
    }
    def applyToAll(l: mutable.Set[FactoryParameters], v: String, index: Int) = l.foreach(_.getParam(index).setValue(v))

    paramValues.zipWithIndex.foreach(x => {
      val index = x._2
      val arr = x._1
      val news = nOngoing(arr.size)
      (news, arr.toList).zipped.foreach(applyToAll(_, _, index))
      ongoing = oneOngoing(news)
    })

    ongoing.toList
  }

  def getResults() = Action{ request =>
    val jsonified = Json.toJson(
      Map("data" -> CollabDB.getResultsForDisplay().map(x => x.toMap)))
    Ok(jsonified).as("application/json")
  }

  def getWideResults() = Action{ request =>
    val jsonified = Json.toJson(
      Map("data" -> CollabDB.getWideResultsForDisplay().map(x => x.toMap)))
    Ok(jsonified).as("application/json")
  }

  def resultsList() = Action {
    val cont = views.html.results("CollabDash")(FactoryRegister)
    Ok(cont)
  }

  def wideResultsList() = Action {
    val cont = views.html.wideResults("CollabDash")(FactoryRegister)
    Ok(cont)
  }
}
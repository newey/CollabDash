package controllers

import factories.util.{FactoryParameters, FactoryBase, CollabDashParameters}
import play.api.Play.current
import play.api._
import play.api.mvc._

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
    Ok(views.html.factory("data source factories")
      (FactoryRegister.dataSourceFactories(index), index, routes.Application.buildDataSource(_)))
  }

  def createTopicModelFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("topic model factories")
      (FactoryRegister.topicModelFactories(index), index, routes.Application.buildTopicModel(_)))
  }

  def createCollabFilterModelFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("collab filter model factories")
      (FactoryRegister.collabFilterModelFactories(index), index, routes.Application.buildCollabFilterModel(_)))
  }

  def createEvaluationFromFactory(index: Int) = Action { request =>
    Ok(views.html.factory("collab filter model factories")
      (FactoryRegister.evaluationFactories(index), index, routes.Application.buildEvaluation(_)))
  }

  def buildDataSource(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.dataSourceFactories(index)
    val parameters = bindRequestToParams(factory, request)

    val datasource = factory.buildDataSource(parameters, new CollabDashParameters)

    val whatHappened = datasource.getDescription + "\n"

    CollabDB.addInstance(datasource)

    Ok(whatHappened)
  }

  def fetchDataSource(index: Int) = Action {
    val datasource = CollabDB.getDataSource(index)

    Ok(datasource.getDescription + "\n" + datasource.getNumItems)
  }


  def buildTopicModel(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.topicModelFactories(index)
    val parameters = bindRequestToParams(factory, request)

    val topicModel = factory.buildTopicModel(parameters, new CollabDashParameters)

    val whatHappened = topicModel.getDescription + "\n"

    CollabDB.addInstance(topicModel)

    Ok(whatHappened)
  }

  def fetchTopicModel(index: Int) = Action {
    val topicModel = CollabDB.getTopicModel(index)
    val dataSource = topicModel.dataStore

    Ok(topicModel.getDescription + "\n" + topicModel.numTopics + "\nData source: "+dataSource.getDescription)
  }

  def buildCollabFilterModel(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.collabFilterModelFactories(index)
    val parameters = bindRequestToParams(factory, request)
    val collabFilter = factory.buildCollabFilterModel(parameters, new CollabDashParameters)

    val whatHappened = collabFilter.getDescription + "\n"

    CollabDB.addInstance(collabFilter)

    Ok(whatHappened)
  }


  def fetchCollabFilterModel(index: Int) = Action {
    val cfModel = CollabDB.getCollabFilterModel(index)
    Ok(cfModel.getDescription + "\n")
  }

  def buildEvaluation(index: Int) = Action (parse.multipartFormData) { request =>
    val factory = FactoryRegister.evaluationFactories(index)
    val parameters = bindRequestToParams(factory, request)
    val evaluations = factory.buildEvaluation(parameters, new CollabDashParameters)
    CollabDB.addResultGroup(evaluations)

    val whatHappened = evaluations.description + " " + evaluations.modelId.toString + " " + evaluations.scoreTypes.toString + " " + evaluations.scores.toString() + "\n"
    Ok(whatHappened)
  }

  private def bindRequestToParams(factory: FactoryBase, request: Request[MultipartFormData[Files.TemporaryFile]]): FactoryParameters = {
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
}
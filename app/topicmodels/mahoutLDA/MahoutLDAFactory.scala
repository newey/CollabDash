package topicmodels.mahoutLDA

import databases.CollabDB
import factories.{Document, DataSource, TopicModel, TopicModelFactory}
import factories.util.{ParamType, CollabDashParameters, FactoryParameters, Param}
import org.apache.mahout.math.{SparseColumnMatrix, Matrix}

import scala.collection.immutable.WrappedString

/**
 * Created by robert on 7/10/2014.
 */
object MahoutLDAFactory extends TopicModelFactory {
  override protected val params = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource),
    new Param(
      "Number of topics",
      "The desired number of topics for the LDA model",
      ParamType.Int),
    new Param(
      "Alpha",
      "",
      ParamType.Float),
    new Param(
      "Eta",
      "",
      ParamType.Float),
    new Param(
      "Number of training threads",
      "Suggested: 1",
      ParamType.Int),
    new Param(
      "Number of updating threads",
      "",
      ParamType.Int),
    new Param(
      "Model corpus fraction",
      "",
      ParamType.Float)
  )
  override val name: String = "MahoutLDA"

  override val description: String = "Builds an LDA model using the Mahout example code"

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): TopicModel = {
    val dataSourceInt = fp.getParam(0).getIntValue
    val dataSource = CollabDB.getDataSource(dataSourceInt)
    val numTopics = fp.getParam(1).getIntValue
    val alpha = fp.getParam(2).getDoubleValue
    val eta = fp.getParam(3).getDoubleValue
    val numTrainingThreads = fp.getParam(4).getIntValue
    val numUpdatingThreads = fp.getParam(5).getIntValue
    val modelCorpusFraction = fp.getParam(6).getDoubleValue
    val corpus = dataSourceToCorpusMatrix(dataSource)

    val lda = new MahoutLDA(corpus, numTopics, alpha, eta, numTrainingThreads, numUpdatingThreads, modelCorpusFraction)

    lda.iterateUntilConvergence(0.001, 100, 10)

    val description = new WrappedString("topics:%d-ds:%d-alpha:%f-eta:%f-corpusFrac:%f").
      format(numTopics, dataSourceInt, alpha, eta, modelCorpusFraction)


    val facId = 4625768947464L

    val matrix = lda.getTopicsPerItem


    val tm = new TopicModel(description, facId, dataSourceInt, matrixToArrays(matrix))


    tm
  }

  private def matrixToArrays(m: Matrix): Array[Array[Double]] =
    Array.range(0, m.numCols()).map(c => {
      Array.range(0, m.numRows()).map(r => m.get(r, c))
    })

  private def dataSourceToCorpusMatrix(ds: DataSource): Matrix = {
    val oldCorpus = ds.getCorpus
    val corpus = new SparseColumnMatrix(ds.getNumItems, ds.getNumWords)

    def insertDoc (doc: Document) = {
      val index = doc.index
      doc.terms.foreach(term => corpus.setQuick(index, term.term, term.count))
    }

    oldCorpus.foreach(insertDoc(_))

    corpus
  }
}

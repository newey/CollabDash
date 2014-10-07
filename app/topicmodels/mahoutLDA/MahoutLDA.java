package topicmodels.mahoutLDA;

import com.google.common.collect.Maps;
import org.apache.mahout.clustering.lda.cvb.ModelTrainer;
import org.apache.mahout.clustering.lda.cvb.TopicModel;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by robert on 11/09/2014.
 */
public class MahoutLDA {

    private static final Logger log = LoggerFactory.getLogger(MahoutLDA.class);

    private int numTopics;
    private int numTerms;
    private int numDocuments;
    private double alpha;
    private double eta;
    //private int minDfCt;
    //private double maxDfPct;
    private boolean verbose = false;
    private Matrix corpusWeights; // length numDocs;
    private double totalCorpusWeight;
    private double initialModelCorpusFraction;
    private Matrix docTopicCounts;
    private int numTrainingThreads;
    private int numUpdatingThreads;
    private ModelTrainer modelTrainer;

    public MahoutLDA(Matrix corpus,
                     int numTopics,
                     double alpha,
                     double eta,
                     int numTrainingThreads,
                     int numUpdatingThreads,
                     double modelCorpusFraction) {
        //this.seed = seed;
        this.numTopics = numTopics;
        this.alpha = alpha;
        this.eta = eta;
        //this.minDfCt = 0;
        //this.maxDfPct = 1.0f;
        corpusWeights = corpus;
        numDocuments = corpus.numRows();
        this.initialModelCorpusFraction = modelCorpusFraction;
        numTerms = corpus.numCols();

        this.numTrainingThreads = numTrainingThreads;
        this.numUpdatingThreads = numUpdatingThreads;
        postInitCorpus();
        initializeModel();

    }

    private void postInitCorpus() {
        totalCorpusWeight = 0;
        int numNonZero = 0;
        for (int i = 0; i < numDocuments; i++) {
            Vector v = corpusWeights.viewRow(i);
            double norm;
            if (v != null && (norm = v.norm(1)) != 0) {
                numNonZero += v.getNumNondefaultElements();
                totalCorpusWeight += norm;
            }
        }
        String s = "Initializing corpus with %d docs, %d terms, %d nonzero entries, total termWeight %f";
        log.info(String.format(s, numDocuments, numTerms, numNonZero, totalCorpusWeight));
    }

    private void initializeModel() {
        TopicModel topicModel = new TopicModel(numTopics, numTerms, eta, alpha, RandomUtils.getRandom(), null,
                numUpdatingThreads, initialModelCorpusFraction == 0 ? 1 : initialModelCorpusFraction * totalCorpusWeight);
        //topicModel.setConf(getConf());

        TopicModel updatedModel = initialModelCorpusFraction == 0
                ? new TopicModel(numTopics, numTerms, eta, alpha, null, null, numUpdatingThreads, 1)
                : topicModel;
        //updatedModel.setConf(getConf());
        docTopicCounts = new DenseMatrix(numDocuments, numTopics);
        docTopicCounts.assign(1.0 / numTopics);
        modelTrainer = new ModelTrainer(topicModel, updatedModel, numTrainingThreads, numTopics, numTerms);
    }

    private static void logTime(String label, long nanos) {
        log.info("{} time: {}ms", label, nanos / 1.0e6);
    }

    public void trainDocuments() {
        trainDocuments(0);
    }

    public void trainDocuments(double testFraction) {
        long start = System.nanoTime();
        modelTrainer.start();
        for (int docId = 0; docId < corpusWeights.numRows(); docId++) {
            if (testFraction == 0 || docId % (1 / testFraction) != 0) {
                Vector docTopics = new DenseVector(numTopics).assign(1.0 / numTopics); // docTopicCounts.getRow(docId)
                modelTrainer.trainSync(corpusWeights.viewRow(docId), docTopics , true, 10);
            }
        }
        modelTrainer.stop();
        logTime("train documents", System.nanoTime() - start);
    }

    public double iterateUntilConvergence(double minFractionalErrorChange,
                                          int maxIterations, int minIter) {
        return iterateUntilConvergence(minFractionalErrorChange, maxIterations, minIter, 0);
    }

    public double iterateUntilConvergence(double minFractionalErrorChange,
                                          int maxIterations, int minIter, double testFraction) {
        int iter = 0;
        double oldPerplexity = 0;
        while (iter < minIter) {
            trainDocuments(testFraction);
            if (verbose) {
                log.info("model after: {}: {}", iter, modelTrainer.getReadModel());
            }
            log.info("iteration {} complete", iter);
            oldPerplexity = modelTrainer.calculatePerplexity(corpusWeights, docTopicCounts,
                    testFraction);
            log.info("{} = perplexity", oldPerplexity);
            iter++;
        }
        double newPerplexity = 0;
        double fractionalChange = Double.MAX_VALUE;
        while (iter < maxIterations && fractionalChange > minFractionalErrorChange) {
            trainDocuments();
            if (verbose) {
                log.info("model after: {}: {}", iter, modelTrainer.getReadModel());
            }
            newPerplexity = modelTrainer.calculatePerplexity(corpusWeights, docTopicCounts,
                    testFraction);
            log.info("{} = perplexity", newPerplexity);
            iter++;
            fractionalChange = Math.abs(newPerplexity - oldPerplexity) / oldPerplexity;
            log.info("{} = fractionalChange", fractionalChange);
            oldPerplexity = newPerplexity;
        }
        if (iter < maxIterations) {
            log.info(String.format("Converged! fractional error change: %f, error %f",
                    fractionalChange, newPerplexity));
        } else {
            log.info(String.format("Reached max iteration count (%d), fractional error change: %f, error: %f",
                    maxIterations, fractionalChange, newPerplexity));
        }
        return newPerplexity;
    }

    public Matrix getTopicsPerItem() {
        return docTopicCounts;
    }

    public TopicModel getTrainedModel() {
        return modelTrainer.getReadModel();
    }
}

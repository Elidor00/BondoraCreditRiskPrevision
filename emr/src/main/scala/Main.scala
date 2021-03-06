import java.net.URI

import it.unibo.assembler.AssemblerFactory
import it.unibo.classifier.ClassifierFactory
import it.unibo.classifier.ClassifierFactory.{MLP, RF}
import it.unibo.datapreprocessor.DataPreprocessorFactory
import it.unibo.filesys.FileHandlerFactory
import it.unibo.normalizer.NormalizerFactory
import it.unibo.sparksession.{Configuration, SparkConfiguration}
import it.unibo.utils.Primitives
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.{DataFrame, Dataset, Row}

object Main {

  val normalizedDataSetPath: String = "/normalized.csv"
  val originDataSetPath: String = "/LoanData.csv"
  val meanFolder: String = "mean"

  def main(args: Array[String]): Unit = {

    implicit val sparkConfiguration: SparkConfiguration = new SparkConfiguration()

    val basePath: String = args.headOption getOrElse ".."

    val normalized: DataFrame = Primitives.time(getNormalizedDataFrame(basePath))

    val splits = normalized.randomSplit(Array(0.6, 0.4), seed = 1234L)
    val train: Dataset[Row] = splits(0)
    val test = splits(1)

    val trainers = Seq(ClassifierFactory(MLP), ClassifierFactory(RF))

    val normalizer = NormalizerFactory().getNormalizer(normalized.columns)
    val assembler = AssemblerFactory().getAssembler(normalized.columns)

    trainers.foreach(t => t.train(train, Array(assembler, normalizer)))

    val modelFileHandler = FileHandlerFactory(FileHandlerFactory.model)
    val dfFileHandler = FileHandlerFactory(FileHandlerFactory.df)

    modelFileHandler.createModelFolder()

    trainers.foreach(t => t.saveModel())

    if (modelFileHandler.isS3Folder(basePath)) {
      Seq("mlp", "rf").foreach(p => modelFileHandler.copyToS3(p, basePath))
      dfFileHandler.copyToS3(meanFolder, basePath)
    }

    trainers.foreach(t => println(s"${t.getClass}: ${t.evaluate(test)}"))
  }

  private def getNormalizedDataFrame(root: String)(implicit sparkConfiguration: Configuration): DataFrame = {
    println("getNormalizedDataFrame")
    val conf = sparkConfiguration.getOrCreateSession.sparkContext.hadoopConfiguration
    val fs = FileSystem.get(URI.create(root), conf)
    if (fs.exists(new Path(root + normalizedDataSetPath))) retrieveNormalizedDataFrame(root)
    else getDataFrame(root)
  }

  private def retrieveNormalizedDataFrame(root: String)(implicit sparkConfiguration: Configuration): DataFrame =
    sparkConfiguration.getOrCreateSession.read.format("csv")
      .option("header", value = true)
      .option("inferSchema", "true")
      .load(root + normalizedDataSetPath)

  private def getDataFrame(root: String)(implicit sparkConfiguration: Configuration): DataFrame = {

    val preprocessor = DataPreprocessorFactory()
    val df = preprocessor.readFile(root + originDataSetPath)
    Primitives.time(preprocessor.normalizeToTrain(df))
  }

}

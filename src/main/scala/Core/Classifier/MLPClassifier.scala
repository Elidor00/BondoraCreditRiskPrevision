package Core.Classifier

import Core.Assembler.BaseCustomAssembler
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.DataFrame

private class MLPClassifier(assembler: BaseCustomAssembler) extends BaseClassifier {

  override def train(df: DataFrame, features: Seq[String]): PipelineModel = {

    val trainer = createTrainer

    val vectorAssembler = assembler.createAssembler(features)

    new Pipeline()
      .setStages(Array(vectorAssembler, trainer))
      .fit(df)
  }

  private def createTrainer: MultilayerPerceptronClassifier =
    new MultilayerPerceptronClassifier()
      .setLayers(Array[Int](11, 6, 3))
      .setBlockSize(64)
      .setSeed(1234L)
      .setFeaturesCol("features")
      .setLabelCol("Status")
      .setMaxIter(1)

}

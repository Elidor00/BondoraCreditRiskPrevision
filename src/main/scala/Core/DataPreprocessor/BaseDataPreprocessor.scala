package Core.DataPreprocessor

import org.apache.spark.sql.functions.{current_date, lit}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

abstract class BaseDataPreprocessor(sparkSession: SparkSession) {

  val features = Seq("Age",
    "AppliedAmount",
    "Interest",
    "LoanDuration",
    "UseOfLoan",
    "MaritalStatus",
    "EmploymentStatus",
    "IncomeTotal",
    "NewCreditCustomer",
    "Country",
    "Status")

  def readFile(filePath: String): DataFrame =
    sparkSession.read.format("csv")
      .option("header", value = true)
      .load(filePath)

  // TODO: we need to decide how many labels we want to use
  def filterEndedLoans(df: DataFrame): Dataset[Row] = {
    df.select(features.head, features.tail: _*)
      .where(df.col("ContractEndDate").lt(lit(current_date())))
  }

  def normalize(df: DataFrame): DataFrame

}
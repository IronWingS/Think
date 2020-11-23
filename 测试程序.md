df命令，是用来查看目前在linux系统上文件系统磁盘使用情况统计。







```scala
package org.example

import com.huaban.analysis.jieba.JiebaSegmenter.SegMode
import com.huaban.analysis.jieba.{JiebaSegmenter, SegToken}
import org.ansj.recognition.impl.StopRecognition
import org.ansj.splitWord.analysis.{DicAnalysis, ToAnalysis}
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SQLContext, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import org.junit.{Assert, Test}

import scala.collection.mutable.ArrayBuffer

class test extends Assert {

  /** ************ jieba 分词测试 *****************/
  @Test
  def testDemo(): Unit = {
    val segmenter = new JiebaSegmenter
    val sentences = Array[String]("这是一个伸手不见五指的黑夜。我叫孙悟空，我爱北京，我爱Python和C++。", "我不喜欢日本和服。",
      "雷猴回归人间。", "工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作", "结果婚的和尚未结过婚的")

    for (sentence <- sentences) {
      System.out.println(segmenter.process(sentence, SegMode.INDEX).toString)
    }
  }


  /** ************* 读取csv文件配合jieba分词,以及word2vec，成功版本 ****************/
  @Test
  def dataFrametest(): Unit = {
    // 创建SparkSession
    val spark = SparkSession.builder()
      .appName("APP")
      .master("local")
      .getOrCreate()

    // 读取CSV文件
    val fileDF = spark.read.format("csv")
      .option("delimiter", ",")
      .option("header", "true")
      .option("nullValue", "")
      .option("inferSchema", "true")
      .load("test.csv")
      .select("f_media_desc")

    fileDF.show()

    // DataFrame转为Array
    val fileArr = fileDF.collect.map(_.toSeq).flatten

    // 保存分词结果的可变数组
    val segArrBuf = ArrayBuffer[String]()

    // 遍历Array并做jieba 分词
    for (s <- fileArr) {
      val segmenter = new JiebaSegmenter() // 创建jieba对象

      // 对读取的文件按行进行jieba分词
      val segResult = segmenter
        .process(s.toString, SegMode.SEARCH)
        .toArray()
        .map(_.asInstanceOf[SegToken].word)

      // 将分词的结果保存在可变数组中
      segArrBuf.++=:(segResult)

    }

    // 将数组转变为RDD，再转变为DataFrame
    val segDF = spark.createDataFrame(Seq(segArrBuf).map(Tuple1.apply)).toDF("content")

    segDF.show(false)

    /** ********* 调用word2vec统计词向量 ************/
    val word2Vec = new Word2Vec()
      .setInputCol("content")
      .setOutputCol("result")
      .setVectorSize(3)
      .setMinCount(0)

    val model = word2Vec.fit(segDF)
    val synonyms = model.findSynonyms("李莲英", 5)

    synonyms.show()

  }

  /** ********** 创建DataFrame测试 *******************/
  @Test
  def createDFTest(): Unit = {
    val spark = SparkSession.builder()
      .appName("APP")
      .master("local")
      .getOrCreate()

    /**
     * 1. Spark Create DataFrame from RDD
     */
    // 要引入这个RDD到DataFrame的隐式转换，得先创建一个SparkSession才行
    import spark.implicits._

    val columns = Seq("language", "users_count")
    val data = Seq(("Java", "20000"), ("Python", "100000"), ("Scala", "3000"))
    val rdd = spark.sparkContext.parallelize(data)

    /* 创建DataFrame的方法一 */
    val dfFromRDD1 = rdd.toDF("language", "users_count")
    dfFromRDD1.printSchema()

    /* 创建DataFrame的方法二 */
    val dfFromRDD2 = spark.createDataFrame(rdd).toDF(columns: _*)

    /* 创建DataFrame的方法三 */
    val schema = StructType(Array(StructField("language", StringType, true),
      StructField("language", StringType, true)))
    val rowRDD = rdd.map(attributes => Row(attributes._1, attributes._2))
    val dfFromRDD3 = spark.createDataFrame(rowRDD, schema)

    /**
     * 2. Create Spark DataFrame from List and Seq Collection
     */
    val dfFromData1 = data.toDF("language", "users_count")
    dfFromData1.show()
  }

  /** ********** word2vec测试1 *******************/
  @Test
  def word2vecTest: Unit = {
    val conf = new SparkConf().setAppName("Word2Vec example").setMaster("local")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    // $example on$
    // Input data: Each row is a bag of words from a sentence or document.
    val documentDF = sqlContext.createDataFrame(Seq(
      "Hi I heard about Spark".split(" "),
      "I wish Java could use case classes".split(" "),
      "Logistic regression models are neat".split(" ")
    ).map(Tuple1.apply)).toDF("text")

    documentDF.show(false)

    // Learn a mapping from words to Vectors.
    val word2Vec = new Word2Vec()
      .setInputCol("text")
      .setOutputCol("result")
      .setVectorSize(3)
      .setMinCount(0)
    val model = word2Vec.fit(documentDF)
    val result = model.transform(documentDF)
    result.select("result").take(3).foreach(println)
    // $example off$
  }


  /** ********** word2vec测试2 *******************/
  @Test
  def word2vector2(): Unit = {
    val conf = new SparkConf().setAppName("Word2Vec example").setMaster("local")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    // $example on$
    // Input data: Each row is a bag of words from a sentence or document.
    val documentDF = sqlContext.createDataFrame(Seq(
      "Hi wish hope  I heard about Spark".split(" "),
      " wish hope I wish hope Java wish hope could use case wish hope classes".split(" "),
      "Logistic wish hope regression wish hope models are neat wish hope ".split(" ")
    ).map(Tuple1.apply)).toDF("text")

    documentDF.show()

    // Learn a mapping from words to Vectors.
    val word2Vec = new Word2Vec()
      .setInputCol("text")
      .setOutputCol("result")
      .setVectorSize(1)
      .setMinCount(0)

    val model = word2Vec.fit(documentDF)
    val synonyms = model.findSynonyms("wish", 5)

    synonyms.show()

  }

  @Test
  def hadoopFileTest(): Unit = {
    val spark = SparkSession.builder()
      .appName("hadoopFileTest")
      .master("local")
      .getOrCreate()

    import spark.implicits._
    val file = "t_media_series_i1.csv"

    val cleanFileRDD = spark.read.format("csv")
      .option("delimiter", ",")
      .option("header", "true")
      .option("inferSchema", "true")
      .load(file)
      .select("f_media_desc")
      .filter(row => !row.equals(null))
      .rdd.map(_.toString())

    val stop = new StopRecognition()
    stop.insertStopNatures("w") //过滤掉标点
    stop.insertStopNatures("m") //过滤掉m词性
    stop.insertStopNatures("null") //过滤null词性
    stop.insertStopNatures("<br />") //过滤<br　/>词性
    stop.insertStopNatures(":")
    stop.insertStopNatures("'")

    val segmentResult = cleanFileRDD.mapPartitions(row => {
      row.map(word => {
        val nlpList = DicAnalysis.parse(word).recognition(stop).toStringWithOutNature()
        nlpList.trim
      })
    })

    //      .toArray()
    //      .map(_.asInstanceOf[SegToken].word)

    segmentResult.toDF("f_media_desc_before").show()
    val value = segmentResult.map(row => row.split(",")).toDF()


//    val segmentDF = value.toDF("f_media_desc_after")


    val value1 = value.flatMap(_.toString().split(",")).map(x => Seq(x)).toDF("content")

    value1.limit(10).show()

    //创建Word2Vec对象
    val word2Vec = new Word2Vec()
      .setInputCol("content")
      .setVectorSize(50)
      .setNumPartitions(64)

    //    val segDF = spark.createDataFrame(Seq(segArrBuf).map(Tuple1.apply)).toDF("content")
    //                                              .map(Tuple1.apply)
//    val finalDF = value1.map(x => Seq(x.toString()))

//    val list = finalDF.select("content").limit(30).collect().map(_(0)).toList

//    var tmp: String = finalDF.select("content").limit(1).first().get(0).toString
//    println(tmp)
    //    finalDF.limit(10).show()
    //训练模型
    val model = word2Vec.fit(value1)

    value1.limit(100).show()
    val like = model.findSynonyms("安德海", 10)
    like.foreach(println(_))
    //    for((item, literacy) <- like){
    //      print(s"$item $literacy")
    //    }

  }


  @Test
  def ansjTest(): Unit = {
    val str = "欢迎使用ansj_seg,(ansj中文分词)在这里如果你遇到什么问题都可以联系我.我一定尽我所能.帮助大家.ansj_seg更快,更准,更自由!"
    println(ToAnalysis.parse(str))
  }
}

```



```scala
package org.example

import org.ansj.recognition.impl.StopRecognition
import org.ansj.splitWord.analysis.DicAnalysis
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.sql.SparkSession
import org.junit.Test

class test2 {

  @Test
  def hadoopFileTest(): Unit = {
    val spark = SparkSession.builder()
      .appName("hadoopFileTest")
      .master("local")
      .getOrCreate()

    import spark.implicits._
    val file = "t_media_series_i1.csv"

    val cleanFileDF = spark.read.format("csv")
      .option("delimiter", ",")
      .option("header", "true")
      .option("inferSchema", "true")
      .load(file)
      .select("f_media_desc")

    val stop = new StopRecognition()
    stop.insertStopNatures("w") //过滤掉标点
    stop.insertStopNatures("m") //过滤掉m词性
    stop.insertStopNatures("null") //过滤null词性
    stop.insertStopNatures("<br />") //过滤<br　/>词性
    stop.insertStopNatures(":")
    stop.insertStopNatures("'")

    val segmentResultDF = cleanFileDF.map(row => {
        val nlpList = DicAnalysis.parse(row.toString()).recognition(stop).toStringWithOutNature()
        nlpList.trim.split(",")
      }).toDF("content")

    segmentResultDF.show(false)

    println("=======================================================:   " + segmentResultDF.count())

//    segmentResultDF.write.format("csv").save("F:\\Thinker\\wordRecommend\\test1.csv")

    //创建Word2Vec对象
    val word2Vec = new Word2Vec()
      .setInputCol("content")
      .setOutputCol("result")
      .setVectorSize(100)
      .setMinCount(0)
    val model = word2Vec.fit(segmentResultDF)

    //训练模型
    val like = model.findSynonyms("清朝", 10)
    like.foreach(println(_))

    //    for((item, literacy) <- like){
    //      print(s"$item $literacy")
    //    }

  }

  @Test
  def w2vDFtest() = {
    val spark = SparkSession.builder()
      .appName("APP")
      .master("local")
      .getOrCreate()

    val documentDF = spark.createDataFrame(Seq(
      "Hi I heard about Spark".split(" "),
      "I wish Java could use case classes".split(" "),
      "Logistic regression models are neat".split(" ")
    ).map(Tuple1.apply)).toDF("text")

    documentDF.show(false)

    val word2Vec = new Word2Vec().
             setInputCol("text").
             setOutputCol("result").
             setVectorSize(3).
             setMinCount(0)

    val model = word2Vec.fit(documentDF)
    val result = model.transform(documentDF)
    result.select("result").take(3).foreach(println)
  }

  @Test
  def segmentTest():Unit = {
    val spark = SparkSession.builder()
      .appName("hadoopFileTest")
      .master("local")
      .getOrCreate()

    import spark.implicits._
    val file = "test.csv"

    val cleanFileDF = spark.read.format("csv")
      .option("delimiter", ",")
      .option("header", "true")
      .option("inferSchema", "true")
      .load(file)
      .select("f_media_desc")

    val stop = new StopRecognition()
    stop.insertStopNatures("w") //过滤掉标点
    stop.insertStopNatures("m") //过滤掉m词性
    stop.insertStopNatures("null") //过滤null词性
    stop.insertStopNatures("<br />") //过滤<br　/>词性
    stop.insertStopNatures(":")
    stop.insertStopNatures("'")

    val segmentResultDF = cleanFileDF.map(row => {
      val nlpList = DicAnalysis.parse(row.toString()).recognition(stop).toStringWithOutNature()
      nlpList.trim.split(",")
    }).toDF("content")


    segmentResultDF.show(false)

//    val segmentResultDF = cleanFileDF.map(row => {
//      val nlpList = DicAnalysis.parse(row.toString()).recognition(stop).toStringWithOutNature()
//      nlpList.trim.split(",")
//    }).toDF("content")
  }

}

```




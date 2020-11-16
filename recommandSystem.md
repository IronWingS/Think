# 基础

## scala代码



### jieba分词

```scala
package org.example

import com.huaban.analysis.jieba.JiebaSegmenter.SegMode
import com.huaban.analysis.jieba.{JiebaSegmenter, SegToken}

class segmentation() {

  def jiebaSeg(): Unit ={

    // jieba 分词
    val segmenter = new JiebaSegmenter()
    val segmentation = segmenter.process("今天早上，出门的时候，天气很好".filter(_ != '，'), SegMode.SEARCH)
      .toArray()
      .map(_.asInstanceOf[SegToken].word)


  }
}
```





### 错误信息

#### 一、

```shell
org.apache.spark.sql.AnalysisException: cannot resolve '`f_media_desc`' given input columns: ["f_english_name", "f_director", "f_content_focus", "f_media_desc", "f_singers", "f_actors", "f_area", "f_emotion", "f_language", "f_chinese_name", "f_scene", "f_sname", "f_second_name", "f_content_type", "f_nick_name", "f_sub_type", "f_scenarist", "f_story_yesr", "f_tv_station"];;
'Project ['f_media_desc]
```

原因，我的csv文件中，每个字段都有双引号，而我在读取csv文件时指定为了单引号。

DataFrame默认的就是引用符号就是双引号。



```scala
val fileDF = spark.read.format("csv")
      .option("delimiter", ",")
      .option("header", "true")
      .option("quote", "’")   // 这里指定错了，应该为 "\""，或者不指定
      .option("nullValue", "")
      .option("inferSchema", "true")
      .load("t_media_series_i1.csv")
      .select("f_media_desc")
```







```shell
# mvn 打包命令
mvn clean package -Ppro
```




































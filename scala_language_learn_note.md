## SCALA 

```scala
// 把dataFrame转换为map
val halfHourDf = DataFrame.map(x => {
        // ListBuffer是一个可变列表
        // List是不可变列表
        val lb = new ListBuffer[(String, Int)] 
        // RDD 的getAs方法，可以获得RDD中指定的字段        
        val terminal = x.getAs[String]("terminal")
}
```

- case class一般用来给不可变数据提供一个模型，不用new关键字实例化。内置一个apply方法，可以直接利用函数名创建，会默认自动调用该方法

- HashMap

  ```scala
  import sqlContext.implicits._
  
  a += ("k3" -> 3) // 给HashMap添加元素
  a -= ("k3")  // 删除元素
  
  a.get("k3") // 返回的是一个可选值的Option[T]的容器，如果有值就是Some[T]，否则就是None
  a.get("k3").get  // 得到Option中的值
  ```



## Hive

- `collect_set`

Hive不能直接访问非groupby的字段。

`collect_set`方法可以把每个`id`对应的所有`time`字段整合为以逗号分隔的数组，通过该方法可以让hive访问非`group_by`字段。

```sql
SELECT id,collect_set(time) AS t FROM t_action_login WHERE time<='20150906' GROUP BY id
```

- `concat_ws`

用指定的分隔符链接字符串









## SQL 

```sql
SELECT pub_name
FROM publisher
WHERE country <> "USA"
-- <> 这个表示不等于，
-- != 这个也表示不等于
```

```sql
ROUND(D, N)
-- 将D保留N位小数
```

- sql的逻辑计划和物理计划都是什么？这里忘记了









## spark

- 

    ```scala
    import sqlContext.implicits._
    // 可以将普通的scala对象转换为一个DataFrame
    ***.toDF()
    ```

    ```scala
    HiveContext.sql
    // 利用Spark处理一段SQL，并将结果返回为一个DataFrame
    ```

- distinct

  对DataFrame进行去重，原理是先对所有的内容进行wordcount，然后将统计的结果去掉。这样就完成了去重操作。

- agg

  ```scala
  df.agg(max($"age"), avg($"salary"))
  df.groupBy().agg(max($"age"), avg($"salary"))
  // 按字段进行聚合操作，上面是下面这种写法的简写版本。
  ```



## Java

- 匿名内部类持有着外部类的引用

- split

  ```java
  String s = "aaa|aa*aa.aa$aa";
  s.split("\\|");
  // |  *  .  $  用这些转移字符切分字符串的时候必须得加 \\ 
  ```





## 整理

总之都是统计，各种计算，各种条件下的统计， 统计个人数。

那就得先拿到数据，然后判断下数据是否符合要求，也就是过滤一下，然后按照一定的要求进行统计。

所以，不管多复杂的程序，肯定都是这三个步骤：

1. 首先得到数据，一般就是select * from table;  然后开始过滤数据
2. 过滤数据就是要判断
3. 最后就是组合表，进行多条件的查询





##### github上的一本书

<https://zhuangbiaowei.gitbook.io/learn-with-open-source/>
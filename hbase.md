解决hbase的shell无法使用backspace的问题

```shell
https://blog.csdn.net/tongyuehong137/article/details/45458859

https://stackoverflow.com/questions/49649449/hbase-shell-crash-after-backspace
```









```shell
# 创建表
create ‘<table name>’,’<column family>’
create 'userinfo', 'subtype', 'count_subtype'


# 插入数据
put ’<table name>’,’row1’,’<colfamily:colname>’,’<value>’
       userinfo   user_id    subtype:split subtype   score
 
put 'userinfo','1','subtype:1001','0.6'
put 'userinfo','1','subtype:1002','0.4'

put 'emp','1','personal data:city','hyderabad'
put 'emp','1','professional data:designation','manager'
put 'emp','1','professional data:salary','50000'


put 'emp','2','personal data:name','ravi'
put 'emp','2','personal data:city','chennai'
put 'emp','2','professional data:designation','sr.engineer'
put 'emp','2','professional data:salary','30000'

put 'emp','3','personal data:name','rajesh'
put 'emp','3','personal data:city','delhi'
put 'emp','3','professional data:designation','jr.engineer'
put 'emp','3','professional data:salary','25000'

```



```shell
docker exec -e COLUMNS="`tput cols`" -e LINES="`tput lines`" -ti hbase bash
```



创建hive表的时候，只需要指定表名和列族名就可以。

插入数据的时候，可以再进一步指定具体的列名。

不知道是不是这样，暂时先这样理解，测试一下。



在Phoenix和hbase中做视图映射

```shell
create view "emp"(
  "cusid" varchar primary key,
  "personal data"."name" varchar,
  "personal data"."city" varchar,
  "professional data"."designation" varchar,
  "professional data"."salary" varchar)
```





- 遇到的问题

那个一直提示输出路径没有设置的问题是由

```scala
val subtype = row(1).toString.split("[|]").foreach(x => {
    
  put.addColumn(Bytes.toBytes("row_data"), Bytes.toBytes(x), Bytes.toBytes(row(2).toString))
      })

  (new ImmutableBytesWritable(), put)
}).saveAsNewAPIHadoopDataset(jobConf)
```

最后的`saveAsNewAPIHadoopDataset`造成的，将该方法换为`saveAsHadoopDataset`就能解决该问题。

将DataFrame保存到HBase中共有四种方法：

> 1.在RDD内部调用java API。
> 2、调用saveAsNewAPIHadoopDataset（）接口。
> 3、saveAsHadoopDataset（）。
> 4、BulkLoad方法。 

https://www.cnblogs.com/runnerjack/p/10480468.html



- 删除HBase表

  利用shell的disable和drop两个命令删掉HBase的表之后，依然不能创建同名表，因为Zookeeper中还保存了Hbase的表名信息。

  ```shell
  hbase zkcli
  ls /hbase/table-lock   # 删除该目录下的相关文件
  
  ls /hbase/table # 删除该目录下的相关文件
  ```

  这两个目录下的文件都删除之后，就可以新建同名表了。


































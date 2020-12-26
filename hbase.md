解决hbase的shell无法使用backspace的问题

```shell
https://blog.csdn.net/tongyuehong137/article/details/45458859

https://stackoverflow.com/questions/49649449/hbase-shell-crash-after-backspace
```





查看表

```
list
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



在进入docker时，指定列宽

```shell
docker exec -e COLUMNS="`tput cols`" -e LINES="`tput lines`" -ti hbase bash
```





修改HBase表的版本号

```shell
alter 'test1', NAME => 'cf', VERSIONS => 5
alter 'blog', NAME => 'artitle', VERSIONS => 3


# 插入数据
put ’<table name>’,’row1’,’<colfamily:colname>’,’<value>’
       userinfo   user_id    subtype:split subtype   score
 
put 'userinfo','1','subtype:1001','0.6'

put 'blog', '002','artitle:engish', 'No, C++ is the best'
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





HBase中的每个数据都有三个版本，这个版本是有时间戳来实现的。

在HBase中，新增数据和更改数据都是由Put实现的。



user_info

​	        row_data                                  statistic_data

​     user_id          subtype	                      user_id      subtype  

​	

```sql
-- 利用phoenix建立HBase表的语法
-- （但是这样的话，相当于没有指定列族，有点意思）
create table if not exists "test"(
    "user_id" varchar(32),
    "subtype" varchar(32),
    CONSTRAINT pk PRIMARY KEY ("user_id")
);

-- 
CREATE TABLE IF NOT EXISTS "test1" (
    "userid" VARCHAR(32),
    "subtype"."101" VARCHAR(32),
    CONSTRAINT pk PRIMARY KEY ("userid")
);

-- 利用phoenix插入动态列数据到HBase中的语法
UPSERT INTO "test" ("user_id","subtype"."1001" VARCHAR(32))
    VALUES ('500010', '0.9');
    
-- phoenix删除表的语法
delete from system.catalog where table_name = 't1';
```







### HBase修改列的数据类型

1.先看原本类型：（表名要大写）

```sql
select TENANT_ID,TABLE_SCHEM,TABLE_NAME,COLUMN_NAME,COLUMN_FAMILY,DATA_TYPE,COLUMN_SIZE,DECIMAL_DIGITS from SYSTEM.CATALOG where TABLE_NAME='TEST01';
!desc test01;
select * from test01;
```

2.修改DATA_TYPE,12为varchar类型，'A'为字段名

```sql
upsert into SYSTEM.CATALOG (TENANT_ID,TABLE_SCHEM,TABLE_NAME,COLUMN_NAME,COLUMN_FAMILY,DATA_TYPE) values('','','表名','字段名','0',12);
```

3.删除同一字段类型为integer类型的元数据，4为integer类型

```sql
delete from SYSTEM.CATALOG where TABLE_NAME='TEST01' and COLUMN_NAME='A' and DATA_TYPE=4;
```

4.重启HBASE、ph

```shell
oenix
```

5.验证

```shell
!desc test01
select * from test01;
```



create 'test3',

所以，现在
需要处理的情况就是：

一、建表，建立这个可以存储30个版本数据的表

1. 表结构

   主键、        列族         列（具体的剧集id），里面的值就是分数score

   user_id	subtype  



二、将用户id对应的剧集数据保存到这张表里面







get 'user_info1','50000202',{COLUMN=>'row_data:11000004',VERSIONS=>3}











![1608532145637](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1608532145637.png)







```shell
format_string(a)
```










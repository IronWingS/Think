解决hbase的shell无法使用backspace的问题

```shell
https://blog.csdn.net/tongyuehong137/article/details/45458859

https://stackoverflow.com/questions/49649449/hbase-shell-crash-after-backspace
```









```shell
# 创建表
create ‘<table name>’,’<column family>’
create 'emp', 'personal data', 'professional data'


# 插入数据
put ’<table name>’,’row1’,’<colfamily:colname>’,’<value>’


put 'emp','1','personal data:name','raju'
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


































```xml
连接数据库，
<db_ip>192.168.48.100</db_ip>
<db_name>homed_dtvs</db_name>
<db_username>homed</db_username>
<db_password>newclustersql</db_password>
```



```shell
mysql -h 192.168.48.100 -P 3306 -u homed -p newclustersql
```





# 2020-11-27

- [x] 今天的主要工作就是完成两种不同格式数据的统一，并让他们进行运算。

- [x] 然后有时间就搞定docker跨主机通信的问题。（问了下，结果这个需要用docker compose来做，就很麻烦了，今天搞不定，只能先有个大概了解）
- [ ] 刷算法，今天任务少，其实可以偷偷多刷几道



统一时间格式这个，mysql没问题。但是DataFrame，我最开始是想找一个类似的api，但是昨天一下午都没找到。今天，还是先找找看，如果有现成的方法，就直接用吧。

如果还是没找到，就看能不能把s换成datatype。

总之就是要求两个数的商。



# 2020-11-30

今天主要是要搞好docker的一键化部署的脚本还有文档，因为文档的一些内容有些变动，然后就是把文档再改一版为部署的步骤说明性文档，就不要那种科普性的了。

然后上周五本来说多刷几道算法，结果一到也没刷，今天必须至少刷一道算法题。

还有公众号，现在是148个粉丝， 哎，都是我爸妈的功劳，我也得支棱起来啊。今天无论如何得发一篇文章。

- [ ] 搞定docker一键部署代码
- [ ] 整理两份docker文档
- [ ] 一道算法题
- [ ] 一篇公众号文章

**注意，这不是演习，今天必须全部做完，绝对不能留尾巴！！！**







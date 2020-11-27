

```SHELL
rpm -qa | grep mysql
```



```shell
rpm -e --nodeps
```



强制退出当前命令



```SHELL
rpm -ivh http://dev.mysql.com/get/mysql57-community-release-el7-9.noarch.rpm

yum install mysql-community-server

```



```shell
docker run -itd --hostname mysql --name mysql --privileged centos:7.8.2003 init
```

```shell
docker exec -it mysql /bin/bash
```



## CentOS 安装MySQL

centos默认的数据库为MariaDB，如果需要安装MySQL，需要先删除。

检查主机上的MariaDB

```shell
rpm -qa | grep mariadb
```

删除依然使用rpm工具

```shell
# 这里的***指上一条命令查到的结果
rpm -e --nodeps ***
```

使用yum安装mysql可以不用删除mariadb，会覆盖掉



检查mysql

```shell
rpm -qa | grep mysql

rpm -e --nodeps 
```



添加MySQL的 yum repository

> centOS 7开始，MariaDB为yum源默认的数据库安装包，如果在centos7以上的系统中使用yum安装mysql，默认安装的是mariadb。如果想安装官方mysql版本，需要使用mysql提供的yum源。



查看系统版本

```shell
# centos
cat /etc/redhat-release 
# 
```

根据删选的结果进行下载，利用wget下载

```shell
wget https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
```

该命令会吧rpm包下载到

安装yum源

```shell
rpm -Uvh mysql80-community-release-el7-3.noarch.rpm
```

执行成功后在/etc/yum.repos.d/ 目录下生成两个repo文件 mysql-community.repo 和 mysql-community-source.repo

并且通过 yum repolist 可以看到mysql相关资源





查看mysql安装过程的log日志

```shell
cd /var/log/mysql/

```



```shell
docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -d mysql:5.7
```





Debian 安装 yum

sudo apt-get update
sudo apt-get install build-essential
sudo apt-get install yum



添加用户

```
adduser ***
```



# hive

## 本地模式





查看docker容器信息，包括ip

docker inspect <container ID>





定时任务

crontab -e

0 6 * * * root /root/docker_test/hive_data_migration/increm_migration_hive_data.sh



```shell
docker volume create \
  --opt type=tmpfs \
  --opt device=tmpfs \
  --opt o=size=1G,uid=1000 \
  mysql

# 启动mysql容器
docker run -d -p 63306:3306 --net bigdata \
    --name mysql5.7 --hostname mysql5.7 \
    -e MYSQL_ROOT_PASSWORD=root -v mysql:/var/lib/mysql \
    mysql:5.7 
```




```shell

schematool -initSchema -dbType mysql

```



hive-site.xml

```xml
<!--开头加上这几句-->
<property>
    <name>system:java.io.tmpdir</name>
    <value>/tmp/hive/java</value>
  </property>
 <property>
    <name>system:user.name</name>
    <value>${user.name}</value>
 </property>
```



| name                                    | value                                               | description                    |
| --------------------------------------- | --------------------------------------------------- | ------------------------------ |
| `javax.jdo.option.ConnectionURL`        | `jdbc:mysql://localhost:3306/metastore?useSSL=true` | 指定连接的数据库（之前创建的） |
| `javax.jdo.option.ConnectionDriverName` | `com.mysql.jdbc.Driver`                             | 数据库驱动                     |
| `javax.jdo.option.ConnectionUserName`   | `hiveuser`                                          | 用户名（之前创建的）           |
| `javax.jdo.option.ConnectionPassword`   | `123456`                                            | 用户名密码                     |














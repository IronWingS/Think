电子书地址： <https://yeasy.gitbook.io/docker_practice/image/pull>

# 第一章 Apache 软件下载地址

## 一、 Apache官网

<https://archive.apache.org/dist/>

## 二、 镜像

### 2.1 北京理工大学镜像

http://mirror.bit.edu.cn/apache/hadoop/common/

### 三、系统版本

#### 3.1 hadoop

Hadoop 2.7.7

#### 3.2 spark

Spark 2.4.7

#### 3.3 jdk

jdk1.8.0_261

#### 3.4 scala

scala-2.12.8





# 第二章 Docker

## 一、docker安装

### 1.1 配置yum源

```shell
yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
```

### 1.2 安装docker

```shell
# 查看社区版docker版本
yum list docker-ce --showduplicates | sort -r
# 安装
yum install docker-ce-18.03.1.ce -y
```

### 1.3 启动docker服务

```shell
systemctl start docker
systemctl enable docker
```

### 1.4 查看docker版本

```shell
docker version
```



## 二、 镜像

### 2.1 配置国内源

```shell
/etc/docker/daemon.json
```

{
  "registry-mirrors":[
​    "https://hub-mirror.c.163.com",
​    "https://mirror.baidubce.com"
  ],
  "experimental":true
}



### 2.2 常用操作

#### (1) 列出所有的镜像

```shell
docker image ls
```

#### (2) 列出存储信息

```shell
docker system df
```

#### (3) 删除镜像

```shell
docker rmi [容器ID]
```

#### (4) 导出导入镜像

```shell
# 导出镜像
docker save -o <**.tar> <镜像ID/name> 
# 导入镜像
docker load -i nginx.tar
docker load < nginx.tar
   # 其中-i和<表示从文件输入。会成功导入镜像及相关元数据，包括tag信息
```



### 2.3 虚悬镜像

新下载的镜像和原镜像同名了，原镜像的名称和标签都为none。

#### (1) 列出所有的虚悬镜像

`docker image ls -f dangling=true`

#### (2) 删除所有的虚悬镜像

`docker image prune`



### 2.4 制作镜像

#### (1) docker commit

```shell
docker commit \
    --author "Tao Wang <twang2218@gmail.com>" \
    --message "修改了默认网页" \
    webserver \
    nginx:v2
```

#### (2) docker file

```shell
docker build -t nginx:v3 .
```



## 三、 容器

### 3.1 常规操作

#### (1) 启动容器

```shell
docker run --name web2 -d -p 81:80 nginx:v2
```

#### (2) 参数配置

```shell
docker run -itd  -p 50022:22 --privileged --name sshtest ssh:v1  /bin/bash
	-d：后台运行； -i：打开容器的标准输入； -t：为容器建立一个命令行终端
	-m 500M：内存控制
	--cpuset-mems="1,3"：CPU控制   
	-p：端口映射
	--privileged : 开启使用systemctl的特权
```

#### (3) 停止容器

```shell
docker stop <容器ID>
```

#### (4) 查看容器

```shell
docker ps
docker ps -a   # 查看所有容器，包括停止的
```

#### (5) 运行停止容器

```shell
docker start <name> / <ID>
```

#### (6) 删除容器

```shell
docker rm [name]/[ID]
```

```shell
docker rm $(docker ps -a -q)  # 删除所有停止的容器
```

#### (7) 进入到运行中的容器

```shell
 docker attach <ID>
 docker exec -it spark-master /bin/bash
```

#### (8) 退出不关闭容器

```shell
ctrl + p + q
```



## 3.2 配置细节

#### (1) 存储路径

docker的存储层默认是在`/var/lib/docker/overlay2`路径下，但是不能持久化，退出容器后数据就会丢失，所以需要配置volume数据卷。

数据卷的默认路径在`/var/lib/docker/volumes/`

#### (2) 容器静态化为镜像

```shell
docker commit <容器ID/name> <镜像名:版本号>
```

#### (3) 导入导出容器

```shell
docker export -o nginx-test.tar nginx-test
# 其中-o表示输出到文件，nginx-test.tar为目标文件，nginx-test是源容器名
docker import <文件路径>  <容器名>
```

#### (4) 配置存储位置

```shell
# 首先停掉Docker服务
service docker stop
# 然后移动整个/var/lib/docker目录到目的路径：
mv /var/lib/docker /r2/docker
ln -s /r2/docker /var/lib/docker
```

#### (5) 配置静态路由

```shell
route  [add|del] [-net|-host] target [netmask Nm] [gw Gw] [[dev] If]
# add : 添加一条路由规则 / del : 删除一条路由规则
# -net : 目的地址是一个网络 / -host : 目的地址是一个主机
# target : 目的网络或主机
# netmask : 目的地址的网络掩码
# gw : 路由数据包通过的网关
# dev : 为路由指定的网络接口
```

```shell
route add -net 172.17.2.0 netmask 255.255.255.0 gw 192.168.48.59
route add -net 172.17.1.0 netmask 255.255.255.0 gw 192.168.48.58
```

#### (6) 配置iptables

```shell
iptables -t nat -F POSTROUTING
iptables -t nat -A POSTROUTING -s 172.17.2.0/24 ! -d 172.17.0.0/16 -j MASQUERADE

iptables -t nat -F POSTROUTING
iptables -t nat -A POSTROUTING -s 172.17.1.0/24 ! -d 172.17.0.0/16 -j MASQUERADE
```

```shell
# 选项
-t<表>：指定要操纵的表；
-A：向规则链中添加条目；
-D：从规则链中删除条目；
-i：向规则链中插入条目；
-R：替换规则链中的条目；
-L：显示规则链中已有的条目；
-F：清楚规则链中已有的条目；
-Z：清空规则链中的数据包计算器和字节计数器；
-N：创建新的用户自定义规则链；
-P：定义规则链中的默认目标；
-h：显示帮助信息；
-p：指定要匹配的数据包协议类型；
-s：指定要匹配的数据包源ip地址；
-j<目标>：指定要跳转的目标；
-i<网络接口>：指定数据包进入本机的网络接口；
-o<网络接口>：指定数据包要离开本机所使用的网络接口。
```

```shell
# 表名包括：
raw：高级功能，如：网址过滤。
mangle：数据包修改（QOS），用于实现服务质量。
net：地址转换，用于网关路由器。
filter：包过滤，用于防火墙规则。

# 规则链名包括：
INPUT链：处理输入数据包。
OUTPUT链：处理输出数据包。
PORWARD链：处理转发数据包。
PREROUTING链：用于目标地址转换（DNAT）。
POSTOUTING链：用于源地址转换（SNAT）。

# 动作包括：
accept：接收数据包。
DROP：丢弃数据包。
REDIRECT：重定向、映射、透明代理。
SNAT：源地址转换。
DNAT：目标地址转换。
MASQUERADE：IP伪装（NAT），用于ADSL。
LOG：日志记录。
```

#### (7) 查看iptables中的表规则

```shell
iptables -L -t nat # 查看nat表中的规则
```







# 第四章 dockerfile

1. FROM
   指定基础镜像

2. RUN
   用来执行命令行命令的，有两种格式：

   - shell格式
     就像在命令行中输入命令一样

     ```shell
     RUN echo '<h1>Hello, Docker!</h1>' > /usr/share/nginx/html/index.html
     ```

   - exec格式
     类似函数调用的格式

     ```shell
     RUN ["可执行文件", "参数1", "参数2"]
     ```



```shell
FROM centos:7.8.2003

MAINTAINER ironwing <chningwing@163.com>

WORKDIR /root

# install openssh-server, openjdk and wget
RUN yum install -y openssh-server wget which vim lrzsz less openssh-clients initscripts

# copy files
COPY /root/docker_test/hadoop_cluster_test/hadoop-2.7.7.tar.gz /root
COPY /root/docker_test/hadoop_cluster_test/jdk-8u261-linux-x64.tar.gz /root
COPY /root/docker_test/hadoop_cluster_test/spark-2.4.7-bin-hadoop2.7.tgz /root
COPY /root/docker_test/hadoop_cluster_test/scala-2.12.8.tgz /root

# install hadoop-2.7.7  jdk1.8.0_261  scala-2.12.8  spark-2.4.7-bin-hadoop2.7
RUN mkdir /bigdata && \
    tar -xzvf hadoop-2.7.7.tar.gz -C /bigdata && \
    tar -xzvf jdk-8u261-linux-x64.tar.gz -C /bigdata && \
    tar -xzvf spark-2.4.7-bin-hadoop2.7.tgz -C /bigdata && \
    tar -xzvf scala-2.12.8.tgz -C /bigdata && \
    rm hadoop-2.7.7.tar.gz && \
    rm jdk-8u261-linux-x64.tar.gz && \
    rm spark-2.4.7-bin-hadoop2.7.tgz && \
    rm scala-2.12.8.tgz

# set environment variable
ENV JAVA_HOME=/bigdata/jdk1.8.0_261
ENV HADOOP_HOME=/bigdata/hadoop-2.7.7
ENV SCALA_HOME=/bigdata/scala-2.12.8
ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$SCALA_HOME/bin

# ssh without key
RUN ssh-keygen -t rsa -f ~/.ssh/id_rsa -P '' && \
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

RUN mkdir -p ~/hdfs/namenode && \ 
    mkdir -p ~/hdfs/datanode && \
    mkdir $HADOOP_HOME/logs

COPY config/* /tmp/


RUN mv /tmp/ssh_config ~/.ssh/config && \ # /etc/ssh/ssh_config
    mv /tmp/hadoop-env.sh $HADOOP_HOME/etc/hadoop/hadoop-env.sh && \
    mv /tmp/hdfs-site.xml $HADOOP_HOME/etc/hadoop/hdfs-site.xml && \ 
    mv /tmp/core-site.xml $HADOOP_HOME/etc/hadoop/core-site.xml && \
    mv /tmp/mapred-site.xml $HADOOP_HOME/etc/hadoop/mapred-site.xml && \
    mv /tmp/yarn-site.xml $HADOOP_HOME/etc/hadoop/yarn-site.xml && \
    mv /tmp/slaves $HADOOP_HOME/etc/hadoop/slaves && \
    mv /tmp/start-hadoop.sh ~/start-hadoop.sh && \
    mv /tmp/run-wordcount.sh ~/run-wordcount.sh

```

**得记得set ff=unix**





# 第五章 Linux

## 5.1 命令

### 1 常用命令

#### (1) touch

创建一个新的文件。

#### (2) tar

```shell
# 打包命令
tar -zcvf name.tar.gz name
# 解包命令
tar -zxvf name.tar.gz
# 参数
    -z 打包和解包命令
    -x 解打包
    -c 打包
    -v 显示打包过程
    -f 指定目录
```

#### (3) rsync

```shell
# 同步文件命令
rsync spark-cluster_v2.tar 192.168.106.59:/root/docker_test
rsync spark-cluster_v2.tar 192.168.106.68:/root/docker_test
rsync spark-cluster_v2.tar 192.168.106.69:/root/docker_test
```

#### (4) netstat

```shell
netstat   -nultp
```

#### (5) df

```shell
df -h
```

### 2 查看命令

#### (1) 查看端口占用情况

```shell
lsof -i:<端口ID>
```

#### (2) 查看ip地址

```shell
hostname -I
ip addr
ifconfig
```

#### (3) 查看centos的版本

```shell
cat /etc/centos-release
```



## 5.2 配置环境

### 1. Java环境

```shell
JAVA_HOME=...
export PATH=$JAVA_HOME/bin:$PATH
```



### 2. 配置ssh

#### (1) 安装ssh服务器

```shell
yum list | grep ssh
# 只安装client就行了
yum install **client

yum install openssh-server -y
yum install openssh-clients -y
```

#### (2) ssh免密码登录

```shell
ssh-keygen -t rsa
scp ~/.ssh/id_rsa.pub root@192.168.1.1:
cat id_rsa.pub >> ~/.ssh/authorized_keys
```

#### (3) 启动ssh服务

```shell
service sshd start
```

```shell
#（2）进入容器，设置容器root密码
# 修改容器的root密码：passwd
# 密码设置为：123456

#（3）修改ssh配置,允许root登录
# vi /etc/ssh/sshd_config 
# 将PermitRootLogin的值从withoutPassword改为yes

 /usr/sbin/sshd -D
```

### 3 其他配置

#### (1) 安装service服务

```shell
# 使用yum list | grep initscripts可以搜索可安装的软件包
# 会出现initscripts.x86_64
yum -y install initscripts # 就能使用service服务了
```

#### (2) 修改root用户密码

```shell
# root用户下
passwd
```

#### (3) 安装ifconfig

```shell
yum install -y net-tools
```

#### (4) 安装rz、sz

```shell
yum install -y lrzsz
```

#### (5) 在其他机器上运行shell脚本上的命令

```shell
# eeooff可以随便写个什么，但是需要成对出现，
ssh spark-master  << eeooff

ls
echo \"spark-master!\"

eeooff
echo done!
```







# 第六章 docker集群配置

## 一、基础环境

### 1 宿主机

```shell
192.168.48.59  (Bussines2.0-hdfsslave2)  
```

### 2 centos镜像

```shell
docker pull centos:7.8.2003
```



## 二、 配置hadoop伪分布模式

### 1 配置java

```shell
vim hadoop-2.7.7/etc/hadoop/hadoop-env.sh
export JAVA_HOME=/bigdata/java/jdk1.8.0_261
```



### 2 hdfs-site.xml

```xml
<!-- hadoop-2.7.7/etc/hadoop/hdfs-site.xml -->
<property>  
    <name>dfs.replication</name>
    <value>1</value>
</property>
<property>
    <name>dfs.namenode.name.dir</name>
    <value>file:///bigdata/hadoop/hadoop-2.7.7/tmp/name</value>
</property>
<property>
    <name>dfs.datanode.data.dir</name>
    <value>file:///bigdata/hadoop/hadoop-2.7.7/tmp/data</value>
</property>
```



### 3 core-site.xml

```shell
# 创建tmp目录
mkdir /bigdata/hadoop/hadoop-2.7.7/tmp
```

```xml
<!--配置HDFS主节点的地址，就是NameNode的地址-->
<!--9000是RPC通信的端口-->
<property>  
    <name>fs.defaultFS</name>
    <value>hdfs://spark-master:9000</value>
</property> 

<!--HDFS数据块和元信息保存在操作系统的目录位置-->
<!--默认是Linux的tmp目录,一定要修改-->
<property>  
    <name>hadoop.tmp.dir</name>
    <value>/bigdata/hadoop/hadoop-2.7.7/tmp</value>
</property>
```



### 4 mapred-site.xml

```shell
cp mapred-site.xml.template mapred-site.xml
```

```xml
<property>  
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
</property> 
```



### 5 yarn-site.xml

```xml
<!--配置Yarn主节点的位置-->
<property>  
    <name>yarn.resourcemanager.hostname</name>
    <value>spark-master</value>
</property>         

<!--NodeManager执行MR任务的方式是Shuffle洗牌-->
<property>  
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
</property>

<property>
    <name>yarn.resourcemanager.webapp.address</name>
    <value>0.0.0.0:8088</value>
</property>

<!--关闭yarn任务运行时的内存检测-->
<property>
    <name>yarn.nodemanager.pmem-check-enabled</name>
    <value>false</value>
</property>
<property>
    <name>yarn.nodemanager.vmem-check-enabled</name>
    <value>false</value>
</property>
```



### 6 格式化namenode

```shell
hdfs namenode -format 
```



## 三、 配置spark伪分布模式

```shell
mv conf/spark-env.sh.template conf/spark-env.sh  
vim spark-env.sh  
```

```shell
export JAVA_HOME=/bigdata/java/jdk1.8.0_261  
export SCALA_HOME=/bigdata/scala/scala-2.12.8
export HADOOP_HOME=/bigdata/hadoop/hadoop-2.7.7
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export SPARK_HOME=/bigdata/spark/spark-2.4.7
export SPARK_WORKER_CORES=1  
export SPARK_EXECUTOR_INSTANCES=1 
export SPARK_WORKER_MEMORY=1g  
export SPARK_MASTER_IP=spark-master
```



## 四、配置Hadoop全分布模式

### 1 配置文件

#### 1.1 hdfs-site.xml

```xml
<!-- hadoop-2.7.7/etc/hadoop/hdfs-site.xml -->
<property>  
    <name>dfs.replication</name>
    <value>3</value>
</property>
<property>
    <name>dfs.namenode.name.dir</name>
    <value>file:///bigdata/hadoop/hadoop-2.7.7/tmp/name</value>
</property>
<property>
    <name>dfs.datanode.data.dir</name>
    <value>file:///bigdata/hadoop/hadoop-2.7.7/tmp/data</value>
</property>
```

#### 1.2 core-site.xml

```xml
<?xml version="1.0"?>
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://spark-master:9000/</value>
    </property>
</configuration>
```

#### 1.3 slaves

```shell
<slave1 hostname>
<slave2 hostname>
```

#### 1.4 yarn-site.xml

```xml
<?xml version="1.0"?>
<configuration>
    <!--配置Yarn主节点的位置-->
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>spark-master</value>
    </property>

    <!--NodeManager执行MR任务的方式是Shuffle洗牌-->
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>

    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>0.0.0.0:8088</value>
    </property>

    <!--关闭yarn任务运行时的内存检测-->
    <property>
        <name>yarn.nodemanager.pmem-check-enabled</name>
        <value>false</value>
    </property>
    <property>
        <name>yarn.nodemanager.vmem-check-enabled</name>
        <value>false</value>
    </property>
</configuration>
```

#### 1.5 hadoop-env.sh

```shell
# 在该文件中配置java的路径
export JAVA_HOME=/bigdata/jdk1.8.0_261
```



## 五、docker swarm 配置多容器通信

```shell
# master 初始化swarm集群
docker swarm init --advertise-addr 192.168.106.58

# slave 加入swarm集群
docker swarm join --token SWMTKN-1-1iand4b3nj06w1z2bjurcj19ymh7xf4n7hj9xkuo9l02wq5r35-8vor2hookk6llgn8jvji1paqe 192.168.106.58:2377

# 查看
docker network ls

# 创建网络
docker network create --driver overlay --attachable bigdata

# 运行两个容器测试
docker run -it -d --name spark-master --hostname spark-master -m 5G  --cpuset-mems="1" --net bigdata spark-cluster:v1 /bin/bash
      
docker run -it -d --name spark-slave1 --hostname spark-slave1 -m 20G --cpuset-mems="1" --net bigdata spark-cluster:v1 /bin/bash

docker run -it -d --name spark-slave2 --hostname spark-slave2 -m 20G --cpuset-mems="1" --net bigdata spark-cluster:v1 /bin/bash

docker run -it -d --name spark-slave3 --hostname spark-slave3 -m 75G --cpuset-mems="1" --net bigdata spark-cluster:v1 /bin/bash
```



## 六、docker volume

### 1 创建数据卷

```shell
docker volume create \
  --opt type=tmpfs \
  --opt device=tmpfs \
  --opt o=size=50G,uid=1000 \
  hdfs-m

docker volume create \
  --opt type=tmpfs \
  --opt device=tmpfs \
  --opt o=size=50G,uid=1001 \
  hdfs-1
  
docker volume create  \
  --opt type=tmpfs \
  --opt device=tmpfs \
  --opt o=size=50G,uid=1002 \
  hdfs-2
  
# --opt=o=size=20M 控制内存大小
```



### 2 挂载数据卷

```shell
docker run -itd \
    --name test1 \
    --mount source=my-vol,target=/etc \
    centos:7.8.2003 /bin/bash
```





# 第七章 部署

## 1 脚本

### 1.1 容器启动脚本 start-container.sh

```shell
#!/bin/bash

# 配置集群ip
master_ip=192.168.106.58
slave1_ip=192.168.106.68
slave2_ip=192.168.106.69

# 获得当前主机ip
ip=`ifconfig |grep 192.168.106.*|awk '{print $2}'`

echo $ip

# 在各自的主机上创建镜像，启动容器
if [ $ip == $master_ip ]
then
    # 制作镜像
    docker build -t hadoop:v1 .

    # 配置volume
    docker volume create \
      --opt type=tmpfs \
      --opt device=tmpfs \
      --opt o=size=50G,uid=1000 \
      hdfs-m

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

    # 启动hadoop容器
    docker run -it -d \
        -p 60022:22 -p 60080:8080 -p 60088:8088 -p 60070:50070 \
        -p 18080:18080 -p 60040:4040 -p 60020:8020\
        --name spark-master --hostname spark-master \
        -m 5G  --net bigdata --cpuset-mems="1" \
        --privileged \
        -v hdfs-m:/root/hdfs/namenode \
        hadoop:v1 \
        init
        
elif [ $ip == $slave1_ip ]
then
    # 制作镜像
    docker build -t hadoop:v1 .

    # 配置volume
    docker volume create \
      --opt type=tmpfs \
      --opt device=tmpfs \
      --opt o=size=50G,uid=1001 \
      hdfs-s1

    # 启动容器
    docker run -it -d \
        -p 60022:22 -p 60075:50075 -p 60042:8042 -p 60081:8081 \
        --name spark-slave1 --hostname spark-slave1 \
        -m 20G --net bigdata --cpuset-mems="1" \
        --privileged \
        -v hdfs-s1:/bigdata/hadoop/hadoop-2.7.7/hdfs/datanode \
        hadoop:v1 \
        init
        
elif [ $ip == $slave2_ip ]
then
    # 制作镜像
    docker build -t hadoop:v1 .
    # 配置volume
    docker volume create  \
      --opt type=tmpfs \
      --opt device=tmpfs \
      --opt o=size=50G,uid=1002 \
      hdfs-s2

    # 启动容器
    docker run -it -d \
        -p 60022:22 -p 60075:50075 -p 60042:8042 -p 60081:8081 \
        --name spark-slave2 --hostname spark-slave2 \
        -m 75G --net bigdata --cpuset-mems="1" \
        --privileged \
        -v hdfs-s2:/bigdata/hadoop/hadoop-2.7.7/hdfs/datanode \
        hadoop:v1 \
        init
else 
    echo "请在指定的主机上进行安装"
fi
```



### 1.2 集群启动脚本 start-cluster.sh

```shell
#!/bin/bash

# 宿主机ip或主机名
hadoop_slave1=hdfsslave3
hadoop_slave2=hdfsslave4

# 同步配置文件
rsync -rtv /root/docker_test/hadoop_cluster_test $hadoop_slave1:/root/docker_test

rsync -rtv /root/docker_test/hadoop_cluster_test $hadoop_slave2:/root/docker_test

# 调用容器启动脚本
cd /root/docker_test/hadoop_cluster_test
. start-container.sh

ssh $hadoop_slave1 "cd /root/docker_test/hadoop_cluster_test; chmod +x start-container.sh; . start-container.sh"

ssh $hadoop_slave2 "cd /root/docker_test/hadoop_cluster_test; chmod +x start-container.sh; . start-container.sh"

# 进入集群
docker exec -it spark-master /bin/bash
```



### 1.3 集群Dockerfile

```shell
FROM centos:7.8.2003

MAINTAINER ironwing <chningwing@163.com>

WORKDIR /root

# 安装常用工具
RUN yum install -y openssh-server wget which vim lrzsz less openssh-clients initscripts net-tools

# 复制安装包
COPY hadoop-2.7.7.tar.gz /root
COPY jdk-8u261-linux-x64.tar.gz /root
COPY spark-2.4.7-bin-hadoop2.7.tgz /root
COPY scala-2.12.8.tgz /root
COPY apache-hive-1.2.2-bin.tar.gz /root

# 安装相关软件
RUN mkdir /bigdata && \
    tar -xzvf hadoop-2.7.7.tar.gz -C /bigdata && \
    tar -xzvf jdk-8u261-linux-x64.tar.gz -C /bigdata && \
    tar -xzvf spark-2.4.7-bin-hadoop2.7.tgz -C /bigdata && \
    tar -xzvf scala-2.12.8.tgz -C /bigdata && \
    tar -xzvf apache-hive-1.2.2-bin.tar.gz -C /bigdata && \
    mv /bigdata/apache-hive-1.2.2-bin /bigdata/hive-1.2.2 && \
    mv /bigdata/spark-2.4.7-bin-hadoop2.7 /bigdata/spark-2.4.7 && \
    rm hadoop-2.7.7.tar.gz && \
    rm jdk-8u261-linux-x64.tar.gz && \
    rm spark-2.4.7-bin-hadoop2.7.tgz && \
    rm scala-2.12.8.tgz && \
    rm apache-hive-1.2.2-bin.tar.gz

# 环境变量
ENV JAVA_HOME=/bigdata/jdk1.8.0_261
ENV HADOOP_HOME=/bigdata/hadoop-2.7.7
ENV SCALA_HOME=/bigdata/scala-2.12.8
ENV HIVE_HOME=/bigdata/hive-1.2.2
ENV SPARK_HOME=/bigdata/spark-2.4.7
ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$SCALA_HOME/bin:$HIVE_HOME/bin

# 配置ssh
RUN ssh-keygen -t rsa -f ~/.ssh/id_rsa -P '' && \
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys && \
    ssh-keygen -q -t rsa -b 2048 -f /etc/ssh/ssh_host_rsa_key -N '' && \
    ssh-keygen -q -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N '' && \
    ssh-keygen -t dsa -f /etc/ssh/ssh_host_ed25519_key -N ''

RUN echo "root:root" | chpasswd

RUN mkdir -p ~/hdfs/namenode && \
    mkdir -p ~/hdfs/datanode && \
    mkdir $HADOOP_HOME/logs

COPY config/* /tmp/
COPY mysql-connector-java-5.1.49.jar /bigdata/hive-1.2.2/lib

RUN mv /tmp/sshd_config /etc/ssh/sshd_config && \
    mv /tmp/hadoop-env.sh $HADOOP_HOME/etc/hadoop/hadoop-env.sh && \
    mv /tmp/hdfs-site.xml $HADOOP_HOME/etc/hadoop/hdfs-site.xml && \
    mv /tmp/core-site.xml $HADOOP_HOME/etc/hadoop/core-site.xml && \
    mv /tmp/mapred-site.xml $HADOOP_HOME/etc/hadoop/mapred-site.xml && \
    mv /tmp/yarn-site.xml $HADOOP_HOME/etc/hadoop/yarn-site.xml && \
    cp /tmp/slaves $HADOOP_HOME/etc/hadoop/slaves && \
    mv /tmp/slaves $SPARK_HOME/conf && \
    mv /tmp/spark-env.sh $SPARK_HOME/conf && \
    mv /tmp/hive-site.xml $HIVE_HOME/conf && \
    mv /tmp/hive $HIVE_HOME/bin && \
    chmod +x $HIVE_HOME/bin/hive && \
    mv /tmp/start-hadoop.sh /root
```



## 2 配置文件

路径： `./config`

### 2.1 core-site.xml

```shell
<?xml version="1.0"?>
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://spark-master:9000/</value>
    </property>
</configuration>
```



### 2.2 hadoop-env.sh

```shell
# 只新增了JAVA的环境变量
```



### 2.3 hdfs-site.xml

```xml
<?xml version="1.0"?>
<configuration>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file:///root/hdfs/namenode</value>
        <description>NameNode directory for namespace and transaction logs storage.</description>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file:///root/hdfs/datanode</value>
        <description>DataNode directory</description>
    </property>
    <property>
        <name>dfs.replication</name>
        <value>3</value>
    </property>
</configuration>
```



### 2.4 mapred-site.xml

```xml
<?xml version="1.0"?>
<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
</configuration>
```



### 2.5 slaves

```shell
spark-slave1
spark-slave2
```



### 2.6 spark-env.sh

```shell
# 新增如下内容  ，  
export SCALA_HOME=/bigdata/scala-2.12.8
export JAVA_HOME=/bigdata/jdk1.8.0_261
export HADOOP_HOME=/bigdata/hadoop-2.7.7
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
SPARK_MASTER_IP=hadoop-master
SPARK_LOCAL_DIRS=/bigdata/spark-2.4.7
SPARK_DRIVER_MEMORY=512M
```



### 2.7 yarn-site.xml

```xml
<?xml version="1.0"?>
<configuration>
    <!--配置Yarn主节点的位置-->
    <property>  
        <name>yarn.resourcemanager.hostname</name>
        <value>spark-master</value>
    </property>         

    <!--NodeManager执行MR任务的方式是Shuffle洗牌-->
    <property>  
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>

    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>0.0.0.0:8088</value>
    </property>

    <!--关闭yarn任务运行时的内存检测-->
    <property>
        <name>yarn.nodemanager.pmem-check-enabled</name>
        <value>false</value>
    </property>
    <property>
        <name>yarn.nodemanager.vmem-check-enabled</name>
        <value>false</value>
    </property>
</configuration>
```



hive配置文件

$HIVE_HOME/bin/hive

```shell
# 最近将整个架构升级到spark 2.0.0之后，发现一个问题，就是每次进行hive --service metastore启动的时候，总是会报一个小BUG。
# 无法访问/home/ndscbigdata/soft/spark-2.0.0/lib/spark-assembly-*.jar: 没有那个文件或目录。
# 只需要把 $HIVE_HOME/bin/hive 文件 116 行修改下即可
sparkAssemblyPath=`ls ${SPARK_HOME}/lib/spark-assembly-*.jar`
sparkAssemblyPath=`ls ${SPARK_HOME}/jars/*.jar`
```



把hive-default.xml.template改为hive-site.xml然后修改一下几个地方。

```xml

<property>
    <name>system:java.io.tmpdir</name>
    <value>/tmp/hive/java</value>
</property>
<property>
    <name>system:user.name</name>
    <value>${user.name}</value>
</property>

<property>
    <name>hive.exec.dynamic.partition.mode</name>
    <value>nostrict</value>
    <description>
        In strict mode, the user must specify at least one static partition
        in case the user accidentally overwrites all partitions.
        In nonstrict mode all partitions are allowed to be dynamic.
    </description>
</property>


<property>
    <name>javax.jdo.option.ConnectionPassword</name>
    <value>root</value>
    <description>password to use against metastore database</description>
</property>
<property>
      
    
<property>
    <name>javax.jdo.option.ConnectionURL</name>
    <value>jdbc:mysql://mysql5.7:3306/hive?createDatabaseIfNotExist=true&amp;useSSL=false</value>
    <description>JDBC connect string for a JDBC metastore</description>
</property>

    
<property>
    <name>javax.jdo.option.ConnectionDriverName</name>
    <value>com.mysql.jdbc.Driver</value>
    <description>Driver class name for a JDBC metastore</description>
</property>
      
    
<property>
    <name>javax.jdo.option.ConnectionUserName</name>
    <value>root</value>
    <description>Username to use against metastore database</description>
</property>
    
//导入需要的类
import java.util.HashMap;
// 定义类
public class Main {
    // 定义得到好数的方法（int 数组）
    public int numIdenticalPairs(int[] nums) {
        // 定义hashmap
        HashMap<Integer, Integer> map = new HashMap<>();
        // 将数组中出现的数字和该数字出现的次数保存在hashmap中
        map.put(map.getOrDefault(1))
        // 定义求和结果
        int result = 0
        // 利用组合公式求每个数字的组合数，对组合数求和

        // 返回结果
        return result;
    }
}
    
    
class Solution {
    public int numIdenticalPairs(int[] nums) {
        Map<Integer, Integer> m = new HashMap<Integer, Integer>();
        for (int num : nums) {
            m.put(num, m.getOrDefault(num, 0) + 1);
        }

        int ans = 0;
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            int v = entry.getValue();
            ans += v * (v - 1) / 2;
        }

        return ans;
    }
}


```









## 2 端口

```shell
60088  --> 8088   yarn resourcemanager
60070  --> 50070  hdfs namenode
60080  --> 8080   master的webUI，sparkwebUI
60022  --> 22     ssh
60040  --> 4040   application的webUI
60180  --> 18080  historyServer的webUI

60075  --> 50075  hdfs datanode
60042  --> 8042   yarn nodemanager
60081  --> 8081   worker的webUI


       --> 7077：提交任务的端口
```







# 第八章 一键部署

secureCRT最下面的命令窗口叫交谈窗口



1. 两台linux系统之间是怎么通信的
2. 两个进程之间是怎么通信的





### 集群Dockerfile

```shell
FROM centos:7.8.2003

MAINTAINER ironwing <chningwing@163.com>

WORKDIR /root

# install openssh-server, openjdk and wget
RUN yum install -y openssh-server wget which vim lrzsz less openssh-clients initscripts

# copy files
COPY hadoop-2.7.7.tar.gz /root
COPY jdk-8u261-linux-x64.tar.gz /root
COPY spark-2.4.7-bin-hadoop2.7.tgz /root
COPY scala-2.12.8.tgz /root

# install hadoop-2.7.7  jdk1.8.0_261  scala-2.12.8  spark-2.4.7-bin-hadoop2.7
RUN mkdir /bigdata && \
    tar -xzvf hadoop-2.7.7.tar.gz -C /bigdata && \
    tar -xzvf jdk-8u261-linux-x64.tar.gz -C /bigdata && \
    tar -xzvf spark-2.4.7-bin-hadoop2.7.tgz -C /bigdata && \
    tar -xzvf scala-2.12.8.tgz -C /bigdata && \
    rm hadoop-2.7.7.tar.gz && \
    rm jdk-8u261-linux-x64.tar.gz && \
    rm spark-2.4.7-bin-hadoop2.7.tgz && \
    rm scala-2.12.8.tgz

# set environment variable
ENV JAVA_HOME=/bigdata/jdk1.8.0_261
ENV HADOOP_HOME=/bigdata/hadoop-2.7.7
ENV SCALA_HOME=/bigdata/scala-2.12.8
ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$SCALA_HOME/bin

# ssh without key
RUN ssh-keygen -t rsa -f ~/.ssh/id_rsa -P '' && \
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

RUN mkdir -p ~/hdfs/namenode && \ 
    mkdir -p ~/hdfs/datanode && \
    mkdir $HADOOP_HOME/logs

COPY config/* /tmp/


RUN mv /tmp/ssh_config ~/.ssh/config && \ # /etc/ssh/ssh_config
    mv /tmp/hadoop-env.sh $HADOOP_HOME/etc/hadoop/hadoop-env.sh && \
    mv /tmp/hdfs-site.xml $HADOOP_HOME/etc/hadoop/hdfs-site.xml && \ 
    mv /tmp/core-site.xml $HADOOP_HOME/etc/hadoop/core-site.xml && \
    mv /tmp/mapred-site.xml $HADOOP_HOME/etc/hadoop/mapred-site.xml && \
    mv /tmp/yarn-site.xml $HADOOP_HOME/etc/hadoop/yarn-site.xml && \
    mv /tmp/slaves $HADOOP_HOME/etc/hadoop/slaves && \
    mv /tmp/start-hadoop.sh ~/start-hadoop.sh && \
    mv /tmp/run-wordcount.sh ~/run-wordcount.sh
```



### 启动脚本

- 集群配置脚本 start-cluster.sh

  ```shell
  #!/bin/bash
  
  # 同步配置文件
  rsync -rtv /root/docker_test/hadoop_cluster_test hdfsslave3:/root/docker_test
  
  rsync -rtv /root/docker_test/hadoop_cluster_test hdfsslave4:/root/docker_test
  
  # 调用容器启动脚本
  cd /root/docker_test/hadoop_cluster_test
  . start-container.sh
  
  ssh hdfsslave3 "cd /root/docker_test/hadoop_cluster_test; chmod +x start-container.sh; . start-container.sh"
  
  ssh hdfsslave4 "cd /root/docker_test/hadoop_cluster_test; chmod +x start-container.sh; . start-container.sh"
  
  # 进入集群
  docker exec -it hadoop-master /bin/bash
  ```

- 启动容器脚本 start-container.sh

  ```shell
  #!/bin/bash
  
  # 配置集群ip
  master_ip=192.168.106.58
  slave1_ip=192.168.106.68
  slave2_ip=192.168.106.69
  
  # 获得当前主机ip
  ip=`ifconfig |grep 192.168.106.*|awk '{print $2}'`
  
  echo $ip
  
  # 在各自的主机上创建镜像，启动容器
  if [ $ip == $master_ip ]
  then
      # 制作镜像
      docker build -t hadoop:v1 .
      # 启动容器
      docker run -it -d \
          -p 60022:22 -p 60080:8080 -p 60088:8088 -p 60070:50070 \
          -p 18080:18080 -p 60040:4040\
          --name hadoop-master \
          --hostname hadoop-master \
          -m 5G  --net bigdata --cpuset-mems="1" \
          --privileged \
          -v hdfs-m:/bigdata/hadoop/hadoop-2.7.7/tmp \
          hadoop:v1 \
          init
          
  elif [ $ip == $slave1_ip ]
  then
      # 制作镜像
      docker build -t hadoop:v1 .
      # 启动容器
      docker run -it -d \
          -p 60022:22 -p 60075:50075 -p 60042:8042 -p 60081:8081 \
          --name hadoop-slave1 --hostname hadoop-slave1 \
          -m 20G --net bigdata --cpuset-mems="1" \
          --privileged \
          -v hdfs-1:/bigdata/hadoop/hadoop-2.7.7/tmp \
          hadoop:v1 \
          init
          
  elif [ $ip == $slave2_ip ]
  then
      # 制作镜像
      docker build -t hadoop:v1 -f /root/docker_test/hadoop_cluster_test/Dockerfile .
      # 启动容器
      docker run -it -d \
          -p 60022:22 -p 60075:50075 -p 60042:8042 -p 60081:8081 \
          --name hadoop-slave2 --hostname hadoop-slave2 \
          -m 75G --net bigdata --cpuset-mems="1" \
          --privileged \
          -v hdfs-2:/bigdata/hadoop/hadoop-2.7.7/tmp \
          hadoop:v1 \
          init
  else 
      echo "请在制定的主机上进行安装"
  fi
  ```



### 重用volume

如果需要重启一个hdfs的容器，并且需要使用老集群在volume中产生的数据的时候，就是用下面的命令，主要就是去掉最后的init，然后将-v参数修改为--mount参数，如下所示：

```shell
docker run -it -d \
  -p 60022:22 -p 60080:8080 -p 60088:8088 -p 60070:50070 \
  -p 18080:18080 -p 60040:4040\
  --name new_spark-master \
  --hostname new_spark-master \
  -m 5G  --net bigdata --cpuset-mems="1" \
  --privileged \
  --mount          type=bind,source=/var/lib/docker/volumes/namenode,target=/root/hdfs/namenode \
  hadoop:v1
```







# 第九章 其他

## docker compose 

安装

```shell
sudo curl -L https://github.com/docker/compose/releases/download/1.25.5/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose
```





## sublime text 2

按ctrl+shift+p 输入 view， 点击 View:Toggle Menu 就可以显示菜单。






























电子书地址： <https://yeasy.gitbook.io/docker_practice/image/pull>

# 第一章 Apache 软件下载地址

## 一、 Apache官网

<https://archive.apache.org/dist/>

## 二、 镜像

### 2.1 北京理工大学镜像

http://mirror.bit.edu.cn/apache/hadoop/common/

### 2.2 北京外国语大学镜像

https://mirrors.bfsu.edu.cn/apache/

### 2.3 清华大学镜像

https://mirrors.tuna.tsinghua.edu.cn/apache/hive/

##三、系统版本

Hadoop 2.7.7

Spark 2.4.7

jdk1.8.0_261

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

配置以下内容（当然也不用真配这么多，选几个就行了，我这里是把我知道的都列出来了）：

```json
{
  "registry-mirrors":[
     "https://hub-mirror.c.163.com",
     "https://mirror.baidubce.com"，
     "https://docker.mirrors.ustc.edu.cn",
     "https://registry.docker-cn.com",
     "https://dockerhub.azk8s.cn",
     "https://reg-mirror.qiniu.com",
     "https://hub-mirror.c.163.com"
  ],
  "experimental":true
}
```

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

```shell
docker image ls -f dangling=true
```

#### (2) 删除所有的虚悬镜像

```shell
docker image prune
```

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

#### (9) 退出不关闭容器

```shell
# 简略信息
docker system df
# 详细信息
 docker system df -v
```

#### (10) 磁盘清理

`docker system prune -a`

该命令清除掉所有没有被容器使用的docker网络、volume、虚悬镜像、未被使用的镜像和已经停止的容器。



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
COPY hadoop-2.7.7.tar.gz /root
COPY jdk-8u261-linux-x64.tar.gz /root
COPY spark-2.4.7-bin-hadoop2.7.tgz /root
COPY scala-2.12.8.tgz /root
COPY hbase-1.4.13-bin.tar.gz /root

# install hadoop-2.7.7  jdk1.8.0_261  scala-2.12.8  spark-2.4.7-bin-hadoop2.7
RUN mkdir /bigdata && \
    tar -xzvf hadoop-2.7.7.tar.gz -C /bigdata && \
    tar -xzvf jdk-8u261-linux-x64.tar.gz -C /bigdata && \
    tar -xzvf spark-2.4.7-bin-hadoop2.7.tgz -C /bigdata && \
    tar -xzvf scala-2.12.8.tgz -C /bigdata && \
    tar -xzvf hbase-1.4.13-bin.tar.gz -C /bigdata && \
    rm hadoop-2.7.7.tar.gz && \
    rm jdk-8u261-linux-x64.tar.gz && \
    rm spark-2.4.7-bin-hadoop2.7.tgz && \
    rm scala-2.12.8.tgz

# set environment variable
ENV JAVA_HOME=/bigdata/jdk1.8.0_261
ENV HADOOP_HOME=/bigdata/hadoop-2.7.7
ENV SCALA_HOME=/bigdata/scala-2.12.8
ENV HBASE_HOME=/bigdata/hbase-1.4.13
ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$SCALA_HOME/bin:$HBASE_HOME/bin

# ssh without key
RUN ssh-keygen -t rsa -f ~/.ssh/id_rsa -P '' && \
    cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

RUN mkdir -p ~/hdfs/namenode && \
    mkdir -p ~/hdfs/datanode && \
    mkdir $HADOOP_HOME/logs

COPY config/* /tmp/

RUN mv /tmp/ssh_config /etc/ssh/ssh_config && \
    mv /tmp/hadoop-env.sh $HADOOP_HOME/etc/hadoop/hadoop-env.sh && \
    mv /tmp/hdfs-site.xml $HADOOP_HOME/etc/hadoop/hdfs-site.xml && \
    mv /tmp/core-site.xml $HADOOP_HOME/etc/hadoop/core-site.xml && \
    mv /tmp/mapred-site.xml $HADOOP_HOME/etc/hadoop/mapred-site.xml && \
    mv /tmp/yarn-site.xml $HADOOP_HOME/etc/hadoop/yarn-site.xml && \
    mv /tmp/slaves $HADOOP_HOME/etc/hadoop/slaves && \
    mv /tmp/hbase-env.sh $HBASE_HOME/conf/hbase-env.sh && \
    mv /tmp/hbase-site.xml $HBASE_HOME/conf/hbase-site.xml && \
    mv /tmp/start-hadoop.sh ~/start-hadoop.sh && \
    mv /tmp/run-wordcount.sh ~/run-wordcount.sh && \
    mv /tmp/start-ssh.sh ~/start-ssh.sh && \
    mv /tmp/everything-start.sh ~/everything-start.sh && \
    chmod +x start-ssh.sh && \
    chmod +x everything-start.sh
```

**得记得set ff=unix**





# 第五章 docker集群配置

## 一、基础环境

### 1. 宿主机

```shell
192.168.48.59  (Bussines2.0-hdfsslave2)  
```

### 2. centos镜像

```shell
docker pull centos:7.8.2003
```



## 二、 配置hadoop伪分布模式

### 1. 配置java

```shell
vim hadoop-2.7.7/etc/hadoop/hadoop-env.sh
export JAVA_HOME=/bigdata/java/jdk1.8.0_261
```



### 2. hdfs-site.xml

```xml
<!-- hadoop-2.7.7/etc/hadoop/hdfs-site.xml -->
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
  <value>1</value>
</property>
```

创建HDFS文件存储路径

```shell
mkdir -p /root/hdfs/namenode
mkdir -p /root/hdfs/datanode
```

### 3. core-site.xml

```xml
<!--配置HDFS主节点的地址，就是NameNode的地址-->
<!--9000是RPC通信的端口-->
<property>  
    <name>fs.defaultFS</name>
    <value>hdfs://spark-master:9000</value>
</property> 

<!--HDFS数据块和元信息保存在操作系统的目录位置-->
<!--默认是Linux的tmp目录,一定要修改-->
<!--<property>  
    <name>hadoop.tmp.dir</name>
    <value>/bigdata/hadoop/hadoop-2.7.7/tmp</value>
</property>-->
```



### 4. mapred-site.xml

```shell
cp mapred-site.xml.template mapred-site.xml
```

```xml
<property>  
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
</property> 
```



### 5. yarn-site.xml

```xml
<?xml version="1.0"?>
<configuration>
    <!--配置Yarn主节点的位置-->
    <property>  
        <name>yarn.resourcemanager.hostname</name>
        <value>new-spark-master</value>
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

    <!-- 内存-->
    <property>
        <name>yarn.scheduler.minimum-allocation-mb</name>
        <value>1024</value>
    </property>

    <property>
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>6144</value>
    </property>

    <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>16384</value>
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

    <property>
        <name>yarn.log-aggregation-enable</name>
        <value>true</value>
    </property>

    <property>
        <name>yarn.log-aggregation.retain-seconds</name>
        <value>259200</value>
    </property>

    <property>
        <name>yarn.log-aggregation.retain-check-interval-seconds</name>
        <value>300</value>
    </property>

    <property>
        <name>yarn.nodemanager.local-dirs</name>
        <value>/yarnLog</value>
    </property>

    <property>         
        <name>yarn.resourcemanager.nodes.exclude-path</name>        
        <value>/bigdata/hadoop-2.7.7/etc/hadoop/yarn.exclude</value>
        <final>true</final>   
    </property>
</configuration>
```



### 6. 格式化namenode

```shell
hdfs namenode -format 
```



## 三、 配置spark伪分布模式

```shell
mv conf/spark-env.sh.template conf/spark-env.sh  
vim spark-env.sh  
```

在spark-env.sh中配置以下内容：

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

其他文件与伪分布模式文件配置相同

### 1. hdfs-site.xml

```xml
<!-- hadoop-2.7.7/etc/hadoop/hdfs-site.xml -->
<property>  
    <name>dfs.replication</name>
    <!--这里和从节点个数保持一致，但最好不要超过3-->
    <value>2</value>
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



### 2. slaves

```shell
# 伪分布模式
127.0.0.1 
# 如果配置全分布模式，就删掉127.0.0.1，配置为从节点的主机名
<slave主机名>
```





## 五、docker swarm 配置多容器通信

```shell
# master 初始化swarm集群
docker swarm init \ 
 --advertise-addr 192.168.106.58 # 如果主机有多个ip，则需要用该参数指定要用的那个

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



## 七、hive





# 第六章 部署

## 1. 容器启动命令

```shell
docker run -it -d \
  -p 60022:22 -p 60080:8080 -p 60088:8088 -p 60070:50070 \
  -p 18080:18080 -p 60040:4040\
  --name spark-master \
  --hostname spark-master \
  -m 5G  --net bigdata --cpuset-mems="1" \
  --privileged \
  -v hdfs-m:/bigdata/hadoop/hadoop-2.7.7/tmp \
  spark-cluster:v3 \
  /usr/sbin/init

docker run -it -d \
  -p 60022:22 -p 60075:50075 -p 60042:8042 -p 60081:8081 \
  --name spark-slave1 --hostname spark-slave1 \
  -m 20G --net bigdata --cpuset-mems="1" \
  --privileged \
  -v hdfs-1:/bigdata/hadoop/hadoop-2.7.7/tmp \
  spark-cluster:v3 \
  /usr/sbin/init
  
docker run -it -d \
  -p 60022:22 -p 60075:50075 -p 60042:8042 -p 60081:8081 \
  --name spark-slave2 --hostname spark-slave2 \
  -m 75G --net bigdata --cpuset-mems="1" \
  --privileged \
  -v hdfs-2:/bigdata/hadoop/hadoop-2.7.7/tmp \
  spark-cluster:v3 \
  /usr/sbin/init
  
  
# 自己用来研究docker-compose的
docker run -it -d \
  -p 30022:22 -p 30080:8080 -p 30088:8088 -p 30070:50070 \
  -p 28080:18080 -p 30040:4040\
  -p 30075:50075 -p 30042:8042 -p 30081:8081 \
  --name compose --hostname compose \
  -m 75G --net bigdata --cpuset-mems="1" \
  -v /var/run/docker.sock:/var/run/docker.sock dockerindocker:1.0 \
  --privileged \
  spark-cluster:v3 \
  /usr/sbin/init # 最后这句可以让容器使用systemctl命令
  
 
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





# 第七章 Docker集群一键启动

Docker集群一件启动包括三部分：

1. Docker镜像构建文件 Dockerfile
2. Hadoop系列组件配置文件和安装包
3. 集群启动脚本

**注意事项：**

1. 使用集群启动脚本之前，需要现在各个宿主机上配饰docker swarm，配置docker网络，参见第六章第五节。

2. 集群启动脚本运行完毕之后，如果需要配置ssh免密码登陆，需要自己手动配置。

3. 启动集群时，可以使用 `~` 目录下的`everything-start.sh`脚本。

## 一、集群Dockerfile

```shell
FROM centos:7.8.2003

MAINTAINER ironwing <chningwing@163.com>

WORKDIR /root

# 安装常用工具
RUN yum install -y openssh-server wget which vim lrzsz less openssh-clients initscripts net-tools rsync

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
    mkdir $HADOOP_HOME/logs && \
    mkdir /root/hive_tmp_data

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
    mv /tmp/profile /etc/profile && \
    mv /tmp/hive $HIVE_HOME/bin && \
    chmod +x $HIVE_HOME/bin/hive && \
    mv /tmp/start-hadoop.sh /root

```

## 二、启动脚本

### 2.1 集群配置脚本 

start-cluster.sh

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

### 2.2 启动容器脚本 

start-container.sh

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

    #docker volume create \
      --opt type=tmpfs \
      --opt device=tmpfs \
      --opt o=size=1G,uid=1000 \
      mysql

     启动mysql容器
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



## 三、准备工作

### 3.1 配置ssh

`start-cluster.sh`脚本运行结束后会进入到`spark-master`容器内部，此时需要先手动配置`spark-master、spark-slave1和spark-slave2`之间的`ssh`免密码登录。

接着启动`sshd`服务，命令：`/usr/sbin/sshd -D &` 。

### 3.2 启动集群

然后执行`/root/start-hadoop.sh`脚本，具体内容如下：

```shell
#!/bin/bash
echo "格式化namenode\n"
hdfs namenode -format
echo -e "\n"

echo "启动hadoop集群\n"
start-all.sh
echo -e "\n"

echo "启动spark集群\n"
$SPARK_HOME/sbin/start-all.sh
echo -e "\n"

echo "初始化hive\n"
schematool -initSchema -dbType mysql
echo -e "\n"
```





## 四、 docker容器升级

### 3.1 重用volume

如果需要重启一个hdfs的容器，并且需要使用老集群在volume中产生的数据的时候，就是用下面的命令，主要就是去掉最后的init，然后将-v参数修改为--mount参数，如下所示：

```shell
docker run -it -d \
    -p 60022:22 -p 60080:8080 -p 60088:8088 -p 60070:50070 \
    -p 18080:18080 -p 60040:4040 -p 60020:8020\
    --name new_spark-master --hostname new_spark-master \
    -m 5G  --net bigdata --cpuset-mems="1" \
    --mount type=bind,source=/var/lib/docker/volumes/hdfs-m,target=/root/hdfs/namenode \
    hadoop:v1 
```



## 五、hive数据迁移

做hive数据迁移之前，需要把宿主机的ssh公钥拷贝到容器主节点中。迁移过程中用到的几个脚本如下：

### 5.1. 全量数据迁移

`full_migration_hive_data.sh`

```shell
#!/bin/bash

# 从配置文件中获取数据表的名称以及容器ip
. table_name.properties
table_names=$table_name
DEST_IP=$docker_master_ip

# 将表名串拆分为数组
OLD_IFS="$IFS" #保存原有的分隔符
IFS=","
table_name_array=($table_names)
IFS="$OLD_IFS"

# ====== 子程序 ======
transfer_data(){
    #DEST_IP=172.18.0.4
    #TABLE_NAME=t_user_bill_info

    # t_user_bill_info t_user_log_info
    echo "===================得到hive的表结构========================="
    hive -e "SHOW CREATE TABLE $1" | \
            sed "/'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'/d" | \
            sed "s/ROW FORMAT SERDE/ROW FORMAT DELIMITED FIELDS TERMINATED BY '\\\t'/g" | \
            sed "s/master:8020/spark-master:9000/g" > \
            /root/docker_test/hive_data_migration/tmp_data/tablesDDL_full.txt


    echo "================将hive中的数据导出到本地====================="
    last_day="`date -d 'yesterday' +%Y-%m-%d`"
    hive -e "SELECT * FROM $1" > /root/docker_test/hive_data_migration/tmp_data/hive_date_full.csv

    echo "==============利用scp将数据传输到容器======================="
    scp /root/docker_test/hive_data_migration/tmp_data/tablesDDL_full.txt root@$DEST_IP:/root/hive_tmp_data/tablesDDL_full.txt
    scp /root/docker_test/hive_data_migration/tmp_data/hive_date_full.csv root@$DEST_IP:/root/hive_tmp_data/hive_date_full.csv


    echo "===================连接到容器端========================="
    ssh $DEST_IP  <<EOF

    echo "==在容器端hive中创建表=="
    hive -f /root/hive_tmp_data/tablesDDL_full.txt

    echo "==在容器端导入数据=="
    last_day="`date -d 'yesterday' +%Y-%m-%d`"
    hive -e "load data local inpath '/root/hive_tmp_data/hive_date_full.csv' into table $1 partition(day='$last_day');"

    echo "==退出容器=="
    exit
EOF
    echo done!
}

# 循环遍历表名数组
for table_name in ${table_name_array[*]}; do
        echo $table_name
        # 调用hive迁移表数据子程序
        transfer_data $table_name
done
```



### 5.2增量数据迁移

`increm_migration_hive_data.sh`

```shell
#!/bin/bash

# 从配置文件中获取数据表的名称以及容器ip
. table_name.properties
table_names=$table_name
DEST_IP=$docker_master_ip

# 将表名串拆分为数组
OLD_IFS="$IFS" #保存原有的分隔符
IFS=","
table_name_array=($table_names)
IFS="$OLD_IFS"

# ======= 子程序 =======
transfer_data(){

    echo "=================将hive中的数据导出到本地===================="
    last_day="`date -d 'yesterday' +%Y-%m-%d`"
    hive -e "SELECT * FROM $1 WHERE day LIKE '%$last_day%'" > /root/docker_test/hive_data_migration/tmp_data/hive_date_increase.csv

    echo "==============利用rsync将数据传输到容器====================="
    rsync -auz -e 'ssh -p 22' /root/docker_test/hive_data_migration/tmp_data/hive_date_increase.csv root@$DEST_IP:/root/hive_tmp_data/hive_date_increase.csv

    echo "===================连接到容器端========================="
    ssh $DEST_IP  <<EOF

    echo "==在容器端导入数据=="
    last_day="`date -d 'yesterday' +%Y-%m-%d`"
    hive -e "load data local inpath '/root/hive_tmp_data/hive_date_increase.csv' into table $1 partition(day='$last_day');"

    echo "==退出容器=="
    exit    
EOF
    echo done!
}

# 循环遍历表名数组
for table_name in ${table_name_array[*]}; do
    echo $table_name
    # 调用hive迁移表数据子程序
    transfer_data $table_name
done
```



### 5.3 配置文件

table_name.properties

```shell
table_name=t_user_bill_info
docker_master_ip=172.18.0.4
```





# 第九章 hadoop distcp工具

## 一、总览

DistCp （分布式拷贝）是一个用来进行数据拷贝的工具，不同的是，这个命令通常是在大规模集群内部和大规模集群之间使用。DistCp命令的拷贝过程本质依然是MapReduce任务，它通过MR的方式来实现拷贝过程中的数据分发、错误处理以及报告。

该命令将文件和目录的列表作为map任务的输入，每个map任务都会复制原列表中指定路径下的文件。

早期版本的DistCp在用法、扩展性和性能上还存在一些不足，因此新版本对其进行了重构。此外新版本引入了新的规范，该命令的运行时和设置性能得到了提升，但是默认行为依然为旧规范。

该文档的目的是描述新版本DistCp的设计、新的特点、最佳实践以及与之前版本的不同。

## 二、使用

### 2.1 基础使用

DistCp命令最常用的调用方式是在集群之间进行数据拷贝：

```shell
hadoop distcp hdfs://nn1:8020/foo/bar hdfs://nn2:8020/bar/foo
```

该命令会将`nn1`下`/foo/bar`目录扩展为一个临时文件，并将其中的内容分配给不同的`map`任务，并在`nn1`和`nn2`上的`nodemanager`上启动`copy`任务。

同时也可以在命令行中配置多个源目录：

```shell
hadoop disctp hdfs://nn1:8020/foo/a \ 
              hdfs://nn1:8020/foo/b \
              hdfs://nn2:8020/bar/foo
```

或者也可以使用-f参数从文件中读入源目录：

```shell
hadoop distcp -f hdfs://nn1:8020/srclist \
                 hdfs://nn2:8020/bar/foo
```

srclist文件中的内容为：

```shell
hdfs://nn1:8020/foo/a
hdfs://nn1:8020/foo/b
```

当从多个原路径进行复制的时候，如果出现了路径冲突，DistCp会停止当前的拷贝任务并打印一条错误信息，冲突可以通过命令的特定选项解决。默认情况下已经存在目标集群上的文件会被跳过（不会被源文件替换）。任务结束时会报告跳过的文件数，但是如果复制任务在文件的某些子集上失败了，并在之后的尝试中又成功的话，这个数据可能会不准确。

数据复制过程中很重要的一点是，需要保证NodeManager能够连接目标文件系统和源文件系统并且保持通信。对于HDFS而言，源系统和目标系统必须运行相同的版本或者版本可以向下兼容。[不同版本间的文件拷贝参考附录][ 1]。

复制之后，建议生成一个用来交叉验证源和目标的列表，检查复制是否真的成功了。因为DistCp同时使用了Map/Reduce和FileSystem的API，这三个部分中的任何一个出现问题都会对复制过程产生破坏性的，无法追溯的影响。一些情况下利用-update参数可以在第二遍copy时成功执行，但是使用者需要在使用该参数之前搞清楚具体的含义。

需要注意的是，如果另一个客户端在向源文件写入内容时，整个复制过程也会失败。尝试在HDFS上复写一个已经在目标路径写入的文件也应该失败。如果源文件在复制之前被删除了，复制过程会报`FileNotFoundException`。

### 2.2 更新和复写

`-update`是用从源路径复制目标路径没有的或者与目标路径下版本不同的文件。

`-overwrite`用来复写在目标路径下已经存在的文件。

update和overwrite选项需要特别注意，因为他们处理源路径的方式默认方式有一些细微的差别。考虑从`/source/first`和 `/source/second/` 拷贝文件到`/target/`，源路径下包括以下内容：

```shell
hdfs://nn1:8020/source/first/1
hdfs://nn1:8020/source/first/2
hdfs://nn1:8020/source/second/10
hdfs://nn1:8020/source/second/20
```

如果调用DistCp命令时没有加-update或者-overwrite参数，DistCp命令默认情况下会在/target目录下创建first/ 和 second/ 目录。因此以下命令：

```shell
distcp hdfs://nn1:8020/source/first  \
       hdfs://nn1:8020/source/second \
       hdfs://nn2:8020/target
```

会在/target目录下产生以下内容：

```shell
hdfs://nn2:8020/target/first/1
hdfs://nn2:8020/target/first/2
hdfs://nn2:8020/target/second/10
hdfs://nn2:8020/target/second/20
```

而当加入了-update参数或者-overwrite参数时，只会将源路径下的内容拷贝到目标路径下，并不会创建源路径的目录，因此：

```shell
distcp -update hdfs://nn1:8020/source/first  \
               hdfs://nn1:8020/source/second \
               hdfs://nn2:8020/target
```

将会在目标路径/target下创建如下内容：

```shell
hdfs://nn2:8020/target/1
hdfs://nn2:8020/target/2
hdfs://nn2:8020/target/10
hdfs://nn2:8020/target/20
```

通过扩展，如果两个源文件夹包含了相同名称的文件，那么两个源文件都会映射到目标文件夹下，这样就会产生冲突，而DistCp会阻止这种情况产生。

接下来看这个例子：

```shell
distcp hdfs://nn1:8020/source/first  \
       hdfs://nn1:8020/source/second \
       hdfs://nn2:8020/target
```

源文件的目录及文件大小

```shell
hdfs://nn1:8020/source/first/1 32
hdfs://nn1:8020/source/first/2 32
hdfs://nn1:8020/source/second/10 64
hdfs://nn1:8020/source/second/20 32
```

目标文件夹目录及文件大小

```shell
hdfs://nn2:8020/source/first/1 32
hdfs://nn2:8020/source/second/10 32
hdfs://nn2:8020/source/second/20 64
```

执行命令之后，目标文件夹目录

```shell
hdfs://nn2:8020/target/1 32
hdfs://nn2:8020/target/2 32
hdfs://nn2:8020/target/10 64
hdfs://nn2:8020/target/20 32
```

可以看到1被跳过了，因为文件长度和内容都匹配；2是复制过来的，因为目标路径下没有这个文件；10和20被复写了，因为文件长度不匹配。

如果使用了-update参数，那么1也会被复写。



## 三、命令行选项

| 标记                                | 描述                                                         | 注意事项                                                     |
| :---------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| **-p[rbugpcaxt]**                   | **r：** replication <br/>**b：** block size     <br/>**u：** user<br/>**g：** group<br/>**p：** permission<br/>**c：** checksum-type<br/>**a：** ACL<br/>**x：** XAttr<br/>**t：** timestamp | 当使用`-update`选项时，只有当文件大小不同时才会同步文件状态。如果指定了`-pa`选项，`DistCp`还是会保留权限，因为`ACLs`是权限的超集 |
| **-i**                              | 忽略错误                                                     | 正如附录中所说的，该选项比默认情况下，对复制的统计结果更准确。该选项同样同样会从失败的拷贝中保留日志，这对于`debug`非常有帮助。最后，在所有的分裂策略都尝试过之前，一个`map`任务的失败并不会导致整个`job`失败。 |
| **-log** \<logdir\>                 | 向logdir中写入日志                                           | DistCp为每个尝试复制的文件保留一份日志，该文件就是map的输出。如果map任务失败了，当该任务重启的时候，原日志文件不会保留。 |
| **-m**<num_maps>                    | 最大同时复制数                                               | 确定复制数据的map数量。需要注意的是map数量的增多并不会提高系统的吞吐量。 |
| **-overwrite**                      | 复写目标路径                                                 | 如果映射失败，并且没有指定-i参数，拆分中的所有文件（包括复制失败的文件）都会被重新复制。如之前所述，该标记会改变源目录复制到目标目录的文件路径，因此使用的时候需要小心。 |
| **-update**                         | 如果源目录文件和目标目录文件的大小、块大小以及检查数不同就会复写。 | 前文提到过，该命令并不是同步操作。利用源和目标文件的大小、块大小以及检查数作为检验标准，如果这三个标准的值不同，就用源文件替换目标文件。就如前文讨论的，该标记也会改变源路径的路径描述，因此使用的时候需要非常小心。 |
| **-append**                         | 对名称相同但长度不同的文件进行增量拷贝。                     | 如果源文件的长度比目标文件长，就比较公共部分的校验和。如果校验和相等，就使用读和追加的方式拷贝不同的部分。-append选型仅和-update一起使用，没有-skipcrccheck。 |
| **-f<urilist_url>**                 | 使用urilist_uri中的文件列表作为源列表                        | 这个与在命令行列出所有的路径效果是一样的，urilist_uri中的路径应该为绝对路径。 |
| **-filters**                        | 文件路劲更包含一系列模式字符串，一个字符串一行，与该模式匹配的文件路径将从复制过程中排除。 | 支持由java.tuil.regex.Pattern指定的正则表达式。              |
| **-delete**                         | 删除存在于目标路径下但是源路径下不存在的文件。               | 删除操作是由FS Shell完成的，因此如果启用了垃圾桶就会用到。   |
| **-strategy{dynamic\|uniformsize}** | 选择DistCp的复制策略。                                       | 默认情况下，使用uniforsize                                   |
| **-bandwidth**                      | 为每个map任务设置带宽，单位是MB/s                            | 每个map任务都会被限制只能使用特定的带宽。这个数值也不是绝对的。map任务在复制的过程中会逐渐限制自身带宽的使用量，整个网络的带宽使用会逐渐趋向设定的值。 |
| **-atomic {-tmp\<tmp_dir>}**        | 在指定的路径下进行原子提交。                                 | -atomic指定DistCp将元数据拷贝到临时目标位置，然后将数据整体原子式的提交到目标位置。结果就是数据要么整体都在目标位置，要么都不在。可选参数-tmp可以指定临时文件的位置。如果没有指定，就按默认值来。需要注意的是：tmp_dir必须在目标集群上。 |
| **-mapredSslConf <ssl_conf_file>**  | 指定要与HSFTP一起使用的SSL配置文件                           | 当hsftp协议和源一起使用的时候，可以在配置文件中指定安全相关属性，然后传递给DistCp。ssl_conf_file需要配置在类路径中。 |
| **-async**                          | 异步运行DistCp，启动hadoop的Job之后立刻退出。                | 会打印Hadoop的Job-id，方便追踪                               |
| **-diff**                           | 使用快照差异报告比较源和目标之间的差异。                     |                                                              |
| **-skipcrccheck**                   | 是否跳过源和目标之间的crc校验。                              |                                                              |






































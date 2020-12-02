

利用shell命令替换文本内容

sed命令

```shell
sed -* '*/*/*' **.txt
# 第一个*为命令行选项，有 -i -u
-n --quiet --silent 表示一个意思，不打印修改的结果
sed -n '/PATTERN/p' file
-f 将多条sed命令存储在文件里
-v -version 打印目前使用的sed版本
不常用就不说了
sed "/'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'/d" tablesDDL.txt
表示删除tablesDDL.txt内和两个斜杠中间内容匹配的内容。

sed "s/ROW FORMAT SERDE/ROW FORMAT DELIMITED FIELDS TERMINATED BY '\\\t'/g" > \
替换命令，s表示substitution替换。
表示替换，替换第二部分内容为第三部分内容，g表示全局搜索，
```







shell执行远程命令

简单命令

ssh  user@remoteNode "cd /home; ls"

如果需要远程执行较多的命令，可以考虑用脚本方式实现

```shell
#!/bin/bash
ssh user@remoteNode > /dev/null 2>&1 << eeooff
cd /home
touch abcdefg.txt
exit
eeooff
echo done!
```

  远程执行的内容在“<< eeooff ” 至“ eeooff ”之间，在远程机器上的操作就位于其中，注意的点：

1. << eeooff，ssh后直到遇到eeooff这样的内容结束，eeooff可以随便修改成其他形式。
2. 重定向目的在于不显示远程的输出了
3. 在结束前，加exit退出远程节点



ssh连接，要用A机器连接B机器的话，就要把A机器的公钥拷贝到B机器的authorized_keys，可以手工复制，也可以利用ssh-copy-id的方式。

ssh-copy-id [root@172.17.0.4](mailto:root@172.17.0.4) # 需要输入密码（默认公钥）
ssh-copy-id -i ~/.ssh/id_rsa_yangan [root@172.17.0.4](mailto:root@172.17.0.4) # 复制自定义公钥



Permission denied (publickey,gssapi-keyex,gssapi-with-mic) 解决方法

sudo vim /etc/ssh/sshd_config
增加如下修改
PasswordAuthentication yes

如果需要用root用户登录，如下设置即可

PermitRootLogin yes



通过dockerfile修改账号密码：

RUN echo "ming:xxxxx" | chpasswd





Hive能正常执行任务，但出现“WARN: Establishing SSL connection without server’s identity verification is not recommended.”告警，翻译过来就是“不建议不使用服务器身份验证建立SSL连接。”



1.设置`useSSL=false`
这里有个坑就是hive的配置文件是`.XML`格式，而**在xml文件中&amp；才表示&**，所以正确的做法是在Hive的配置文件中，如`hive-site.xml`进行如下设置

```xml
  <property>
    <name>javax.jdo.option.ConnectionURL</name>
    <value>jdbc:mysql://localhost:3306/hive?createDatabaseIfNotExist=true&amp;useSSL=false</value>
    <description>JDBC connect string for a JDBC metastore</description>
  </property>
```



- hive实例化schema报错： Failed to get schema version.

查看hive-site.xml的javax.jdo.option.ConnectionURL设置的url是否正确



- Error: Duplicate key name 'PCS_STATS_IDX' (state=42000,code=1061)

以下异常说明mysql已经启动。 应先关掉先前启动的mysql.再执行初始化schema操作。

或者看mysql中是否已经有hive这个表了，有的话删掉就好了。



- 初次启动hive,解决 ls: cannot access /home/hadoop/spark-2.2.0-bin-hadoop2.6/lib/spark-assembly-*.jar: No such file or directory问题

spark升级到spark2以后，原有lib目录下的大JAR包被分散成多个小JAR包，原来的spark-assembly-*.jar已经不存在，所以hive没有办法找到这个JAR包。

打开hive的安装目录下的bin目录，找到hive文件

```shell
cd $HIVE_HOME/bin
vi hive
```

找到

```shell
sparkAssemblyPath=`ls ${SPARK_HOME}/lib/spark-assembly-*.jar`
```

修改为

```shell
sparkAssemblyPath=`ls ${SPARK_HOME}/jars/*.jar`
```

即可





docker容器开启ssh服务连接远程

先安装基本的

yum install -y net-tools

接着安装openssl，openssh-server

```bash
yum install -y openssl openssh-server
```

然后启动ssh

```bash
/usr/sbin/sshd -D
```

这里会报错

```bash
[root@68e7598797d7 /]# /usr/sbin/sshd -D
Could not load host key: /etc/ssh/ssh_host_rsa_key
Could not load host key: /etc/ssh/ssh_host_ecdsa_key
Could not load host key: /etc/ssh/ssh_host_ed25519_key
```

进行下面的设置

```bash
ssh-keygen -q -t rsa -b 2048 -f /etc/ssh/ssh_host_rsa_key -N ''  
ssh-keygen -q -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N ''
ssh-keygen -t dsa -f /etc/ssh/ssh_host_ed25519_key -N ''
```

然后进行如下的设置

vim /etc/ssh/sshd_config

1. 将 Port 22 前面的注释去掉（开启22号端口）

2. 将**PermitRootLogin** 的 no  改为 yes （这里是设置是否允许root用户登录，可根据自己需求决定是否开启） 

重新启动ssh

```bash
[root@68e7598797d7 /]# /usr/sbin/sshd -D &
```

注意，如果设置都没问题的话，命令结尾加个‘&’，自动后台运行





spark安装配置

2.2 修改配置文件

配置文件位于`/usr/local/bigdata/spark-2.4.3/conf`目录下。

(1) spark-env.sh

将`spark-env.sh.template`重命名为`spark-env.sh`。
 添加如下内容：

```bash
export SCALA_HOME=/usr/local/bigdata/scala
export JAVA_HOME=/usr/local/bigdata/java/jdk1.8.0_211
export HADOOP_HOME=/usr/local/bigdata/hadoop-2.7.1
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
SPARK_MASTER_IP=Master
SPARK_LOCAL_DIRS=/usr/local/bigdata/spark-2.4.3
SPARK_DRIVER_MEMORY=512M
```

(2)slaves

将`slaves.template`重命名为`slaves`
修改为如下内容：

```bash
Slave01
Slave02
```



ssh指定端口连接

ssh 到指定端口 ssh -p xx user@ip xx 为 端口号 user为用户名 ip为要登陆的ip







sed命令:删除匹配行和替换



删除以a开头的行

```cpp
sed -i '/^a.*/d' tmp.txt
```

-i 表示操作在源文件上生效.否则操作内存中数据,并不写入文件中.

在分号内的/d表示删除匹配的行

替换匹配行:

```cpp
sed -i 's/^a.*/haha/g' tmp.txt 
```

分号内的s/表示替换

/g表示全局替换







解决ssh连接docker容器环境变量无效的问题

在 /etc/profile中添加以下代码即可

```shell
for item in `cat /proc/1/environ |tr '\0' '\n'`
do
 export $item
done
```





ip addr add 127.0.0.1/8 dev lo brd + 是什么意思?

我知道：
ip addr add 127.0.0.1/8 dev lo 这是给lo网卡添加IP地址的，
但是 `brd +` 是什么意思？broadcast ADDRESS
----协议广播地址，可以简写成brd，此外可以简单的在后面加上"+"表示广播地址由协议地址加主机位全置1组成，"-"则表示主机位全置0。

例如你的配置：ip addr add 127.0.0.1/8 dev lo brd +
则表示广播地址为127.255.255.255，网络地址（前8位）为127，主机地址（后面的24位）全为1，加起来为广播地址。

扩展：
**ip address add---添加新的协议地址**
操作参数：
dev name
----指定要进行操作的网络设备名称
local ADDRESS (缺省)
----协议地址，地址的格式由使用的协议所决定，比如在ipv4协议中，地址的格式为用小数点分隔的四个十进制数，后面可以用/连接子网掩码的位数，比如192.168.1.100/24。
peer ADDRESS
----使用点对点连接时对端的协议地址。
broadcast ADDRESS
----协议广播地址，可以简写成brd，此外可以简单的在后面加上"+"表示广播地址由协议地址主机位全置1组成，"-"则表示主机位全置0。
label NAME
----地址标志，为了和linux 2.0中的别名相兼容，该标志由该网络设备名称开头，后面用"："接上地址名称，比如eth0:3等等。
scope SCOPE_VALUE
----地址范围，可能的值有：
1． global：说明该地址全局有效；
2． site：说明该地址只在本地站点内有效，该值只在ipv6中使用；
3． link：只在该网络设备上有效；
4． host：只在该主机上有效；
实例：
1． 添加回送地址
ip addr add 127.0.0.1/8 dev lo brd + scope host
2． 添加ip地址
ip addr add 10.0.0.1/24 brd + dev eth0 label eth0:3





Rsync使用非ssh默认端口从远程服务器同步文件到本地

```shell
rsync -avreH --progress 'ssh -p Port' root@remoteip:/remotepath/ /localpath/1
```

比如

```shell
rsync -avreH --progress 'ssh -p 1001' root@222.222.222.222:/data/backup/ ./
```





## linux的shm路径

linux的 /dev/shm 路径就是所谓的tmpfs，这是一个基于内存的文件系统，速度非常的块，因此可以使用该目录达到优化系统的目的。

该路径的容量可以动态调整，默认的最大空间是内存容量的一半。该目录下的文件不会被系统复写。

tmpfs有以下三个优势：

1. 可以动态的调整文件系统的大小
2. 速度非常快，因为典型的tmpfs文件系统会完全的主流在RAM中。
3. tmpfs中的数据重启后不会保留，因为虚拟内存的本质就是容易丢失的。



tmpfs是一种虚拟内存文件系统，



# 小知识
1. ip地址后面的斜杠24表示掩码位是24位的，即用32位二进制表示的子网掩码中有连续的24个“1”：11111111 11111111 11111111 00000000，将其转化为十进制，就是：255.255.255.0了。

2. brd + 
    broadcast ADDRESS ----协议广播地址，可以简写成brd，此外可以简单在后面加上“+”表示广播地址由协议地址加主机位全置1组成，“-”则表示主机位全置0.

假如你的配置：ip addr add 127.0.0.1/8 dev lo brd +
则表示广播地址为127.255.255.255，网络地址（前8位）为127，主机地址（后面的24位）全为1，加起来为广播地址。

扩展：
ip address add --添加新的协议地址
操作参数
dev name
----指定要进行操作的网络设备名称
local ADDRESS（缺省）
----协议地址，地址的格式由使用的协议所决定，比如在ipv4协议中，地址的格式为用小数点分割的四个十进制数，后面可以用/连接子网掩码的位数，比如192.168.1.100/24







### 蔺春满教的，怎么安装mysql

```shell
1、把mysql-5.7.18-linux-glibc2.5-x86_64.tar.gz解压放在/usr/local下
2、把mysqldata_empty_initiate_template.tar.gz解压放在/r2目录下，文件夹属主设置为mysql:mysql
3、把my.cnf拷贝到/etc下
4、直接启动 账号密码为 root  iforgot。 
/usr/local/mysql/bin/mysqld_safe --defaults-file=/etc/my.cnf &

/usr/local/mysql/bin/mysqld_safe --defaults-file=/etc/my.cnf &
这个要是没错误日志，就改成  /usr/local/mysql/bin/mysqld --defaults-file=/etc/my.cnf &

/usr/local/mysql/bin/mysql -uroot -piforgot

chown -R mysql:mysql ../mysqldata/

hadoop distcp hdfs://192.168.106.58:8020/tmp/test.txt hdfs://172.18.0.4:9000/tmp
```





#### mvn 打包命令

mvn clean package -Ppro





overlay 3808300120 97488280 3515472064 3% /r2/docker/overlay2/6031f0fa2***952cdd97607192b/merged

这个是容器的存储层，容器中处理的所有文件都在这个目录下面

shm  65536 0  65536 0% /r2/docker/containers/3d8635c***25b4b43a460ed/mounts/shm

/r2/docker/containers/ 路径下保存的是所有容器的具体的配置信息，
shm本身是一个linux内核管理的虚拟内存文件系统，因为是基于内存的，所以速度特别快，一般用来提高系统的运行速度。





vmware 15.5.2，附上密钥

链接：`https://pan.baidu.com/s/1f9jxVJ-oMy6ORiWZ_iLRqA`
提取码：`lyqz`
复制这段内容后打开百度网盘手机App，操作更方便哦

密钥：

```shell
YY5EA-00XDJ-480RP-35QQV-XY8F6

VA510-23F57-M85PY-7FN7C-MCRG0
```






























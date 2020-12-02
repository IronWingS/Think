# Docker集群一键启动说明文档

Docker集群一键启动包中有以下四部分内容：

1. Docker镜像构建文件 `Dockerfile`
2. Hadoop系列组件配置文件`config/ ` 和 安装包`dependencies/`
3. 集群启动脚本 `start-cluster.sh` `start-container.sh`
4. 集群环境配置文件 `configuration.properties`

**注意事项：**

1. 启动集群之**前**，请先按现场环境需要，设置 `configuration.properties`；
2. 启动集群之**前**，请先按文档，配置docker swarm，设置docker网卡；
3. 集群启动之**后**，需要先在每台机器上使用 `/usr/sbin/sshd -D & ` 启动ssh进程；
4. 容器集群启动**后**，需要在三台机器之间手动配置ssh免密码登录。
5. 如果需要使用hive，请按现场环境，修改 `$HIVE_HOME/conf/hive-site.xml`
6. `/root/everything-start.sh`是集群快速启动脚本，包括启动ssh进程、hadoop、spark以及初始化hive，可按需使用。



## 1 配置docker swarm

1. 在spark-master所在宿主机初始化swarm集群

```shell
docker swarm init --advertise-addr 192.168.7.83 # 如果主机有多个ip，则需要用--advertise-addr指定要用的那个
```

2. 执行该命令后会返回一长串token，也就是下面这条命令--token后的参数。在spark-slave1所在宿主机和spark-slave2所在宿主机执行下面这条命令。

```shell
# slave 加入swarm集群
docker swarm join --token SWMTKN-1-1iand4b3nj06w1z2bjurcj19ymh7xf4n7hj9xkuo9l02wq5r35-8vor2hookk6llgn8jvji1paqe 192.168.106.58:2377
```

**注意：需要将命令中的token替换为上一条命令中生成的那个**

3. 创建好swarm集群后，只需要在spark-master所在节点执行以下命令，创建一张docker用的网卡即可。

```shell
# 创建网络
docker network create --driver overlay --attachable bigdata
# 查看网络
docker network ls
```

**注意：请不要修改网卡的名称**



## 2 启动docker集群

配置好`configuration.properties`文件之后，直接执行`start-cluster.sh` 即可。
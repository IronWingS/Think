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












































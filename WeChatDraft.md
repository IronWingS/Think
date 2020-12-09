# 博客

从0开始，利用docker搭建一套大数据开发环境



天才第一步，雀氏纸尿裤。那么大数据第一步是什么呢？先搭建一套开发环境。

对于刚进入这个领域的同学来说，搭这套环境的过程可以学到很多东西，包括Linux的常用操作，如何设置配置文件，怎么使用linux的命令行，甚至，如果是在windows主机上使用虚拟机搭建环境的话，还能学到很多计算机网络等其他方面的基础知识。所以，亲手从0搭建一套大数据开发系统是非常有必要的。

但是呢，这个过程也是非常痛苦的，堪称新人入门的一道拦路虎，不少人的学习热情，就在这第一步被生生浇灭了。。。所以，为了再次照亮你心中最初的那道光，我开辟了这个系列。

我将在这一个系列中让你最快速的用上大数据系统，敲出自己的第一个hello world，并教你怎么样用一个命令，就能从0完全自动化的搭建一个有三个节点的分布式大数据系统。

当然了，我依然建议同学们亲自用apache的大数据组件搭一套系统出来，这样感受会更深刻。

好了，那开始吧。

问：最快用上大数据系统需要多少s？答：1s。

真的吗？当然了，不过需要借助一样工具，就是大名鼎鼎的docker。docker也叫容器，简单说就是一个小型的虚拟机，其他进一步的细节我们后面再介绍，首先就来安装一下吧。



我这里先讲如何在centos7的linux中安装，其他的如windows，Mac等之后再写文章吧。



centos7中安装docker首先要配置yum源：

```shell
yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
```

然后运行

```shell
yum list docker-ce --showduplicates | sort -r
```

这样能看到很多可供安装的docker版本，使用下面的命令装一个就行了

```shell
yum install docker-ce-18.03.1.ce -y
```



假设你现在已经装好了，输入`docker version`，就能看到下面的界面：

![1606104373112](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606104373112.png)



此时就离大功告成不远了，现在，在命令行输入

```shell
docker pull sequenceiq/hadoop-docker
```

这条命令意思是从docker hub中下载一个名叫sequenceiq/hadoop-docker的镜像，至于docker hub是什么，镜像又是什么，后面会介绍。

命令执行之后，就能看到很多正在下载的layer层

![1606104621095](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1606104621095.png)

docker中的镜像是按层存储的，后面会详细解释，现在暂时有这个一个印象就好。

然后，就是拼网速的时候了。。。

当然了，这个毕竟是国外的东西，所以单纯拼网速还不够，得配置一下镜像加速。

进入 ` /etc/docker ` 目录，修改 ` daemon.json ` 文件为下面的内容即可。

```json
{
  "registry-mirrors":[
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

如果已经开始下载了，按 `ctrl + c` 即可退出下载过程，修改之后再下载一遍就可以了。

下载完成之后，输入 `docker images` 就能看到刚刚下载的hadoop镜像了。

<图片>



下载好之后，输入

```shell
docker run -it --name hadoop -- hostname hadoop sequenceiq/hadoop-docker:2.7.0 /etc/bootstrap.sh -bash
```

这条命令我会在后面详细进行解释。总之，这是一条启动docker容器的命令，输入之后就会看到ssh服务被启动了，然后hdfs和yarn也都被启动了。



这样一个大数据系统就已经搭建完成了，是不是很方便，再也不用再各种配置文件中崩溃了，哈哈哈。

容器启动，怎么用呢？得先登录到容器中

```shell
docker exec -it hadoop /bin/bash
```

登录容器共有四种方法，今天先介绍这种。

登录进去后就可以为所欲为啦。

需要注意的是，这个容器并没有配置环境变量，所以先进入到hadoop的安装目录下。

```shell
cd $HADOOP_PREFIX

# 运行mapreduce测试程序
bin/hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.0.jar grep input output 'dfs[a-z.]+'

# 检查测试程序的输出
bin/hdfs dfs -cat output/*
```



是不是，啪的一下就可以点进来玩大数据了，在也不怕老师让你大环境了，直接不讲武德，哈哈哈。



当然了，这个docker镜像是别人做的，肯定有很多不如意的地方，后面我会教你怎么自己定制一个镜像。



那么今天就到这，拜拜~







该怎么写啊。。。。。。



docker相关知识。



不能做科普导向的，而是应该做成面试导向的。毕竟我需要用这个东西去面大厂，而不是给不懂计算机的人去扫盲。



那就一篇小白扫盲文，保证自己是完全搞懂了现在正在做的事情。然后一篇面试问答文，准备跳槽面试。



说说什么是Docker？

docker也叫容器，是一种系统虚拟化技术，可以实现系统的快速部署，这样就可以解决开发环境与测试环境的不一致的问题。目前常用的系统虚拟化技术有虚拟机和容器两种，虚拟机是一种重量级的



什么是Docker镜像？

什么是Docker容器

Docker容器有几种状态

Dockerfile中最常见的指令是什么

Dockerfile中的命令COPY和ADD命令有什么区别

docker常用命令

容器与主机之间的数据拷贝命令

启动nginx容器（随机端口映射），并挂载本地文件目录到容器html的命令



解释一下dockerfile的ONBUILD指令

什么是Docker Swarm

如何在生产中监控Docker

Docker如何在非Linux系统中运行容器
































































































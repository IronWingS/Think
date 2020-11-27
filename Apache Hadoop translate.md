# distcp 分布式拷贝

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

### raw Namespace Extended Attribute Preservation

（原始命名空间扩展属性保留）

这一部分仅适用于HDFS

### 2.3 命令行选项

| 标记                                      | 描述                                                         | 注意事项                                                     |
| :---------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| **-p[rbugpcaxt]**                         | **r：** replication <br/>**b：** block size     <br/>**u：** user<br/>**g：** group<br/>**p：** permission<br/>**c：** checksum-type<br/>**a：** ACL<br/>**x：** XAttr<br/>**t：** timestamp | 当使用`-update`选项时，只有当文件大小不同时才会同步文件状态。如果指定了`-pa`选项，`DistCp`还是会保留权限，因为`ACLs`是权限的超集 |
| **-i**                                    | 忽略错误                                                     | 正如附录中所说的，该选项比默认情况下，对复制的统计结果更准确。该选项同样同样会从失败的拷贝中保留日志，这对于`debug`非常有帮助。最后，在所有的分裂策略都尝试过之前，一个`map`任务的失败并不会导致整个`job`失败。 |
| **-log** \<logdir\>                       | 向logdir中写入日志                                           | DistCp为每个尝试复制的文件保留一份日志，该文件就是map的输出。如果map任务失败了，当该任务重启的时候，原日志文件不会保留。 |
| **-m**<num_maps>                          | 最大同时复制数                                               | 确定复制数据的map数量。需要注意的是map数量的增多并不会提高系统的吞吐量。 |
| **-overwrite**                            | 复写目标路径                                                 | 如果映射失败，并且没有指定-i参数，拆分中的所有文件（包括复制失败的文件）都会被重新复制。如之前所述，该标记会改变源目录复制到目标目录的文件路径，因此使用的时候需要小心。 |
| **-update**                               | 如果源目录文件和目标目录文件的大小、块大小以及检查数不同就会复写。 | 前文提到过，该命令并不是同步操作。利用源和目标文件的大小、块大小以及检查数作为检验标准，如果这三个标准的值不同，就用源文件替换目标文件。就如前文讨论的，该标记也会改变源路径的路径描述，因此使用的时候需要非常小心。 |
| **-append**                               | 对名称相同但长度不同的文件进行增量拷贝。                     | 如果源文件的长度比目标文件长，就比较公共部分的校验和。如果校验和相等，就使用读和追加的方式拷贝不同的部分。-append选型仅和-update一起使用，没有-skipcrccheck。 |
| **-f<urilist_url>**                       | 使用urilist_uri中的文件列表作为源列表                        | 这个与在命令行列出所有的路径效果是一样的，urilist_uri中的路径应该为绝对路径。 |
| **-filters**                              | 文件路劲更包含一系列模式字符串，一个字符串一行，与该模式匹配的文件路径将从复制过程中排除。 | 支持由java.tuil.regex.Pattern指定的正则表达式。              |
| **-delete**                               | 删除存在于目标路径下但是源路径下不存在的文件。               | 删除操作是由FS Shell完成的，因此如果启用了垃圾桶就会用到。   |
| **-strategy{dynamic <br/>\|uniformsize}** | 选择DistCp的复制策略。                                       | 默认情况下，使用uniforsize                                   |
| **-bandwidth**                            | 为每个map任务设置带宽，单位是MB/s                            | 每个map任务都会被限制只能使用特定的带宽。这个数值也不是绝对的。map任务在复制的过程中会逐渐限制自身带宽的使用量，整个网络的带宽使用会逐渐趋向设定的值。 |
| **-atomic {-tmp\<tmp_dir>}**              | 在指定的路径下进行原子提交。                                 | -atomic指定DistCp将元数据拷贝到临时目标位置，然后将数据整体原子式的提交到目标位置。结果就是数据要么整体都在目标位置，要么都不在。可选参数-tmp可以指定临时文件的位置。如果没有指定，就按默认值来。需要注意的是：tmp_dir必须在目标集群上。 |
| **-mapredSslConf <ssl_conf_file>**        | 指定要与HSFTP一起使用的SSL配置文件                           | 当hsftp协议和源一起使用的时候，可以在配置文件中指定安全相关属性，然后传递给DistCp。ssl_conf_file需要配置在类路径中。 |
| **-async**                                | 异步运行DistCp，启动hadoop的Job之后立刻退出。                | 会打印Hadoop的Job-id，方便追踪                               |
| **-diff**                                 | 使用快照差异报告比较源和目标之间的差异。                     |                                                              |
| **-skipcrccheck**                         | 是否跳过源和目标之间的crc校验。                              |                                                              |

### 2.4 DistCp的结构

新版的`DistCp`可以分为以下几个部分：

- DistCp Driver	
- Copy-listing generator
- Input-formats 和 Map-Reduce components

#### (1) DistCp Driver

**DistCp Driver**部分的职责是：

- 解析`DistCp`命令行中的参数，例如：
  - OptionsParser (选项解析器)
  - DistCpOptionsSwitch (DistCp选项开关)
- 将命令行参数整合到特定的`DistCpOption`对象中并且初始化`DistCp`。这些参数包括：
  - 源路径
  - 目标路径
  - 复制选项（例如：是否在复制的时候更新、复写以及要保留的文件属性等等）
- 通过以下方式编排复制选项：
  - 调用`copy-listing-generator`来产生需要被复制的文件列表
  - 设置并启动`Hadoop`的`Map-Reduce`任务来执行复制。
  - 根据这些选项，可以直接返回`Hadoop MR Job`的句柄，或者等待直到完成。

参数解析器只在命令行或者调用`DistCp::run()`方法时运行。也可以通过编程的方式使用`DistCp`，只要创建`DistCpOption`对象，并适时的初始化该对象即可。

#### (2) Copy-listing Generator

`copy-listing-generator`用来负责产生需要被复制的文件和目录的列表。他们检查源路径中的内容（文件目录还有通配符），并且把所有需要复制的路径记录到`SequenceFile`序列文件中，以供`DistCp`的`Hadoop Job`使用。该模块中主要的类包括：

- **CopyListing：**

  该接口应该被所有copy-listing-generator实现类实现。同时也提供了具体`CopyListing`实现的工厂方法。

- **SimpleCopyListing：**

  是`CopyListing`的一个实现类，可以接受多个源路径，然后递归的列出每个目录下面所有的文件和目录用来复制。

- **globbedCopyListing：**

  `CopyListing`的另一个实现，支持在源路径中使用通配符。

- **FileBaedCopyListing：**

  `CopyListing`的一个实现，从一个指定的文件中读取源路径列表。

根据在`DistCpPotions`中是否指定了源文件列表，`source-listing`按以下两种不同的方式产生：

- 如果没有指定`source-file-list`源文件列表，就是使用`GlobbedCopyListing`。此时所有的通配符都会被扩展，然后所有的扩展都会被转发到`SimpleCopyListing`，通过对每个路径向下递归，依次建立listing。
- 如果指定了`source-file-list`，就使用`FileBasedCopyListing`。从指定的文件中读取源路径，然后将其转发到`GlobbedCopyListing`。然后按上面所说的构建列表。

此外，也可以通过实现`CopyListing`接口定义自己的复制列表的方法。`DistCp`与传统的`DistCp`不同的地方在于如何考虑复制时的路径。

传统的实现只列出明确需要被复制的目标路径，如果目标目录下已经有文件了（并且没有指定`-overwrite`参数），之后的`MapReduce`任务就不会继续考虑这个文件了。在设置过程中（即`MapReduce`任务之前）确定此设置包括文件大小和检查数比较等，是一件比较费时间的事情。

新版的`DistCp`将这些检查推迟到`MapReduce`任务中，因此可以节省设置时间。因为这些检查被并行的分布在各个map任务中，因此性能得到了很大的提升。

#### (3) InputFormats and MapReduce Components

`InputFormats`和`MapReduce`部分主要负责实际的文件拷贝工作，由`copy-list`生成器产生的列表文件就在拷贝过程执行时起作用。这里几个值得注意的类包括：

- **UniformSizeInputFormat：**

  该类实现了`org.apache.hadoop.mapreduce.InputFormat`接口，`InputFormat`为传统的`DistCp`的每个map任务的负载进行均衡`UniformSizeInputFormat`的目的是让每个`map`任务负责的拷贝任务的文件字节数大致相同。同时文件列表被适当的分成路径组，这样使得每个`InputSplit`中文件数的总和与其他`map`几乎相等。这种切分并不总是完美的，但是这种分解的方式让设置时间变短了。

- **DynamicInputFormat 和 DynamicRecordReader：**

  `DynamicInputFormat`实现了`org.apache.hadoop.mapreduce.InputFormat`，是`DistCp`的新增功能。文件列表被切分为不同的块文件，块文件的数量是`Hadoop`任务中请求的`map`个数的倍数。在任务提交之前，每一个`map`任务都分配了一个块文件（通过把块文件更名为任务id的方式）。使用`DynamicRecordReader`从每个块中读取路径，并在`CopyMapper`中处理。当快中的所有路径都被处理之后，当前块就会被删除，然后获取新的块。这个过程一致持续到所有的数据块都被处理完为止。这种动态的方式可以让速度较快的map任务比速度较慢的map任务处理更多的路径，因此`DistCp`任务的速度就会提升很多。

- **CopyMapper：**

  这个类实现了物理的文件拷贝。根据输入选项检查输入路径（在`Job Configuration`指定）来决定一个文件是否需要拷贝。只有当一下条件中任意条件满足就执行拷贝。

  - 目标目录下不存在同名文件
  - 目标目录下存在同名文件，但是文件长度不同
  - 目标目录下存在同名文件，但是有不同的校验和，并且没有使用-skipcrccheck参数
  - 目标目录下存在同名文件，但是使用-overwrite参数
  - 目标目录下存在同名文件，但是块大小不同，同时块大小需要被保留。

- **CopyCommitter：**

  这个类负责DistCp任务的提交，包括：

  - 保留目录权限（如果在选项中指定了）
  - 清理临时文件、工作目录等等。

  ​	




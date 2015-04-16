项目
=================================

whipper-base是一个面向一般在线题库的数据存储系统。该工程不仅提供面向一般在线测评试题数据的存储策略，同时提供一套Scala on Play!框架开发的RESTful接口。该工程旨在帮助开发者快速搭建一套在线题库系统。

同时，该项目develop-docker-compose分支旨在构建一个完整独立题库后台服务。项目采用了Docker Compose实现完整运行时环境的配置，所以在任意一台具有Docker环境的计算机上均能够实现快速部署的目的。

配置
=================================

* [MySQL](https://registry.hub.docker.com/_/mysql/) --- An official MySQL docker image.
* [RabbitMQ](https://registry.hub.docker.com/u/tutum/rabbitmq/) --- A RabbitMQ docker image.
* [Redis](https://registry.hub.docker.com/_/redis/) --- A official Redis docker image.
* [Scala](http://www.scala-lang.org/) - version 2.10.4
* [Activator](http://www.typesafe.com/) (contains Play! Framework) - version 1.2.8
* [Docker](https://www.docker.com/) - version 1.3 or above
* [Docker Compose](https://docs.docker.com/compose/) - version 1.1.0

其中，

1. amqp-client连接RabbitMQ，实现了一组异步操作MySQL数据库的RESTful APIs。
2. play-plugins-redis作为play的插件，扩展了play原生的CachePlugin，从而可以通过Cache自带的API实现以redis为数据存储介质的缓存方案。
3. docker-compose.yml和Compose使得配置开发部署环境变得十分简单。

运行
=================================
1. 必须安装有Docker及Docker Compose
2. 必须安装有activator
3. 在工程根目录，运行:
	`activator clean; activator dist`
4. 在工程根目录，运行:
	`docker-compose up`
5. 在浏览器地址栏输入[http://localhost:9000/](http://localhost:9000/ "wipbase is running")

数据结构
=================================
1. ##### 试题数据的管理(Question CRUD):

	Question是选择题、主观题等等用户最小可练习试题对象的抽象，可以实现选择题、主观题等等具体的测试对象实例。

2. ##### 试题组管理(Composite CRUD):

	Composite是由一组Question和材料文本构成的试题组。利用Composite结构，开发者可以实现例如材料题、阅读题、实验题等等附带有材料信息的组合试题。

3. ##### 试卷管理(Paper CRUD):

	Paper是试卷的抽象数据结构，由试卷基础信息(例如试卷名、创建时间等)、试卷试题信息(由Question和Composite的复合结构构成)和试卷扩展信息(一组K-V列表结构，可存储例如年份、试卷创建人、试卷所属地区等)。试卷是单纯的数据存储结构，是考试的抽象。

4. ##### 考试管理(Exam CRUD):

	Exam是考试的抽象数据结构，通过获取指定试卷数据的副本，整合具体的考试信息(考试开始时间窗、考试时长等)，构成一场考试。Exam是Paper的副本，是面向用户业务可操作的数据对象。为了最大程度减少考试数据的修改和用户对考试数据的操作间的耦合性，Exam仅允许三类操作：

		- 指定一张Paper，获取其副本用来发布一场Exam
		– 撤销一场已经发布的Exam
		– 获取一场指定Exam的信息(包括试题信息、考试时长、考试开始时间窗等)

	**考试开始时间窗**：在之前开发B/S架构的在线测评系统时，开发团队发现如果要实现严格统一的用户考试开始时间约束，既不易于系统的实现与维护，同时对于用户交互体验不友好。于是我们开发一种“时间窗”机制，用户在考前规定一段指定时间内可以组卷获取考试权限，然而超过这个时间段，用户无法再参与该考试。像给这样一个时间段一个更加易于交流的名词，于是命名为“考试开始时间窗”。

5. ##### 标签管理(Catalog CRUD):
	
	Catalog是创建目的是为了解决上述抽象数据的分类问题。例如：用户需要将一系列归属“武汉历年真题”这样一种类型的数据组织起来，便于查询管理和满足特定业务需求。于是开发者可以通过提供一个预定的“武汉历年真题”便签，并关联指定的Exam，即可通过“武汉历年真题”标签的查询API，获取关联的Exam ID序列。亦或者建议开发者通过预定义“武汉”标签，再定义“历年真题”标签，通过将“历年真题”与“武汉”标签建立上下级结构关联，便于以后扩展更多隶属“武汉”标签的业务功能(例如可能会需要“武汉”-“模拟测试”功能等)。
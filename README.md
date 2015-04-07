项目
=================================

whipper-base是一个面向一般在线题库的数据存储系统。该工程不仅提供面向一般在线测评试题数据的存储策略，同时提供一套Scala on Play!框架开发的RESTful接口。该工程旨在帮助开发者快速搭建一套在线题库系统。

同时，该项目develop-docker-compose分支旨在构建一个完整独立题库后台服务。项目采用了Docker Compose实现完整运行时环境的配置，所以在任意一台具有Docker环境的计算机上均能够实现快速部署的目的。

配置
=================================

* [MySQL](https://registry.hub.docker.com/_/mysql/) - 5.5 Docker Official Image 
* [Scala](http://www.scala-lang.org/) - 2.10.4
* [Activator](http://www.typesafe.com/) (contains Play! Framework) - 1.2.8
* [Docker](https://www.docker.com/) - 1.3 or above
* [Docker Compose](https://docs.docker.com/compose/) - 1.1.0

运行
=================================
1. 必须安装有Docker及Docker Compose
2. 必须安装有activator
3. 在工程根目录，运行:
	`activator clean; activator dist`
4. 在工程根目录，运行:
	`docker-compose up`
5. 在浏览器地址栏输入[http://localhost:9000/](http://localhost:9000/ "wipbase is running")
# 目录

* [什么是JMX](#什么是jmx)
* [JMX的基础架构](#jmx的基础架构)
* [怎么使用JMX](#怎么使用jmx)
  * [需求](#需求)
  * [工程环境](#工程环境)
  * [主要步骤](#主要步骤)
  * [创建项目](#创建项目)
  * [引入依赖](#引入依赖)
  * [编写MBean接口](#编写mbean接口)
  * [编写实现类](#编写实现类)
  * [本地连接](#本地连接)
    * [注册MBean](#注册mbean)
    * [测试](#测试)
  * [远程连接方式一(代码实现)](#远程连接方式一代码实现)
    * [开启远程连接](#开启远程连接)
    * [测试](#测试-1)
  * [远程连接方式二(启动参数)](#远程连接方式二启动参数)
  * [远程连接方式三(启动参数+配置文件)](#远程连接方式三启动参数配置文件)
    * [启动参数](#启动参数)
    * [配置文件](#配置文件)
  * [设置账户密码](#设置账户密码)
    * [启动参数](#启动参数-1)
    * [配置文件](#配置文件-1)
    * [测试](#测试-2)
* [参考资料](#参考资料)




# 什么是JMX

`JMX`，全称`Java Management Extensions`，用于我们管理和监控`java`应用程序。`JMX`有以下用途：

1. 监控应用程序的运行状态和相关统计信息。
2. 修改应用程序的配置（无需重启）。
3. 状态变化或出错时通知处理。

举个例子，我们可以通过`jconsole`监控应用程序的堆内存使用量、线程数、类数，查看某些配置信息，甚至可以动态地修改配置。另外，有时还可以利用`JMX`来进行测试。

本文将介绍以下内容：

1. 什么是`JMX`；
2. `JMX`的基础架构；
3. 如何使用`JMX`。

# JMX的基础架构

首先，看下这种图：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145655394-1054785429.png" alt="JMX的基础架构" style="zoom:80%;" />

这里简单介绍下这三层结构：

| 层次            | 描述                                                         |
| --------------- | ------------------------------------------------------------ |
| Instrumentation | 主要包括了一系列的接口定义和描述如何开发`MBean`的规范。在`JMX`中`MBean`代表一个被管理的资源实例，通过`MBean`中暴露的方法和属性，外界可以**获取被管理的资源的状态和操纵`MBean`的行为**。 |
| Agent           | 用来管理相应的资源，并且为远端用户提供访问的接口。该层的核心是`MBeanServer`,**所有的`MBean`都要向它注册，才能被管理**。注册在`MBeanServer`上的`MBean`并不直接和远程应用程序进行通信，他们通过协议适配器（`Adapter`）和连接器（`Connector`）进行通信。 |
| Distributed     | 定义了一系列用来访问Agent的接口和组件，包括`Adapter`和`Connector`的描述。注意，`Adapter` 和`Connector`的区别在于：`Adapter`是使用某种`Internet`协议来与` Agent`获得联系，`Agent`端会有一个对象 (`Adapter`)来处理有关协议的细节。比如`SNMP Adapter`和`HTTP Adapter`。而`Connector`则是使用类似`RPC`的方式来访问`Agent`，在`Agent`端和客户端都必须有这样一个对象来处理相应的请求与应答。比如`RMI Connector`。 |



# 怎么使用JMX

## 需求

1. 测试本地连接管理`MBean`。
2. 测试远程连接管理`MBean`，包括代码实现、启动参数、启动参数+配置文件等方式。
3. 如何开启账号密码认证。

## 工程环境

`JDK`：1.8.0_231

`maven`：3.6.1

`IDE`：eclipse 4.12

## 主要步骤

1. 定义`MBean`接口，并编写实现类；
2. 注册`MBean`到`MBeanServe`；
3. 启动程序；
4. 使用`jconsole`连接管理该程序。

## 创建项目

项目类型Maven Project，打包方式`jar`

## 引入依赖

入门案例暂时不需要引入外部依赖。

## 编写MBean接口

注意，接口名格式必须为：`被管理的类的类名+MBean`。

```java
public interface UserMBean {

	String getName();

	void setName(String name);

	Integer getAge();

	void setAge(Integer age);

	String getAddress();

	void setAddress(String address);
	
	String sayHello();
	
}
```

## 编写实现类

这里简单实现下就行。当属性被设置时，会在控制台打印相关内容。

```java
public class User implements UserMBean {

	private String name;

	private Integer age;

	private String address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		System.err.println("set name to " + name);
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		System.err.println("set age to " + age);
		this.age = age;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		System.err.println("set address to " + address);
		this.address = address;
	}

	public String sayHello() {
		return "Hello!";
	}

}
```

## 本地连接

### 注册MBean

路径为`test`目录下的`cn.zzs.jmx`，类名`JMXTest`。只有将`MBean`注册到`MBeanServer`，`MBean`才能被管理。`MBean`的对象名格式为：`域名：type=MBean类型,name=MBean名称`。其中，`域名`和`MBean名称`可以随便取，对象名中除了type，我们还可以自定义其他条目，以方便管理。

注意，为了让这个程序持续工作，这里强制线程睡眠。

```java
	public static void main(String[] args) throws Exception {
		// 设置MBean对象名,格式为：“域名：type=MBean类型,name=MBean名称”
		String jmxName = "cn.zzs.jmx:type=user,name=user001";
		// 获得MBeanServer
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		// 创建ObjectName
		ObjectName objectName = new ObjectName(jmxName);
		// 创建并注册MBean
		server.registerMBean(new User(), objectName);
		Thread.sleep(60 * 60 * 1000);
	}
```



### 测试

启动程序，打开`jconsole`（在`JDK`安装路径的`bin`目录下），出现如下界面，这时可以看到我们测试的程序：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145729621-429862360.png" alt="jconsole_01.png" style="zoom: 67%;" />

选择`JMXTest`后，点击连接，这时如果弹窗“安全连接失败，是否以不安全的方式重试”，这是因为我们没有开启`ssl`加密，可以不去理会它。点击不安全的连接，即可进入以下页面：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145752737-50192967.png" alt="jconsole_02.png" style="zoom: 67%;" />

通过这个窗口我们可以查看程序的堆内存使用量、线程、类等信息，我们再点击`MBean`选项卡，可以看到我们编写的`MBean`，我们定义的对象名为`cn.zzs.jmx:type=user,name=user001`，其中`cn.zzs.jmx`作为第一级目录，`type=user`作为第二级目录，`name=user001`对应具体的对象，它具备属性和操作。其中，`user`的`setter`和`getter`方法被合并在了一起。

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145820391-2012237632.png" alt="jconsole_03.png" style="zoom: 67%;" />

通过这个界面，我们可以查看和设置`user`的属性，或调用它的方法。例如，我先设置name的值，通过程序控制台可以看到该方法被调用了：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145907904-708194981.png" alt="jconsole_04.png" style="zoom:67%;" />

接着我再调用`sayHello`方法：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145937155-1989042006.png" alt="jconsole_05.png" style="zoom:80%;" />

通过以上例子，可以看到，`JMX`还是非常有用的，除了查看类的属性外，我们还可以在不重启程序的情况下进行配置或执行某些方法。

以上例子中，我们只能在本地访问`JMXTest`，接下来介绍如何实现远程连接。本文介绍三种方式，可根据实际场景选择：

1. 代码实现；
2. 启动参数配置；
3. 启动参数+文件配置。

## 远程连接方式一(代码实现)

本例子在本地连接的基础上修改。

### 开启远程连接

注意，这里的`localhost`最好改为你的`IP`，不然可能连接不上。

```java
	public static void main(String[] args) throws Exception {
		// 设置MBean对象名,格式为：“域名：type=MBean类型,name=MBean名称”
		String jmxName = "cn.zzs.jmx:type=user,name=user001";
		// 获得MBeanServer
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		// 创建ObjectName
		ObjectName objectName = new ObjectName(jmxName);
		// 创建并注册MBean
		server.registerMBean(new User(), objectName);
		// 注册一个端口
		LocateRegistry.createRegistry(9999);
		// URL路径的结尾可以随意指定，但如果需要用Jconsole来进行连接，则必须使用jmxrmi
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
		JMXConnectorServer jcs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
		jcs.start();
	}
```

### 测试

打开`jconsole`，使用远程连接方式，输入我们设置好的ip和端口，点击连接即可：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101145959329-565895891.png" alt="jconsole_06.png" style="zoom:80%;" />

## 远程连接方式二(启动参数)

本例子在本地连接的基础上修改。在程序启动时加入以下启动参数，也可以实现远程连接：

```
-Djava.rmi.server.hostname=<your-ip> // 你的ip
-Dcom.sun.management.jmxremote.port=9999 // 开放端口号
-Dcom.sun.management.jmxremote.local.only=false // 是否只能本地连接
-Dcom.sun.management.jmxremote.ssl=false // 是否使用ssl加密
-Dcom.sun.management.jmxremote.authenticate=false // 是否需要账号密码认证
```

可以看到，我们关闭了`ssl`加密和账号密码认证。

## 远程连接方式三(启动参数+配置文件)

本例子在本地连接的基础上修改。

### 启动参数

在程序启动时加入以下启动参数，并结合配置文件，也可以实现远程连接：

```
-Dcom.sun.management.config.file=D:/growUp/git_repository/jdk-extend/02-jmx-demo/src/main/resources/config/management.properties // 配置文件路径
-Djava.rmi.server.hostname=<your-ip>
```

### 配置文件

在配置文件中配置以下参数：

```properties
# 开放端口号
com.sun.management.jmxremote.port=9999
# 是否只能本地连接
com.sun.management.jmxremote.local.only=false
# 是否使用ssl加密
com.sun.management.jmxremote.ssl=false
# 是否需要账号密码认证
com.sun.management.jmxremote.authenticate=false
```

关于`management.propertie`的详细配置，可以在`$JRE/lib/management/`目录下找到。其实，当我们在启动参数中存在以下参数时，默认会去读取`$JRE/lib/management/management.properties`的配置文件。

```properties
#    -Dcom.sun.management.jmxremote.port=<port-number>
# or -Dcom.sun.management.snmp.port=<port-number>
```


## 设置账户密码

实际使用中，我们更希望采用安全加密的方式来监控程序，这个时候我们可以设置ssl加密或账号密码认证。ssl加密的本文暂时不扩展，这里只介绍如何设置账号密码认证。

### 启动参数

和上个例子一样，需要设置如下启动参数：

```
-Dcom.sun.management.config.file=D:/growUp/git_repository/jdk-extend/02-jmx-demo/src/main/resources/config/management.properties
-Djava.rmi.server.hostname=<your-ip>
```

### 配置文件

配置文件中加入以下内容：

```properties
# 开放端口号
com.sun.management.jmxremote.port=9999
# 是否只能本地连接
com.sun.management.jmxremote.local.only=false
# 是否使用ssl加密
com.sun.management.jmxremote.ssl=false
# 是否需要账号密码认证
com.sun.management.jmxremote.authenticate=true
# 密码文件路径
com.sun.management.jmxremote.password.file=D:/growUp/git_repository/jdk-extend/02-jmx-demo/src/main/resources/config/jmxremote.password
# 权限文件路径
com.sun.management.jmxremote.access.file=D:/growUp/git_repository/jdk-extend/02-jmx-demo/src/main/resources/config/jmxremote.access
```

在此之前，我们需要配置好密码文件和权限文件：

密码文件

```
ZhangZiSheng001 root
```

权限文件

```
ZhangZiSheng001 readwrite
```

### 测试

打开`jconsole`，选择远程连接，并输入账号密码，点击连接即可：

<img src="https://img2018.cnblogs.com/blog/1731892/202001/1731892-20200101150024462-1332920764.png" alt="jconsole_07.png" style="zoom:80%;" />


# 参考资料

[JMX超详细解读]: https://www.cnblogs.com/dongguacai/p/5900507.html

[从零开始玩转JMX(一)——简介和Standard MBean]: https://blog.csdn.net/u013256816/article/details/52800742



>本文为原创文章，转载请附上原文出处链接：https://github.com/ZhangZiSheng001/02-jmx-demo

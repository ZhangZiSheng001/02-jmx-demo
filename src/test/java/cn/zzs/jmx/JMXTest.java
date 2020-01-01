package cn.zzs.jmx;

import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.zzs.jmx.dbcp.JDBCUtils;

/**
 * <p>测试JMX功能</p>
 * @author: zzs
 * @date: 2019年12月30日 上午8:55:29
 */
public class JMXTest {

	private static final Log log = LogFactory.getLog(JMXTest.class);

	public static void main(String[] args) throws Exception {
		// 测试本地访问
		testUserLocal();
		// 测试远程访问
		//testUserRemote();
		// 测试DBCP的JMX支持
		//testDBCP();

	}

	private static void testUserLocal() throws Exception {
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

	private static void testUserRemote() throws Exception {
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

	/**
	 * 
	 * <p>测试DBCP的JMX支持</p>
	 * @author: zzs
	 * @date: 2019年12月30日 上午9:35:37
	 * @return: void
	 * @throws Exception 
	 */
	private static void testDBCP() throws Exception {
		findAll();
		Thread.sleep(60 * 60 * 1000);
	}

	/**
	 * 测试查找用户
	 */
	private static void findAll() {
		// 创建sql
		String sql = "select * from demo_user where deleted = false";
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			// 获得连接
			connection = JDBCUtils.getConnection();
			// 获得Statement对象
			statement = connection.prepareStatement(sql);
			// 执行
			resultSet = statement.executeQuery();
			// 遍历结果集
			while(resultSet.next()) {
				String name = resultSet.getString(2);
				int age = resultSet.getInt(3);
				System.out.println("用户名：" + name + ",年龄：" + age);
			}
		} catch(SQLException e) {
			log.error("查询用户异常", e);
		} finally {
			// 释放资源
			JDBCUtils.release(connection, statement, resultSet);
		}
	}
	
	
}

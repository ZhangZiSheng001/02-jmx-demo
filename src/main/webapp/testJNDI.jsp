<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="cn.zzs.jmx.dbcp.JDBCUtils"%>
<%@page import="java.sql.Connection"%>
<%@page import="javax.naming.Context"%>
<%@page import="javax.naming.RefAddr"%>
<%@page import="javax.naming.CompositeName"%>
<%@page import="org.apache.commons.dbcp2.datasources.SharedPoolDataSourceFactory"%>
<%@page import="org.apache.commons.dbcp2.datasources.SharedPoolDataSource"%>
<%@page import="javax.naming.StringRefAddr"%>
<%@page import="javax.naming.Reference"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<%
    	// Obtain our environment naming context
    	Context initCtx = new InitialContext();
    	Context envCtx = (Context)initCtx.lookup("java:comp/env/");

    	// Look up our data source
    	DataSource dataSource = (DataSource)envCtx.lookup("bean/SharedPoolDataSourceFactory");

    	JDBCUtils.setDataSource(dataSource);

    	// 创建sql
    	String sql = "select * from demo_user where deleted = false";
    	Connection connection = null;
    	PreparedStatement statement = null;
    	ResultSet resultSet = null;
    	try {
    		// 获得连接
    		connection = JDBCUtils.getConnection("root","root");
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
    		System.err.println("查询用户异常：" + e);
    	} finally {
    		// 释放资源
    		JDBCUtils.release(connection, statement, resultSet);
    	}
    %>
</body>
</html>
package cn.zzs.jmx;

/**
 * <p>用户的MBean</p>
 * @author: zzs
 * @date: 2019年12月30日 上午9:28:52
 */
public interface UserMBean {

	String getName();

	void setName(String name);

	Integer getAge();

	void setAge(Integer age);

	String getAddress();

	void setAddress(String address);
	
	String sayHello();
	
}

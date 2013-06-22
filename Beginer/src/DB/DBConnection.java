/**
 * 
 */
package DB;

/**
 * @author xiaoqian
 *
 */
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnection {
	String jdbcDriver; 
	String connectionString;// 数据库连接字符串
	String username; // 数据库用户名
	String password; // 数据库密码 	
	Properties property = new Properties();
	InputStream in = getClass().getResourceAsStream("DB.properties");
	
	public Connection getConnection(){
		try {
			property.load(in);
			jdbcDriver = property.getProperty("jdbcDriver");
			connectionString = property.getProperty("connectionString");
			username = property.getProperty("username");
			password = property.getProperty("password");			
		}catch(Exception e){
			System.out.println("读取配置文件DB.properties出错。");
			e.printStackTrace();
			
		}

		Connection con = null;
		
		try {
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			System.err.println(e.toString());
		}
		try {
			con = DriverManager.getConnection(connectionString, username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return con;
	}
	
	public static void close(Connection con){
		try{
			if(con != null){
				con.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void close(ResultSet rs){
		try{
			if(rs != null){
				rs.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void close(Statement stmt){
		try{
			if(stmt != null){
				stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		//DBConnection dbConn = new DBConnection();
		//Connection con = dbConn.getConnection();
		//System.out.println(con);
		SqlHelper sh = new SqlHelper();
		String sql = "select * from bookread";
		ResultSet rs = sh.query(sql);
		boolean chong = false;
		
		String strTest="百年孤独";
		try{
			while (rs.next()){				
				if (strTest.equals(rs.getString("book"))){
					System.out.println("chongfu");
					chong = true;
					break;
				}	
			}
			if (!chong){
				String  insertsql = "insert into bookread(book,source,date) values(?,?,?)";
				String[] paras = {strTest,"me",new java.util.Date().toString()};
				sh.exeUpdate(insertsql, paras);	
			}
		}catch(SQLException e){
			e.printStackTrace();
		}finally {
			sh.close();
			DBConnection.close(rs);
		}
		
	}

}

/**
 * 
 */
package DB;

import java.sql.*;

/**
 * @author xiaoqian
 *
 */
public class SqlHelper {
	PreparedStatement ps;
	public Connection conn;
	ResultSet rs;
	DBConnection dbConn = new DBConnection();
	
	public SqlHelper(){
		conn = dbConn.getConnection();
	}
	
	public ResultSet query(String sql, String[] paras) {
		try {
			ps = conn.prepareStatement(sql);
			for (int i = 0; i < paras.length; i++) {
				ps.setString(i+1, paras[i]);
			}
			rs = ps.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return rs;
	}
	
	public ResultSet query(String sql){
		try{
			Statement st = conn.createStatement();
			rs = st.executeQuery(sql);
		}catch(SQLException e){
			e.printStackTrace();
		}
		return rs;
	}
	
	//数据库的增删改
	public boolean exeUpdate(String sql, String[] paras) {
		boolean b = true;
		try {
			ps = conn.prepareStatement(sql);
			for (int i = 0; i < paras.length; i++) {
				ps.setString(i + 1, paras[i]);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			b = false;
			e.printStackTrace();
		}
		return b;
	}
	
	public boolean exeUpdateInt(String sql, int para) {
		boolean b = true;
		try {
			ps = conn.prepareStatement(sql);
				ps.setInt(1, para);
			ps.executeUpdate();
		} catch (SQLException e) {
			b = false;
			e.printStackTrace();
		}
		return b;
	}
	
	public void close() {
		try {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (!conn.isClosed()) {
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
}

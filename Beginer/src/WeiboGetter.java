
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import DB.DBConnection;
import DB.SqlHelper;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;
import weibo4j.org.json.JSONObject;

/**
 * 
 */

/**
 * @author xiaoqian
 *
 */
public class WeiboGetter {
	private String access_token = "2.005T6ErBy4EQLB5615e164e2sGoMAD";
	Timeline tm = new Timeline();
	
	public WeiboGetter(){
		this.tm.client.setToken(access_token);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WeiboGetter wg =new WeiboGetter();
		ArrayList<String> list = new ArrayList<String>();
		list = wg.getContent("qian291random");
		System.out.println(list.size());
		for (String s:list){
			System.out.println(s);
		}
	}
	
	/**
	 * 获取该昵称中的微博所有内容
	 * @param nickName 微博昵称
	 * @return 返回ArrayList存放微博内容
	 */
	public ArrayList<String> getContent(String nickName){
		ArrayList<String> list = new ArrayList<String>();		
		try {
			//获取当前最新微博ID
			JSONObject json = tm.getUserTimelineIdsByName(nickName);
			String latestId = json.getJSONArray("statuses").getString(0);
			
			//数据库中最新微博ID和总页数
			Integer totalPageOld = getTotalPage(nickName);
			String tempId = getLatestWid(nickName);
			if (tempId != null){
				if (latestId.compareTo(tempId) > 0){
					//当前微博比数据库中的晚发布
					//开始爬取比数据库记录微博发布晚的微博
					getContentFromSina(nickName, tempId,latestId, totalPageOld);					
					//整合数据库中已有的条目
					list = getContentFromDB(nickName);
					return list;
				}else{
					//当前且之前的微博都已在数据库
					//直接从数据库拿出数据就可以了
					list = getContentFromDB(nickName);
					return list;
				}
			}
			//此昵称在数据库中没有数据,从新浪爬全部
			list = getContentFromSina(nickName, "0",latestId, null);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return list;
	}

	public void insertDB(String nickName,ArrayList<String> list){
		SqlHelper sh = new SqlHelper();
		String sql = "insert into weibo(nickname,content,date) values(?,?,?)";
		
		String[] paras = {nickName,list.get(0),new java.util.Date().toString()};
		
		try {
			sh.conn.setAutoCommit(false);
			sh.conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE);
			for (String s:list){
				paras[1] = s;
				sh.exeUpdate(sql, paras);
			}
			sh.conn.commit();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			sh.close();
		}
	}
	
	public void insertRecord(String nickName,String latestWid,Integer totalPage){
		SqlHelper sh = new SqlHelper();
		String sql = "insert into record(nickname,latestwid,totalpage,date) values(?,?,?,?)";
		String[] paras = {nickName,latestWid,totalPage.toString(),new java.util.Date().toString()};
		try {
			sh.exeUpdate(sql, paras);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			sh.close();
		}
	}
	
	public void updateRecord(String nickName,String latestWid,Integer totalPage){
		SqlHelper sh = new SqlHelper();
		String sql = "update record set latestwid=?,totalpage=?,date=? where nickName = ?";
		String[] paras = {latestWid,totalPage.toString(),new java.util.Date().toString(),nickName};
		sh.exeUpdate(sql, paras);
		sh.close();
	}
	
	/**
	 * 从数据库获得收录的最新微博id
	 * @param nickName 微博昵称
	 * @return 返回id号
	 */
	public String getLatestWid(String nickName){
		SqlHelper sh = new SqlHelper();
		ResultSet rs;
		String sql = "select latestwid from record where nickName=?";
		String[] paras = {nickName};
		rs = sh.query(sql, paras);
		try {
			while (rs.next())
				return rs.getString("latestwid");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			sh.close();
			DBConnection.close(rs);
		}
		return null;
	}
	
	/**
	 * 从数据库获得收录的微博总页数
	 * @param nickName 微博昵称
	 * @return 返回页数
	 */
	public Integer getTotalPage(String nickName){
		SqlHelper sh = new SqlHelper();
		ResultSet rs;
		String sql = "select totalpage from record where nickName=?";
		String[] paras = {nickName};
		rs = sh.query(sql, paras);
		try {
			while (rs.next())
				return Integer.valueOf(rs.getString("totalpage"));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			sh.close();
			DBConnection.close(rs);
		}
		return null;
	}
	
	/**
	 * 从数据库获取到该昵称的微博内容
	 * @param nickName 微博昵称
	 * @return 返回ArrayList存放微博内容
	 */
	public ArrayList<String> getContentFromDB(String nickName){
		SqlHelper sh = new SqlHelper();
		ArrayList<String> list = new ArrayList<String>();
		String sql = "select content from weibo where nickname=?";
		String[] paras = {nickName};
		ResultSet rs = sh.query(sql, paras);
		try {
			while (rs.next()){

				list.add(rs.getString("content"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			sh.close();
			DBConnection.close(rs);
		}
		return list;
	}
	
	/**
	 * 从新浪api获取微博的内容
	 * @param nickName 微博昵称
	 * @param sinceId 返回ID比since_id大的微博（即比since_id时间晚的微博），0为全部都要
	 * @param totalPageOld 微博的总页数,如页数未知为null
	 * @return 存放微博内容的ArrayList
	 */
	public ArrayList<String> getContentFromSina(String nickName,String sinceId,String latestWid,Integer totalPageOld){
		ArrayList<String> list = new ArrayList<String>();
		//每页100，初为第1页
		Paging page = new Paging(1,100);
		//初始化总页数和当前页
		int totalPage = 1;
		int currentPage = 1;
		StatusWapper stList;
		int tempTotal=0;
		
		while(currentPage <= totalPage){				
			page.setPage(currentPage);
			try {
				stList = tm.getUserTimelineByName(nickName, page, 1, sinceId);
				
				// 获取总页数
				// System.out.println(stList.getTotalNumber());
				tempTotal = (int) (stList.getTotalNumber() / 100);
				if (stList.getTotalNumber() % 100 != 0)
					tempTotal = tempTotal + 1;
				
				
				//总页数=总数量-数据库已存数量
				if (totalPageOld != null)
					totalPage = tempTotal-totalPageOld;
				else
					totalPage = tempTotal;

				for (Status status : stList.getStatuses()) {
					if (status.getRetweetedStatus() == null) {
						list.add(status.getText());
					} else {
						// 获取转发的微博内容
						list.add(status.getText() + "#￥#"
								+ status.getRetweetedStatus().getText());
					}
				}
				currentPage++;
			} catch (WeiboException e) {
				e.printStackTrace();
				return null;
			}
		}//while
		insertDB(nickName,list);
		if (totalPageOld == null)
			insertRecord(nickName, latestWid, tempTotal);
		else
			updateRecord(nickName, latestWid, tempTotal);
		return list;
		
	}
}

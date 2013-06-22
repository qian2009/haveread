/**
 * 
 */

/**
 * @author xiaoqian
 *
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import DB.DBConnection;
import DB.SqlHelper;

public class BookMatcher {
	
	
	
	/**
	 * 链接url
	 * @param url 网址
	 * @return 返回html字符串
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	public String connectURL(URL url) throws UnsupportedEncodingException, IOException{
		//sb为网页返回的HTML字符串
		StringBuffer sb = new StringBuffer();
			//URL url = new URL("https://www.googleapis.com/books/v1/volumes?q="+URLEncoder.encode(check)+"&printType=books&projection=lite&key=AIzaSyAwPPwJxhd75CCr2Y6H83cvbZJXQHUqyvQ");
			//URI uri = new URI("https","www.googleapis.com/books/v1/volumes",);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
				
			String temp =null;
			BufferedReader bf = new BufferedReader(new InputStreamReader(url.openStream(),"utf-8"));
			while ((temp = bf.readLine())!= null){
				sb.append(temp+"\n");
			}
			bf.close();

		return sb.toString();
	}
	
	/**
	 * 主搜索方法，先从数据库找起，之后再找google图书
	 * @param check 要查询的书目
	 * @return 返回匹配结果
	 */
	public Book searchDB(String check){
		Book book = null;
		SqlHelper sh = new SqlHelper();
		String sql = "select * from bookread";
		ResultSet rs = sh.query(sql);
		//String strTest="mybook5";
	
		try{
			while (rs.next()){				
				if (check.equals(rs.getString("book"))){
					//System.out.println(check+"     :已在数据库!");
					book = new Book();
					book.name = check;
					book.isbn = rs.getString("isbn");
					return book;
				}	
			}
			//找找看是否以前匹配过（在可能不是书的表中）
			sql = "select * from otherbook";
			rs = sh.query(sql);			
			while (rs.next()){
				if (check.equals(rs.getString("name"))){
					//System.out.println(check+"     :已在数据库!- -虽然不是书。。。。");
					return null;
				}	
			}
			
			//不在数据库的书目，开始放在当当匹配
			if((book=dangdangMatch(check))!=null){
				insertBook(book,"dangdang");
				return book;
			}else{		
				// 不在数据库也不在当当的书目，开始在google图书上匹配
				if ((book = googleMatch(check)) != null) {
					//google上有这书，放进数据库
					insertBook(book,"google_books");
					return book;
				} else {
					//最后在百度看豆瓣有没有（有时会遇到验证码- -||，不精确，补漏用。。。。）
					if ((book = baiduMatch(check)) != null){
						insertBook(book,"baidu");
						return book;
					}
					insertOther(check);
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}finally {
			sh.close();
			DBConnection.close(rs);
		}
		return null;
	}
	
	/**
	 * 匹配书目“check”，返回匹配结果，不匹配则返回NULL
	 * @param check 要查的书名称
	 * @return 返回书本对象
	 */
	public Book googleMatch(String check){
		String s;//s为返回的json字符串
		try{
			URL url = new URL("https://www.googleapis.com/books/v1/volumes?q="+URLEncoder.encode(check)+"&printType=books&key=AIzaSyAwPPwJxhd75CCr2Y6H83cvbZJXQHUqyvQ");
			try {
				s = connectURL(url);
			}catch(IOException e){
				System.out.print("google quota用完，改成百度匹配。。。。");
				return baiduMatch(check);
			}
			//转换json对象，匹配书目
			JSONObject json = new JSONObject(s);
			if (json.getInt("totalItems")==0){
				//System.out.println("google上找不到匹配");
				return null;
			}
			
			JSONArray array = json.getJSONArray("items");
			
			for (int i=0;i<array.length();i++){
				JSONObject book = (JSONObject) array.get(i);
				//System.out.println(book.toString());
				String title;
				try{
					title = book.getJSONObject("volumeInfo").getString("title");
				}catch(Exception e){
					return null;
				}
				
				if (check.equals(title)){
					//结果匹配,收集这本书的信息并返回
					//System.out.println(check);
					Book bookInfo = new Book();			
					bookInfo.name = check;		
					//System.out.println(book.toString());
					
					try{
						bookInfo.isbn = ((JSONObject)(book.getJSONObject("volumeInfo").getJSONArray("industryIdentifiers").get(0))).getString("identifier");
					}catch(Exception e){
						bookInfo.isbn = null;
					}
					return bookInfo;					
				}
			}		
		}catch(Exception e){
			e.printStackTrace();
		}
		//找不到匹配（可能不是书）
		return null;
	}
	
	/**
	 * 从百度上搜索该书
	 * @param check
	 * @return
	 */
	public Book baiduMatch(String check){
		String url;
		url = "http://www.baidu.com/s?wd="
				+ URLEncoder.encode("site:(www.douban.com)inurl:(isbn)" + check);
		try {
			Document doc = Jsoup.connect(url).get();
			Elements node = doc.select("h3.t>a").select(":contains(" + check);
			if (node.size() != 0){
				for (Element e:node){
					if (e.text().equals(check + "(豆瓣)")
							|| e.text().equals(check + " (豆瓣)")) {
						return new Book(check);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//System.out.println("豆瓣也没有！");
		return null;
	}
	
	/**
	 * 从当当上搜索该书
	 * @param check
	 * @return
	 */
	public Book dangdangMatch(String check) {
		String url;
		
		try {
			url = "http://search.dangdang.com/?key=" + URLEncoder.encode(check, "gbk") +"&category_path=01.00.00.00.00.00";
			Document doc = Jsoup.connect(url).get();
			Elements node = doc.select("div.inner>a[title]");
			if (node.size() != 0)
				for (Element e:node){
					//System.out.println(e.attr("title"));
					if (e.attr("title").trim().equals(check))
						return new Book(check);
				}

		} catch (Exception e) {
			e.printStackTrace();
		}
		//System.out.println("当当也没有！");
		return null;
	}

	public void insertBook(Book book,String source){
		String insertsql;
		SqlHelper sh = new SqlHelper();
		
		insertsql = "insert into bookread(book,source,date,isbn) values(?,?,?,?)";
		String[] paras = { book.name, source,
				new java.util.Date().toString(), book.isbn };
		sh.exeUpdate(insertsql, paras);
		sh.close();
	}
	
	public void insertOther(String name){
		String insertsql = "insert into otherbook(name,date) values(?,?)";
		SqlHelper sh = new SqlHelper();
		String[] paras = { name, new java.util.Date().toString() };
		sh.exeUpdate(insertsql, paras);
		sh.close();
	}

	public static void main(String[] str){
		//BookMatcher cr = new BookMatcher();
		
		//System.out.println(cr.baiduMatch("非理性的人").name);
//		cr.searchDB("mybook2", "Arber");
//		Book book = cr.googleMatch("水洗马路");
//		System.out.println(book.isbn);
//
//		BufferedReader br;
//		BufferedWriter bw;
//		String s;
//		Book result;
//		try {
//			br = new BufferedReader(new FileReader("G:\\test2.txt"));
//			bw = new BufferedWriter(new FileWriter("G:\\test3.txt"));
//			while ((s = br.readLine()) != null) {
//				if ((result = cr.searchDB(s)) != null) {
//					bw.write(result.name);
//					bw.newLine();
//					bw.flush();
//				}
//			}
//			bw.close();
//			br.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		if (cr.dangdangMatch("第一次亲密接触") != null)
//			System.out.println(cr.dangdangMatch("第一次亲密接触").name);
	}
	
}

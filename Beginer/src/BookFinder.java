/**
 * 
 */

/**
 * @author xiaoqian
 *
 */

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
public class BookFinder {
	private int netToken = 0;
	public ArrayList<String> nolist = new ArrayList<String>();
	public ArrayList<String> yeslist = new ArrayList<String>();
	public HashSet<String> set = new HashSet<String>();//存正则匹配书目
	
	public Book netFindBook(String check){
		String url = null;	
		Document doc;
		String text;
		try{
			switch (netToken) {
			case 0:
				url = "http://www.baidu.com/s?wd=" + URLEncoder.encode(check);
				//System.out.println("我在使用百度！百度百度百度百度百度百度百度百度");
				netToken = (++netToken) % 8;
				break;
			case 1:
				url = "http://s.aliyun.com/s?q=" + URLEncoder.encode(check);
				netToken = (++netToken) % 8;
				//System.out.println("我在使用即刻即刻即刻即刻即刻即刻即刻即刻即刻即刻即刻");
				break;
			case 2:
				url = "http://cn.bing.com/search?q=" + URLEncoder.encode(check);
				//System.out.println("bingbingbingbingbingbing");
				netToken = (++netToken) % 8;
				break;
			case 3:
				url = "http://www.sogou.com/web?query=" + URLEncoder.encode(check);
				netToken = (++netToken) % 8;
				//System.out.println("搜狗搜狗搜狗搜狗搜狗搜狗搜狗搜狗搜狗搜狗搜狗搜狗");
				break;
			case 4:
				url = "http://search.panguso.com/pagesearch.htm?q=" + URLEncoder.encode(check);
				netToken = (++netToken) % 8;
				//System.out.println("盘古盘古盘古盘古盘古盘古盘古盘古盘古盘古盘古盘古盘古盘古盘古");
				break;
			case 5:
				url = "http://www.soso.com/q?w=" + URLEncoder.encode(check);
				netToken = (++netToken) % 8;
				//System.out.println("soso,soso,soso,soso,soso,soso");
				break;
			case 6:
				url = "http://www.youdao.com/search?q=" + URLEncoder.encode(check);
				netToken = (++netToken) % 8;
				//System.out.println("有道。有道。有道。有道。有道。有道。");
				break;
			case 7:
				url = "http://www.so.com/s?q=" + URLEncoder.encode(check);
				netToken = (++netToken) % 8;
				//System.out.println("360,360,360,360,360,360,360,360,");
				break;
			}
			doc = Jsoup.connect(url).get();
			Element node = doc.select("h3>a").select(":contains(豆瓣)")
					.first();

			if (node != null) {
				text = node.text();
				//text = text.replace(" (豆瓣)", "");
				System.out.println(text);
				return new Book(text);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
		
	}
	
	public ArrayList<String> regFindBook(String check){
		//匹配《》或者<>
		String regex = "《[^《@/\\~=]+?》|<[^<@/\\~=]+?>";
		Pattern pattern = Pattern.compile(regex);

		ArrayList<String> list = new ArrayList<String>();
		String matStr;//书名
		
		//byte[] bytes = s.getBytes("UTF-8");
		//String s2 =new String (bytes,"GBK");
		//System.out.println(s2);
		Matcher matcher = pattern.matcher(check);
		//匹配开始
		while (matcher.find()){
			matStr = matcher.group().replaceAll("《|》|<|>","");
			list.add(matStr);
		}
		return list;
	}
	
	public void findBooks(ArrayList<String> list){
			ArrayList<String> regList = new ArrayList<String>();
			BookMatcher bookMatcher = new BookMatcher();
			Book book;
			int count = 0;
			for(String content:list){
				
				regList = regFindBook(content);				
				if (regList.size() != 0){
					//正则表达式匹配书名的
					for (String matStr:regList){
						//去重
						if (set.contains(matStr))
							continue;
						set.add(matStr);
						
						//开始匹配书名
						book = bookMatcher.searchDB(matStr);
						if (book != null){
							//是书
							yeslist.add(matStr);
							if (++count % 15 == 0){
								System.out.println();
							}
							System.out.print("《"+book.name+"》   ");
						}else
							//可能不是书
							nolist.add(matStr);
					}
				}else{
					//微博中没有书名号的，在搜索引擎中查找书本（准确率效率都太渣，抛弃了。。。T T）
					//netFindBook(content);
				}

			}
			
//			//输出
//			Iterator<String> it = set.iterator();
//			while(it.hasNext()){
//				Object key = it.next();
//				System.out.println(key.toString());
//			}

	}

	/**
	 * 主程序入口
	 * @param args 参数：第一个参数为微博昵称
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WeiboGetter wg = new WeiboGetter();
		ArrayList<String> list = new ArrayList<String>();
		int count = 0;
		
		System.out.println("正在获取微博内容......（时间视乎微博量，请稍等.......）");
		list = wg.getContent(args[0]);				
		System.out.println("博主共发微博数："+list.size());
		
		System.out.println("正在分析该用户微博......");		
		BookFinder bookFinder = new BookFinder();
		System.out.println("以下为博主可能看过的书 ：");
		bookFinder.findBooks(list);
		System.out.println();System.out.println();System.out.println();
		System.out.println("以下为博主可能看过的其他杂文（电影，报告，文章等）：");
		for (String s:bookFinder.nolist){
			if (++count % 15 == 0){
				System.out.println();
			}
			System.out.print("《"+s+"》  ");
		}
		
	}

}

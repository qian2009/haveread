haveread
========

get books that someone read from weibo
分析：
    整个项目实现思路简单，缺点也不少。具体来说就是：先通过昵称获取到用户发过的所有微博，然后用正则表达式匹配书名号，最后就是确定获得的条目哪些是真正的书目。最后一步费了不少时间，最后确定的方案是：先放上当当网上搜索，搜不到的话，再放到google图书上匹配（google的api比较坑爹，每天只能调用1000次......所以每天用完quota之后，准确率又要下降了......），还没匹配的话最后再丢给百度了。
    这样通过匹配书名号确实有点无语- -||，其实我是有想了另一个方案的，就是把没有书名号的微博内容放搜索引擎看搜索结果，再作判断，后来做出来后发现准确率和效率都很低.......最后抛弃这个做法了......

用到的外部工具：
    weibo的sdk，google的api，jsoup（java的html解析库），org.json，数据库用的是mysql 5.5.25

项目架构：
    共有1个实体类:Book.java，属性暂时有书名和isbn码。
    3个工作类:WeiboGetter.java---获取用户的微博内容；BookFinder.java---从微博内容获取书名；BookMatcher.java----判断从微博内容获取的书名是不是书。
    还有2个数据库操作类，DBConnection.java,SqlHelper.java。使用数据库在于缓存，方便以后搜索，例如搜过的用户，下次搜的话就只爬最新的发布的那几条微博就可以了。还有书目的匹配也是如此。

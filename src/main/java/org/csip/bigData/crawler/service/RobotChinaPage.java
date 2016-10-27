package org.csip.bigData.crawler.service;

import org.csip.bigData.crawler.dao.MongoDBPipeline;
import org.csip.bigData.crawler.util.ParamsConfigurationUtil;
import org.csip.bigData.crawler.util.dataProcessUtil.*;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;
import org.csip.bigData.crawler.util.spiderUtil.DateUtil;
import org.csip.bigData.crawler.util.MD5Util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by csip on 7/26/16.
 * 中国机器人网（www.robot-china.com）
 */
public class RobotChinaPage implements PageProcessor{

    private static String customTime0;
    private static String customTime1;
//    private static String customTime2;
//    private static String customTime3;

    //资讯列表（行业、展会）
    private static final String URL_NEWS_LIST = "http://www\\.robot-china\\.com/news/list-\\d+-\\d+.html";
    //专题列表
    private static final String URL_TOPIC_LIST = "http://www\\.robot-china\\.com/zhuanti/list-\\d+-\\d+.html";
    //企业新闻列表
    private static final String URL_COMPANY_NEWS_LIST = "http://www\\.robot-china\\.com/company/news-htm-page-\\d+.html";
    //资讯文章页
    private static final String URL_NEWS_ARTICLE = "http://www\\.robot-china\\.com/news/\\d+/\\d+/\\d+.html";
    //专题文章页
    private static final String URL_TOPIC_ARTICLE = "http://www\\.robot-china\\.com/zhuanti/show-\\d+.html";
    //企业新闻文章页
    private static final String URL_COMPANY_ARTICLE = "http://\\w+\\.robot-china\\.com/news/itemid-\\d+.shtml";


    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    @Override
    public void process(Page page) {
        //资讯列表页
        if (page.getUrl().regex(URL_NEWS_LIST).match()) {
            /*page.addTargetRequests(page.getHtml().xpath("//div[@class=\"panel-container\"]").links().regex(URL_NEWS_ARTICLE).all());
            List<String> links_post = page.getHtml().xpath("//div[@class=\"pages\"]").links().regex(URL_NEWS_LIST).all();
            page.addTargetRequests(links_post);*/
            increaseUpdate(page, URL_NEWS_ARTICLE);
            //专题列表页
//        } else if (page.getUrl().regex(URL_TOPIC_LIST).match()) {
//            page.addTargetRequests(page.getHtml().xpath("//div[@class='panel-container']").links().regex(URL_TOPIC_ARTICLE).all());
//            List<String> links_post = page.getHtml().xpath("//div[@class='pages']").links().regex(URL_TOPIC_LIST).all();
//            page.addTargetRequests(links_post);
////            increaseUpdate(page, URL_TOPIC_ARTICLE);
            //企业新闻列表页
        } else if (page.getUrl().regex(URL_COMPANY_NEWS_LIST).match()) {
            /*page.addTargetRequests(page.getHtml().xpath("//div[@class=\"catlist\"]").links().regex(URL_COMPANY_ARTICLE).all());
            List<String> links_post = page.getHtml().xpath("//div[@class=\"pages\"]").links().regex(URL_COMPANY_NEWS_LIST).all();
            page.addTargetRequests(links_post);*/
            increaseUpdate(page, URL_COMPANY_ARTICLE);
            //资讯和专题文章页
        } else {
            if (page.getUrl().regex(URL_NEWS_ARTICLE).match() || page.getUrl().regex(URL_TOPIC_ARTICLE).match()) {
                page.putField("id", "com.robot-china.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
                page.putField("source_url", page.getUrl().toString());
                String title = page.getHtml().xpath("//div[@class='zx3']/h3/text()").toString();
                page.putField("title", title);
                String publishTime = page.getHtml().xpath("//ul[@id='plshare']/li[2]/text()").toString();
                page.putField("publish_time", publishTime.replace("时间：", "") + " 00:00:00");

                String sourceInfo = page.getHtml().xpath("//ul[@id='plshare']/li[3]/text()").toString();
                if (sourceInfo.contains("来源")) {
                    page.putField("source", sourceInfo.replace("来源：", "").trim());
                    page.putField("author", page.getHtml().xpath("//ul[@id='plshare']/li[4]/text()").replace("编译：", "").toString());
                } else {
                    //System.out.println("来源网站");
                    page.putField("source", "中国机器人网");
                    page.putField("author", sourceInfo.replace("编译：", ""));
                }
                page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
                //page.putField("digest", "");

                String content = page.getHtml().xpath("//div[@class='content']").toString();
                //去掉所有html标签的干净文本
                String cleanContent = CleanTextUtil.getCleanText(content);
                page.putField("cleanContent",cleanContent);
                try {
                    SimHash simHash = new SimHash(cleanContent, 64);
                    page.putField("simhash",simHash.get64strSimHash());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ArrayList<String> segments = HanlpUtil.instance.getSegment(cleanContent);
                StringBuilder sb = new StringBuilder();
                for (String segment : segments) {
                    sb.append(segment).append(" ");
                }
                page.putField("chineseSegment", sb.toString());

                page.putField("source_tag", page.getHtml().xpath("//ul[@id='plshare']/li[1]/a/text()").toString());
                //page.putField("category", "动态");
                String imageStr = page.getHtml().xpath("//div[@class='content']//img/@src").all().toString();
                String[] imageArr = imageStr.replace("[", "").replace("]", "").split(",");
                int imalength=imageArr.length;
                if(imageArr[imageArr.length-1]==null||imageArr[imageArr.length-1].equals("")){
                    imalength=0;
                }
                for (int i = 0; i < imalength; i++) {
                    String img_one = imageArr[i].trim();
                    //int positon = Arrays.binarySearch(imageArr, img_one);
                    content = content.replace(img_one, "images[" + i + "]");
                }
                page.putField("content", ContentUtil.removeTags(content));
                page.putField("images", imageStr);

                page.putField("source_content", page.getRawText());
                page.putField("from_site", "中国机器人网www.robot-china.com");

                page.putField("category", ArticleCategoryUtil.instance.getCategory( title ));
                page.putField("digest", HanlpUtil.instance.getPartWordsSummary(cleanContent));
                ArrayList<String> sourceTagList=new ArrayList<String>();
                int number=10;
                ArrayList<String> keywords= KeywordExtractUtil.getKeyword(cleanContent,sourceTagList,number);
                HashSet<String> sets = new HashSet<String>();
                for (String keyword : keywords) {
                    sets.add(keyword);
                }
                StringBuilder setSb = new StringBuilder();
                for (String set: sets) {
                    setSb.append(set).append(" ");
                }
                page.putField( "tag",setSb.toString().trim());
                //企业新闻文章页
            } else if (page.getUrl().regex(URL_COMPANY_ARTICLE).match()) {
                page.putField("id", "com.robot-china.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
                page.putField("source_url", page.getUrl().toString());
                String title = page.getHtml().xpath("//div[@class='title']/text()").toString();
                page.putField("title", title);
                String publishTime = page.getHtml().xpath("//div[@class='info']/text()").toString();
                page.putField("publish_time", publishTime.replace("发布时间：", "").substring(0, 10) + " 00:00:00");
//                System.out.println("内容页："+DateUtil.getSystemCurrentDateTime());
                page.putField("source", "中国机器人网");
                page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
                page.putField("author", "中国机器人网");
                //page.putField("digest", "");

                String content=page.getHtml().xpath("//div[@class='content']").toString();
                //去掉所有html标签的干净文本
                String cleanContent = CleanTextUtil.getCleanText(content);
                page.putField("cleanContent",cleanContent);
                try {
                    SimHash simHash = new SimHash(cleanContent, 64);
                    page.putField("simhash",simHash.get64strSimHash());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ArrayList<String> segments = HanlpUtil.instance.getSegment(cleanContent);
                StringBuilder sb = new StringBuilder();
                for (String segment : segments) {
                    sb.append(segment).append(" ");
                }
                page.putField("chineseSegment", sb.toString());
                String imageStr = page.getHtml().xpath("//div[@class='content']//img/@src").all().toString();
                String[] imageArr = imageStr.replace("[", "").replace("]", "").split(",");
                int imalength=imageArr.length;
                if(imageArr[imageArr.length-1]==null||imageArr[imageArr.length-1].equals("")){
                    imalength=0;
                }
                for (int i = 0; i < imalength; i++) {
                    String img_one = imageArr[i].trim();
                    //int positon = Arrays.binarySearch(imageArr, img_one);
                    content = content.replace(img_one, "images[" + i + "]");
                }

                page.putField("content",  ContentUtil.removeTags(content));

                page.putField("source_tag", "企业新闻");
                //page.putField("category", "动态");
                page.putField("images", page.getHtml().xpath("//div[@class='content']//img/@src").all().toString());
                page.putField("source_content", page.getRawText());
                page.putField("from_site", "中国机器人网www.robot-china.com");
                page.putField("category", ArticleCategoryUtil.instance.getCategory( title ));
                page.putField("digest", HanlpUtil.instance.getPartWordsSummary(cleanContent));
                ArrayList<String> sourceTagList=new ArrayList<String>();
                int number=10;
                ArrayList<String> keywords= KeywordExtractUtil.getKeyword(cleanContent,sourceTagList,number);
                HashSet<String> sets = new HashSet<String>();
                for (String keyword : keywords) {
                    sets.add(keyword);
                }
                StringBuilder setSb = new StringBuilder();
                for (String set: sets) {
                    setSb.append(set).append(" ");
                }
                page.putField( "tag",setSb.toString().trim());

            }
        }
    }
    @Override
    public Site getSite()
    {
        return site;
    }

    /**
     * 增量更新
     *
     * @param page
     */
    private static void increaseUpdate(Page page, String articleRegex) {
        String nextPage = "下一页";
        int nodeNum = 0;
        //用于计数当页符合规则链接数，如果当页链接全部抓取，则需要抓取下一页链接
        int count = 0;
        //资讯文章页
        if (URL_NEWS_ARTICLE.equals(articleRegex)) {
            //获取内容页的链接所在的节点
            List<Selectable> selectableList = page.getHtml().xpath("//div[@id='tab']/ul").nodes();
            nodeNum = selectableList.size();
            System.out.println("nodeNum:" + nodeNum);
            //遍历每个链接所在的节点，根据日期规则，选择符合规则链接并加入页面访问列表中
            for (Selectable selectable : selectableList) {
                //获取内容页链接
                String url = selectable.links().regex(articleRegex).toString();
                String[] urlArr = url.split("/");
                //获取内容页的发布时间
                String dateStr = urlArr[4] + urlArr[5];

                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                try {
                    Date newDate = df.parse(dateStr);
//                    System.out.println("df.parse(customTime0).getTime()"+df.parse(customTime0));
                    System.out.println("哈哈URL_NEWS_ARTICLE:dataStr=" + newDate);
//                    System.out.println("df.parse(customTime1).getTime():"+df.parse(customTime1));
                    //发布时间与预定义的更新时间进行比较。
                    if (newDate.getTime() >= df.parse(customTime0).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        page.addTargetRequest(url);
                        System.out.println("URL_NEWS_ARTICLE:dataStr=" + newDate.getTime());
                        System.out.println(url);
                        count++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            //企业新闻文章页
        } else if (URL_COMPANY_ARTICLE.equals(articleRegex)) {
            //获取内容页的链接所在的节点
            List<Selectable> selectableList = page.getHtml().xpath("//div[@class='catlist']/ul/li").nodes();
            nodeNum = selectableList.size();
            System.out.println("nodeNum:" + nodeNum);
            //遍历每个链接所在的节点，根据日期规则，选择符合规则链接并加入页面访问列表中
            for (Selectable selectable : selectableList) {
                //获取内容页链接
                String url = selectable.links().regex(articleRegex).toString();
                //获取内容页的发布时间
                String dateStr = selectable.xpath("//em/text()").toString();

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                try {
                    Date newDate = df.parse(dateStr);
//                    System.out.println("df.parse(customTime2).getTime():"+df.parse(customTime2));
                    System.out.println("企业URL_COMPANY_ARTICLE:哈哈dataStr=" + newDate);
//                    System.out.println("df.parse(customTime3).getTime():"+df.parse(customTime3));
                    //发布时间与预定义的更新时间进行比较。
                    if (newDate.getTime() > df.parse(customTime1).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        page.addTargetRequest(url);
                        System.out.println("企业URL_COMPANY_ARTICLE:dataStr=" + newDate.getTime());
                        System.out.println(url);
                        count++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }else if(URL_TOPIC_ARTICLE.equals(articleRegex)){
            List<Selectable> selectableList = page.getHtml().xpath("//div[@class='catlist']/ul/li").nodes();
            nodeNum = selectableList.size();
            System.out.println("nodeNum:" + nodeNum);
            //遍历每个链接所在的节点，根据日期规则，选择符合规则链接并加入页面访问列表中
            for (Selectable selectable : selectableList) {
                //获取内容页链接
                String url = selectable.links().regex(articleRegex).toString();
                //获取内容页的发布时间
                String dateStr = selectable.xpath("//em/text()").toString();

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                try {
                    Date newDate = df.parse(dateStr);
//                    System.out.println("df.parse(customTime2).getTime():"+df.parse(customTime2));
                    System.out.println("企业URL_COMPANY_ARTICLE:哈哈dataStr=" + newDate);
//                    System.out.println("df.parse(customTime3).getTime():"+df.parse(customTime3));
                    //发布时间与预定义的更新时间进行比较。
                    if (newDate.getTime() > df.parse(customTime1).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        page.addTargetRequest(url);
                        System.out.println("企业URL_COMPANY_ARTICLE:dataStr=" + newDate.getTime());
                        System.out.println(url);
                        count++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("count:" + count);
        //只有在当页链接全部符合更新规则时，才抓取下一页的链接
        if (count == nodeNum) {
            List<Selectable> listUrlList = page.getHtml().xpath("//div[@class='pages']/a").nodes();
            //遍历列表page的节点
            for (Selectable select : listUrlList) {
                //获取列表页链接
                String listUrl = select.xpath("//a/@href").toString();
                System.out.println("listUrl:" + listUrl);
                //获取名字（1,2,3，。。。，下一页）
                String listText = select.xpath("//a/text()").toString();
                System.out.println("listText:" + listText);
                //如果是“下一页”，就把这个链接加入到队列中。
                if (listText.equals(nextPage)) {
                    page.addTargetRequest(listUrl);
                    System.out.println("listUrl:" + listUrl);
                    System.out.println("listText:" + listText);
                }
            }
        }
    }

    public static void main(String[] args) {

        //customTime1 = "20160822";
        customTime0 = args[0];
        customTime1 = args[1]+" "+args[2];



        //读取配置文件的示例
        String collection = ParamsConfigurationUtil.instance.getParamString("mongodb.collection.RobotChina");

        //分类id依次对应：行业资讯，展会资讯
        String[] newsCatArr = new String[]{"937", "1140"};
        //分类id依次对应：法律专题，行业专题，技术专题，企业专题，展会专题
        String[] topicCatArr = new String[]{"1429", "187", "188", "189", "190"};
        for (String aNewsCatArr : newsCatArr) {
            Spider.create(new RobotChinaPage())
                    .addUrl("http://www.robot-china.com/news/list-" + aNewsCatArr + "-1.html")
                    .addPipeline(new MongoDBPipeline(collection))
                    //.addPipeline(new JsonFilePipeline("/bigData/robot0829"))
                    .run();
        }
        for (String aTopicCatArr : topicCatArr) {
            Spider.create(new RobotChinaPage())
                    .addUrl("http://www.robot-china.com/zhuanti/list-" + aTopicCatArr + "-1.html")
                    .addPipeline(new MongoDBPipeline(collection))
                    //.addPipeline(new JsonFilePipeline("/bigData/robot0817"))
                    .run();
        }
        //企业新闻
        Spider.create(new RobotChinaPage())
                .addUrl("http://www.robot-china.com/company/news-htm-page.html")
                .addPipeline(new MongoDBPipeline(collection))
                .run();
//        Spider.create(new org.csip.bigData.crawler.service.RobotChinaPage()).addUrl("http://www.robot-china.com/news/201608/31/35391.html").addPipeline(new JsonFilePipeline("/bigData/robot0831")).run();
    }

}


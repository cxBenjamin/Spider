package org.csip.bigData.crawler.service;

import org.csip.bigData.crawler.dao.MongoDBPipeline;
import org.csip.bigData.crawler.util.MD5Util;
import org.csip.bigData.crawler.util.ParamsConfigurationUtil;
import org.csip.bigData.crawler.util.dataProcessUtil.*;
import org.csip.bigData.crawler.util.spiderUtil.DateUtil;
import org.csip.bigData.crawler.util.spiderUtil.UrlcodeUtil;
import org.csip.bigData.crawler.util.spiderUtil.UserAgentUtil;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.apache.commons.lang.StringUtils.trim;

/**
 * Created by csip on 7/28/16.
 * 深圳市机器人协会 www.szrobot.org.cn
 */
public class SzRobotPage implements PageProcessor {
    private static String customTime = null;
    private static final List<String> URLlist = new ArrayList<String>();
    //行业新闻列表
    private static final String URL_NEWS = "http://www\\.szrobot\\.org\\.cn/NewsClass.asp\\?BigClass=行业新闻&SmallClass=&page=\\d+";
    private static final String URL_NEWS_LIST = "http://www\\.szrobot\\.org\\.cn/NewsClass.asp\\?BigClass=%D0%D0%D2%B5%D0%C2%CE%C5&SmallClass=&page=\\d+";
    //通知公告列表
    private static final String URL_NOTICE = "http://www\\.szrobot\\.org\\.cn/NewsClass.asp\\?BigClass=通知公告&SmallClass=&page=\\d+";
    private static final String URL_NOTICE_LIST = "http://www\\.szrobot\\.org\\.cn/NewsClass.asp\\?BigClass=%CD%A8%D6%AA%B9%AB%B8%E6&SmallClass=&page=\\d+";
    //文章页
    private static final String URL_ARTICLE = "http://www\\.szrobot\\.org\\.cn/shownews\\.asp\\?id=\\d+";

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent(UserAgentUtil.getUserAgent());

    @Override
    public void process(Page page) {
        //行业新闻列表页
        if (page.getUrl().regex(URL_NEWS_LIST).match() || page.getUrl().regex(URL_NEWS).match()) {
            //page.addTargetRequests(page.getHtml().xpath("//td[@class=\"line23\"]").links().regex(URL_ARTICLE).all());
            /*List<String> links =page.getHtml().xpath("//td[@class=\"line23\"]").links().regex(URL_ARTICLE).all();
            page.addTargetRequests(links);*/
            //增量更新抓取下一页
            increaseUpdate(page);
            /*List<String> links_post =page.getHtml().links().regex(URL_NEWS).all();
            links_post= UrlcodeUtil.encoderStrList(links_post);
            page.addTargetRequests(links_post);*/
            //通知公告列表页
        } else if (page.getUrl().regex(URL_NOTICE).match() || page.getUrl().regex(URL_NOTICE_LIST).match()) {
            /*List<String> links =page.getHtml().xpath("//td[@class=\"line23\"]").links().regex(URL_ARTICLE).all();
            page.addTargetRequests(links);
            List<String> links_post =page.getHtml().links().regex(URL_NOTICE).all();
            links_post= UrlcodeUtil.encoderStrList(links_post);
            page.addTargetRequests(links_post);*/
            //增量更新抓取下一页
            increaseUpdate(page);
            //文章页
        } else if (page.getUrl().regex(URL_ARTICLE).match()) {
            page.putField("id", "cn.org.szrobot.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url", page.getUrl().toString());
            System.out.println("getURL:" + page.getUrl());
            String title = page.getHtml().xpath("//td[@class='STYLE23']/b/font/text()").toString();
            page.putField("title", title);
            System.out.println(title);
            String[] publish = page.getHtml().xpath("//tr[@align='center']/td/text()").replace(" 阅读：次", "").toString().split("发布时间：", 2);
            if (null != publish && publish.length == 2) {
                String publishTimeStr = trim(publish[1]);
                if (publishTimeStr.length() > 12) {//有时分秒
                    page.putField("publish_time", DateUtil.parseTimeLikeSlash(publishTimeStr));
                } else {//只有年月日
                    page.putField("publish_time", DateUtil.parseDateLikeSlash(publishTimeStr) + " 00:00:00");
                }
                System.out.println(publishTimeStr);
            }

            page.putField("source", "深圳市机器人协会");
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
            page.putField("author", trim(publish[0].replace("发布者：", "")));
            //page.putField("digest","");
            String content = page.getHtml().xpath("//*[@class='black']").toString();
            //去掉所有html标签的干净文本
            String cleanContent = CleanTextUtil.getCleanText(content);
            page.putField("cleanContent", cleanContent);
            try {
                SimHash simHash = new SimHash(cleanContent, 64);
                page.putField("simhash", simHash.get64strSimHash());
            } catch (IOException e) {
                e.printStackTrace();
            }
            ArrayList<String> segments = HanlpUtil.instance.getSegment(cleanContent);
            StringBuilder sb = new StringBuilder();
            for (String segment : segments) {
                sb.append(segment).append(" ");
            }
            page.putField("chineseSegment", sb.toString());

            String source_images = page.getHtml().xpath("//*[@id='fontzoom']//img/@src").all().toString();
            String[] imageArr = source_images.replace("[", "").replace("]", "").split(",");
            int imalength = imageArr.length;
            if (imageArr[imageArr.length - 1] == null || imageArr[imageArr.length - 1].equals("")) {
                imalength = 0;
            }
            for (int i = 0; i < imalength; i++) {
                String img_one = imageArr[i].trim();
                content = content.replace(img_one, "images[" + i + "]");
            }
            page.putField("content", ContentUtil.removeTags(content));
            page.putField("source_tag", "新闻内容");
            //page.putField("category", "动态");
            page.putField("images", page.getHtml().xpath("//*[@id='fontzoom']//img/@src")
                    .replace("/admin", "http://www.szrobot.org.cn/admin").all().toString());
            page.putField("source_content", page.getRawText());
            page.putField("from_site", "深圳市机器人协会www.szrobot.org.cn");

            page.putField("category", ArticleCategoryUtil.instance.getCategory(title));
            page.putField("digest", HanlpUtil.instance.getPartWordsSummary(cleanContent));
            ArrayList<String> sourceTagList = new ArrayList<String>();
            int number = 10;
            ArrayList<String> keywords = KeywordExtractUtil.getKeyword(cleanContent, sourceTagList, number);
            HashSet<String> sets = new HashSet<String>();
            for (String keyword : keywords) {
                sets.add(keyword);
            }
            StringBuilder setSb = new StringBuilder();
            for (String set : sets) {
                setSb.append(set).append(" ");
            }
            page.putField("tag", setSb.toString().trim());
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    /**
     * 增量更新
     *
     * @param page
     */
    private static void increaseUpdate(Page page) {

        String nextPage = "下一页";
        //获取内容页的链接所在的节点
        List<Selectable> selectableList = page.getHtml().xpath("//td[@class=\"line23\"]/table/tbody/tr").nodes();
        int nodeNum = selectableList.size()-1;
        System.out.println("nodeNum:" + nodeNum);
        //用于计数当页符合规则链接数，如果当页链接全部抓取，则需要抓取下一页链接
        int count = 0;
        //遍历每个链接所在的节点，根据日期规则，选择符合规则链接并加入页面访问列表中
        for (Selectable selectable : selectableList) {
            //获取内容页链接
            String url = selectable.links().regex(URL_ARTICLE).toString();
            //获取内容页的发布时间。
            String date = selectable.xpath("//td[3]/font[1]/text()").toString();
            if (!"".equals(date) && date != null) {
                date = date.replace("[", "").replace("] (点击)", "").trim();
                SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
                try {
                    Date newDate = df.parse(date);
                    //发布时间与预定义的更新时间进行比较。
                    if (newDate.getTime() > df.parse(customTime).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        page.addTargetRequest(url);
                        //System.out.println(url);
                        count++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        }
        System.out.println("count:"+count);
        //只有在当页链接全部符合更新规则时，才抓取下一页的链接
        if (count == nodeNum) {
            List<Selectable> listUrlList = page.getHtml().xpath("/html/body/table[3]/tbody/tr/td/table[3]/tbody/tr/td[2]/table[2]/tbody/tr/td/table/tbody/tr[21]/td/form").nodes();
            //遍历列表page的节点
            for (Selectable select : listUrlList) {
                //获取列表页链接
                String listUrl = select.xpath("//a/@href").toString();
                //获取名字（1,2,3，。。。，下一页）
                String listText = select.xpath("//a/text()").toString();
                //如果是“下一页”，就把这个链接加入到队列中。
                if (listText.equals(nextPage)) {
                    page.addTargetRequest(UrlcodeUtil.encodeSingleStr(listUrl));
                    //System.out.println("listUrl:" + listUrl);
                    //System.out.println("listText:" + listText);
                }
            }
        }
    }

    public static void main(String[] args) {
        //读取配置文件的示例
//        customTime = args[0];
        customTime = "2006/01/01";
        String collection = ParamsConfigurationUtil.instance.getParamString("mongodb.collection.SzRobot");

        //行业新闻
        URLlist.add("http://www.szrobot.org.cn/NewsClass.asp?BigClass=%D0%D0%D2%B5%D0%C2%CE%C5&SmallClass=&page=1");

        //通知公告
        URLlist.add("http://www.szrobot.org.cn/NewsClass.asp?BigClass=%CD%A8%D6%AA%B9%AB%B8%E6&SmallClass=&page=1");

        /*EC2ConnectUtil ec2ConnectUtil=new EC2ConnectUtil();
        ec2ConnectUtil.Ec2ConnectStart();*/
        int count = 0;
        for (String url : URLlist) {
            System.out.println("列表页" + URLlist.get(count));
            count++;
            Spider.create(new SzRobotPage())
                    .addUrl(url)
                    //.addPipeline(new JsonFilePipeline("E://test/bigData/robot0829"))
                    .addPipeline(new MongoDBPipeline(collection))
                    .thread(5)
                    .run();
        }
//        ec2ConnectUtil.Ec2ConnectStop();
//        Spider.create(new org.csip.bigData.crawler.service.SzRobotPage()).addUrl("http://www.szrobot.org.cn/shownews.asp?id=213").addPipeline(new JsonFilePipeline("/bigData/robot0815/")).addPipeline(new MongoDBPipeline("172.16.21.165",27017,"BigData","SzRobot")).run();

    }

    /*public static void main(String[] args) throws JMException {

        String url_one;
        ArrayList<String> urls=new ArrayList<String>();
        for(int i=1;i<=3;i++) {
            url_one="http://www.szrobot.org.cn/NewsClass.asp?BigClass=%D0%D0%D2%B5%D0%C2%CE%C5&SmallClass=";
            url_one+="&page="+String.valueOf(i);
            urls.add(url_one);
            System.out.println(url_one);

        }
        Spider szrobotSpider = Spider.create(new org.csip.bigData.crawler.service.SzRobotPage()).startUrls(urls).addPipeline(new JsonFilePipeline("/bigData/robot"));
//        Spider.create(new org.csip.bigData.crawler.service.SzRobotPage()).startUrls(urls)
        //加入了JMX监听模式
        SpiderMonitor.instance().register(szrobotSpider);
        szrobotSpider.start();

    }*/
    /**
     * 开启爬虫实例
     *
     * @param reqUrl
     * @param totalPage
     */
/*    private static void spiderCreate(String reqUrl, int totalPage) {
        String url_one;
        for (int i = 1; i < totalPage; i++) {
            url_one = reqUrl  + String.valueOf(i);
            System.out.println(url_one);
            Spider.create(new org.csip.bigData.crawler.service.SzRobotPage()).addUrl(url_one).addPipeline(new MongoDBPipeline("54.223.50.219",27017,"BigData","test")).thread(5).run();
        }
    }*/

}

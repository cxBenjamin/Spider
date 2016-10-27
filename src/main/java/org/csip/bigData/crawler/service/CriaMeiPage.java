package org.csip.bigData.crawler.service;

import com.hankcs.hanlp.HanLP;
import org.csip.bigData.crawler.dao.MongoDBPipeline;
import org.csip.bigData.crawler.util.MD5Util;
import org.csip.bigData.crawler.util.ParamsConfigurationUtil;
import org.csip.bigData.crawler.util.dataProcessUtil.*;
import org.csip.bigData.crawler.util.spiderUtil.DateUtil;
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

/**
 * Created by csip on 7/28/16.
 * 中国机器人产业联盟（cria.mei.net.cn）
 */
public class CriaMeiPage implements PageProcessor {
    static String updateTime=null;
    //联盟新闻列表
    private static final String URL_LMXW_LIST = "http://cria.mei.net.cn/lmzx.asp\\?PG=\\d+";
    //联盟活动列表
    private static final String URL_LMHD_LIST = "http://cria.mei.net.cn/lmhd.asp\\?PG=\\d+";
    //行业资讯列表
    private static final String URL_HYZX_LIST = "http://cria.mei.net.cn/hyxx.asp\\?PG=\\d+";
    //政策法规列表
    private static final String URL_ZCFG_LIST = "http://cria.mei.net.cn/hyxx.asp\\?lm=/1211&PG=\\d+";
    //行业标准列表
    private static final String URL_HYBZ_LIST = "http://cria.mei.net.cn/hyxx.asp\\?lm=/1212&PG=\\d+";
    //产品技术列表
    private static final String URL_CPJS_LIST = "http://cria.mei.net.cn/hyxx.asp\\?lm=/1213&PG=\\d+";
    //成员发布列表
    private static final String URL_CYFB_LIST = "http://cria.mei.net.cn/cyfb.asp\\?PG=\\d+";
    //文章页
    private static final String URL_ARTICLE = "http://cria.mei.net.cn/news.asp\\?vid=\\d+";
    private static String NEXT_PAGE = "[后一页]";
    private static String LMZX_LIST = "http://cria.mei.net.cn/lmzx.asp?PG=";//联盟资讯
    private static String LMHD_LIST = "http://cria.mei.net.cn/lmhd.asp?PG=";//联盟活动
    private static String HYZX_LIST = "http://cria.mei.net.cn/hyxx.asp?PG=";//行业资讯
    private static String ZCFG_LIST = "http://cria.mei.net.cn/hyxx.asp?lm=/1211&PG=";//政策法规
    private static String HYBZ_LIST = "http://cria.mei.net.cn/hyxx.asp?lm=/1212&PG=";//行业标准
    private static String CPJS_LIST = "http://cria.mei.net.cn/hyxx.asp?lm=/1213&PG=";//产品技术与解决方案
    private static String CYFB_LIST = "http://cria.mei.net.cn/cyfb.asp?PG=";//成员发布
    private Site site = Site.me().setRetryTimes(3).setSleepTime(2000).setUserAgent(UserAgentUtil.getUserAgent());

    @Override
    public void process(Page page) {
        //联盟新闻列表页
        if (page.getUrl().regex(URL_LMXW_LIST).match() || page.getUrl().regex(URL_LMHD_LIST).match()
                || page.getUrl().regex(URL_HYZX_LIST).match() || page.getUrl().regex(URL_ZCFG_LIST).match()
                || page.getUrl().regex(URL_HYBZ_LIST).match() || page.getUrl().regex(URL_CPJS_LIST).match()
                || page.getUrl().regex(URL_CYFB_LIST).match()) {

            //List<String> contentLinks = page.getHtml().xpath("//div[@class=\"wzlb\"]").links().regex(URL_ARTICLE).all();
            //page.addTargetRequests(contentLinks);

            System.out.println(page.getHtml().xpath("//div[@id='class-title']").toString());

            List<Selectable> contentNodes = page.getHtml().xpath("//div[@class='wzlb']/ul/form/li").nodes();
            List<String> contentLinks=new ArrayList<String>();

            for(Selectable select: contentNodes){
                String url=select.links().regex(URL_ARTICLE).toString();
                String date=select.xpath("//span/text()").toString()+" 00:00";
                System.out.println(date);
                SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm");

                try {
                    Date newDate=df.parse(date);
                    //发布时间与预定义的更新时间进行比较。
//                    String standTime=updateTime+" 00:00";
                    if(newDate.getTime()>df.parse(updateTime).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        contentLinks.add(url);
                        System.out.println(url);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

            page.addTargetRequests(contentLinks);

            //如果link的数量小于node数量，则不再抓取下一页链接（link最多等于node数量）
            if(contentLinks.size()==contentNodes.size()){

                List<Selectable> listUrlList = page.getHtml().xpath("//div[@class=pagination]/a").nodes();
                //System.out.println(listUrlList);

                for (Selectable select : listUrlList) {
                    //String listUrl=select.xpath("//a/@href").toString();
                    String listText = select.xpath("//a/font/text()").toString();
                    System.out.println("text=" + listText);
                    if (listText.equals(NEXT_PAGE)) {
                        String pageUrl = page.getUrl().toString();
                        String pageNum = pageUrl.substring(pageUrl.lastIndexOf("=") + 1);
                        System.out.println("pageNum=" + pageNum);
                        if (page.getUrl().regex(URL_LMXW_LIST).match()) {
                            page.addTargetRequest(LMZX_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        } else if (page.getUrl().regex(URL_LMHD_LIST).match()) {
                            page.addTargetRequest(LMHD_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        } else if (page.getUrl().regex(URL_HYZX_LIST).match()) {
                            page.addTargetRequest(HYZX_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        } else if (page.getUrl().regex(URL_ZCFG_LIST).match()) {
                            page.addTargetRequest(ZCFG_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        } else if (page.getUrl().regex(URL_HYBZ_LIST).match()) {
                            page.addTargetRequest(HYBZ_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        } else if (page.getUrl().regex(URL_CPJS_LIST).match()) {
                            page.addTargetRequest(CPJS_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        } else if (page.getUrl().regex(URL_CYFB_LIST).match()) {
                            page.addTargetRequest(CYFB_LIST + String.valueOf(Integer.parseInt(pageNum) + 1));
                        }
                    }
                }

            }
            //文章页
        } else if (page.getUrl().regex(URL_ARTICLE).match()) {
            page.putField("id", "cn.net.mei.cria/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url",page.getUrl().toString());
            String title=page.getHtml().xpath("//div[@class='content']/h1/text()").toString();
            page.putField("title",title);
            System.out.println("title:"+title);
            String sourceInfo = page.getHtml().xpath("//div[@class='author']/text()").toString();
            if (!sourceInfo.isEmpty()) {
                String[] sourceArr = sourceInfo.split("来源:", 2);
                String publish_time = sourceArr[0].replace("发布时间：", "").replace("\u00A0\u00A0\u00A0\u00A0", "");
                page.putField("publish_time", publish_time + " 00:00:00");
                System.out.println("publish_time:"+publish_time + " 00:00:00");
                String source = sourceArr[1].replace("来源：", "").replace(" ", "");
                page.putField("source", source);
                page.putField("author", source);
            }
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
            //page.putField("author", source);
            String content = page.getHtml().xpath("//div[@class='txt']").toString();

            //去掉所有html标签的干净文本
            String cleanContent = CleanTextUtil.getCleanText(content);
            page.putField("cleanContent", cleanContent);
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
            page.putField("digest", HanlpUtil.instance.getPartWordsSummary(cleanContent));
//            page.putField("source_tag", "联盟新闻");
            page.putField("category", ArticleCategoryUtil.instance.getCategory( title ));
            String source_images = page.getHtml().xpath("//div[@class='txt']//img/@src").all().toString();
            String[] imageArr = source_images.replace("[", "").replace("]", "").split(",");
            int imalength=imageArr.length;
            if(imageArr[imageArr.length-1]==null||imageArr[imageArr.length-1].equals("")){
                imalength=0;
            }
            for (int i = 0; i < imalength; i++) {
                String img_one = imageArr[i].trim();
                content = content.replace(img_one, "images[" + i + "]");
            }

            page.putField("content", ContentUtil.removeTags(content));

            page.putField("images", page.getHtml().xpath("//div[@class='txt']//img/@src").replace("/admin","http://cria.mei.net.cn/admin").all());

            page.putField("source_content", page.getRawText());
            page.putField("from_site", "中国机器人产业联盟cria.mei.net.cn");
            ArrayList<String> sourceTagList=new ArrayList<String>();
//            sourceTagList.add("资讯");
//            sourceTagList.add("政策");
            int number=10;
            ArrayList<String> keywords= KeywordExtractUtil.getKeyword(CleanTextUtil.getCleanText(content),sourceTagList,number);
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

    @Override
    public Site getSite()
    {
        return site;
    }
    public static void main(String[] args) {

        //读取配置文件的示例
        updateTime=args[0]+" "+args[1];
        //updateTime = "2016-09-27 01:00";
        String collection = ParamsConfigurationUtil.instance.getParamString("mongodb.collection.CriaMei");

        //列表类型依次为：联盟新闻，联盟活动，行业资讯，政策法规，行业标准，产品技术与解决方案，成员发布
        String[] listUrlArr = new String[]{LMZX_LIST, LMHD_LIST, HYZX_LIST, ZCFG_LIST, HYBZ_LIST, CPJS_LIST, CYFB_LIST};
        List<String> urls=new ArrayList<String>(  );

        for (String aUrl : listUrlArr) {
            String url=aUrl+"1";
            urls.add( url );
        }
        Spider.create(new CriaMeiPage())
                .startUrls( urls )
                //.addPipeline(new JsonFilePipeline("E://test/bigData/robot08"))
                .addPipeline(new MongoDBPipeline(collection))
                .run();
        //Spider.create(new org.csip.bigData.crawler.service.CriaMeiPage()).addUrl("http://cria.mei.net.cn/news.asp?vid=3554").addPipeline(new JsonFilePipeline("/bigData/robot0831")).run();
    }
}

package org.csip.bigData.crawler.service;

import org.csip.bigData.crawler.dao.MongoDBPipeline;
import org.csip.bigData.crawler.util.MD5Util;
import org.csip.bigData.crawler.util.ParamsConfigurationUtil;
import org.csip.bigData.crawler.util.dataProcessUtil.*;
import org.csip.bigData.crawler.util.spiderUtil.DateUtil;
import org.csip.bigData.crawler.util.spiderUtil.UserAgentUtil;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by csip on 7/27/16.
 * 中国机器人行业协会 www.robotcn.org
 */
public class RobotCnOrgPage implements PageProcessor {

    private static String customTime = null;
    private static final List<String> URLlist = new ArrayList<String>();
    //行业新闻列表
    private static final String URL_NEWS_LIST = "http://www\\.robotcn\\.org/news/list.php\\?catid=400&\\w+=\\d+";
    //企业报道列表
    private static final String URL_REPORT_LIST = "http://www\\.robotcn\\.org/news/list.php\\?catid=403&\\w+=\\d+";
    //行业市场研究列表
    private static final String URL_RESEARCH_LIST = "http://www\\.robotcn\\.org/news/list.php\\?catid=401&\\w+=\\d+";
    //新产品新技术列表
    private static final String URL_PRODUCT_LIST = "http://www\\.robotcn\\.org/news/list.php\\?catid=402&\\w+=\\d+";
    //招标咨询列表
    private static final String URL_CONSULT_LIST = "http://www\\.robotcn\\.org/news/list.php\\?catid=421&\\w+=\\d+";
    //热点招标列表
    private static final String URL_HOTSPOT_LIST = "http://www\\.robotcn\\.org/news/list.php\\?catid=422&\\w+=\\d+";
    //咨询文章页
    private static final String URL_INFORMATION = "http://www\\.robotcn\\.org/news/show.php\\?itemid=\\d+";

    //机器人行情列表
    private static final String URL_MARKET_LIST = "http://www\\.robotcn\\.org/quote/416/\\d+\\.html";
    //法律法规列表
    private static final String URL_LAW_LIST = "http://www\\.robotcn\\.org/quote/417/\\d+\\.html";
    //焦点行情列表
    private static final String URL_FOCUS_LIST = "http://www\\.robotcn\\.org/quote/418/\\d+\\.html";
    //其它行情列表
    private static final String URL_OTHER_LIST = "http://www\\.robotcn\\.org/quote/419/\\d+\\.html";
    //行情文章页
    private static final String URL_MARKET = "http://www\\.robotcn\\.org/quote/\\d+/\\d+/\\d+\\.html";

    private Site site = Site.me().setRetryTimes(3).setSleepTime(10000).setUserAgent(UserAgentUtil.getUserAgent());

    private static Map<String, Object> getcontentTime(Selectable select, String regex) {
        Map<String, Object> cpMap = new HashMap<String, Object>();
        //获取列表页链接
        String listUrl = select.links().regex(regex).toString();
        //System.out.println("url:"+listUrl);
        //获取标题中的时间
        String contentTime = select.xpath("//span[@class=\"f_r px11 f_gray\"]/text()").toString();
        String contentUrl = select.xpath("//a/@href").toString();
        //System.out.println("时间:"+contentTime);
        //System.out.println("链接:"+contentUrl);
        String date = contentTime + ":00";
        //System.out.println("date:" + date);
        //利用Request来加入publish_time属性
        Request request = new Request(listUrl);
        request.putExtra("publish_time", date);
        cpMap.put("request", request);
        cpMap.put("url", contentUrl);
        cpMap.put("date", contentTime);
        return cpMap;
    }

    public void getandAddLinks(Page page, String urlRegex) {
        List<Selectable> listUrlList = page.getHtml().xpath("//div[@class=\"catlist\"]//li[@class='catlist_li']").nodes();

        List<String> LinkList = new ArrayList<String>();

        //遍历列表page的节点
        for (Selectable select : listUrlList) {
            Map<String, Object> cMap = getcontentTime(select, urlRegex);

            String date = cMap.get("date").toString();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            try {
                Date newDate = df.parse(date);
                //发布时间与预定义的更新时间进行比较。
                if (newDate.getTime() > df.parse(customTime).getTime()) {
                    //如果是更新的，就把这个内容页的链接加入到队列中，
                    LinkList.add(cMap.get("url").toString());
                    //System.out.println("符合的链接："+cMap.get("url").toString());
                    page.addTargetRequest((Request) cMap.get("request"));
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }

            //page.addTargetRequest((Request) cMap.get("request"));
            //page.addTargetRequest(cMap.get("url").toString());
        }

        if (LinkList.size() == listUrlList.size()) {
            //获取下一页
            List<Selectable> pageNode = page.getHtml().xpath("//div[@class='pages']/a").nodes();
            for (Selectable node : pageNode) {
                if (node.xpath("//a/text()").toString().replaceAll("\\u00A0", "").equals("下一页»")) {
                    String link = node.xpath("//a/@href").toString();
                    //System.out.println("获得下一页链接"+link);
                    page.addTargetRequest(link);
                    break;
                }
            }
        }


    }

    @Override
    public void process(Page page) {
        //行业新闻列表页
        if (page.getUrl().regex(URL_NEWS_LIST).match()) {
            //page.addTargetRequests(page.getHtml().xpath("//div[@class=\"catlist\"]").links().regex(URL_INFORMATION).all());
            getandAddLinks(page, URL_INFORMATION);

            //企业报道列表列表页
        } else if (page.getUrl().regex(URL_REPORT_LIST).match()) {
            getandAddLinks(page, URL_INFORMATION);
            //行业市场研究列表页
        } else if (page.getUrl().regex(URL_RESEARCH_LIST).match()) {
            getandAddLinks(page, URL_INFORMATION);
            //新产品新技术列表页
        } else if (page.getUrl().regex(URL_PRODUCT_LIST).match()) {
            getandAddLinks(page, URL_INFORMATION);
            //招标咨询列表页
        } else if (page.getUrl().regex(URL_CONSULT_LIST).match()) {
            getandAddLinks(page, URL_INFORMATION);
            //热点招标列表页
        } else if (page.getUrl().regex(URL_HOTSPOT_LIST).match()) {
            getandAddLinks(page, URL_INFORMATION);
            //机器人行情列表页
        } else if (page.getUrl().regex(URL_MARKET_LIST).match()) {
            getandAddLinks(page, URL_MARKET);
            //法律法规列表页
        } else if (page.getUrl().regex(URL_LAW_LIST).match()) {
            getandAddLinks(page, URL_MARKET);
            //焦点行情列表页
        } else if (page.getUrl().regex(URL_FOCUS_LIST).match()) {
            getandAddLinks(page, URL_MARKET);
            //其它行情列表页
        } else if (page.getUrl().regex(URL_OTHER_LIST).match()) {
            //page.addTargetRequests(page.getHtml().xpath("//div[@class=\"catlist\"]").links().regex(URL_MARKET).all());
            /*List<Selectable> listUrlList=page.getHtml().xpath("//div[@class=\"catlist\"]//li[@class='catlist_li']").nodes();
            List<String> links_post = page.getHtml().xpath("//div[@class=\"pages\"]").links().regex(URL_OTHER_LIST).all();
            page.addTargetRequests(links_post);
            //System.out.println(listUrlList.size());
            //遍历列表page的节点
            for(Selectable select:listUrlList) {
                Map<String,Object> cMap = getcontentTime(select,URL_MARKET);
                page.addTargetRequest((Request) cMap.get("request"));
                page.addTargetRequest(cMap.get("url").toString());
            }*/
            getandAddLinks(page, URL_MARKET);
            //咨询文章页
        } else if (page.getUrl().regex(URL_INFORMATION).match() || page.getUrl().regex(URL_MARKET).match()) {
            page.putField("id", "cn.org.robotcn.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url", page.getUrl().toString());
            //System.out.println("getURL:" + page.getUrl());
            String title = page.getHtml().xpath("//*[@id='title']/text()").toString();
            page.putField("title", title);
            //System.out.println(title);
            //String publish = page.getHtml().xpath("/html/body/div[8]/div[1]/div/div[2]/text()").replace("发布日期：", "").replace("浏览次数：", "").toString();
            //page.putField("publish_time", trim(publish) + " 00:00:00");
            page.putField("publish_time", page.getRequest().getExtra("publish_time").toString());
            //System.out.println("publish_time:"+page.getRequest().getExtra("publish_time").toString());
            page.putField("source", "中国机器人行业协会");
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
            page.putField("author", "中国机器人行业协会");
            //page.putField("digest", page.getHtml().xpath("//div[@class='introduce']/text()").replace("核心提示：", "").toString());
//            System.out.println("文章的来源字段："+page.getHtml().xpath("//ul[@id='plshare']/li[3]/text()").replace("来源： ","").toString());
            StringBuffer content = new StringBuffer();
            content.append(page.getHtml().xpath("//div[@class='content']").toString());
            //去掉所有html标签的干净文本
            String cleanContent1 = CleanTextUtil.getCleanText(content.toString());
            String cleanContent = cleanContent1.replaceAll("\\'\\&gt;", "");

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

            String source_images = page.getHtml().xpath("//div[@id='article']/p/img/@original").all().toString();
            String[] imageArr = source_images.replace("[", "").replace("]", "").split(",");
            int imalength = imageArr.length;
            if (imageArr[imageArr.length - 1] == null || imageArr[imageArr.length - 1].equals("")) {
                imalength = 0;
            }

            for (int i = 0; i < imalength; i++) {
                String img_one = imageArr[i].trim();
                //System.out.println("imagr:"+img_one);
                ///content = content.replace(img_one, "images[" + i + "]");
                int firstIndex = content.indexOf(img_one);
                content = content.replace(firstIndex, img_one.length() + firstIndex, "images[" + i + "]");
                //System.out.println("i:"+i);
            }
            page.putField("content", ContentUtil.removeTags(content.toString()));
            page.putField("images", page.getHtml().xpath("//div[@id='article']/p/img/@original").all().toString());
            page.putField("source_tag", "行业新闻");
            //page.putField("category", "动态");
            page.putField("source_content", page.getHtml().toString());
            page.putField("from_site", "中国机器人行业协会www.robotcn.org");

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

    public static void main(String[] args) {

        customTime = args[0] + " " + args[1];
        //customTime = "2016-10-16 15:21";
        //读取配置文件的示例
        String collection = ParamsConfigurationUtil.instance.getParamString("mongodb.collection.RobotCnOrg");

        //行业新闻
        URLlist.add("http://www.robotcn.org/news/list.php?catid=400&page=1");
        //企业报道
        URLlist.add("http://www.robotcn.org/news/list.php?catid=403&page=1");
        //行业市场研究
        URLlist.add("http://www.robotcn.org/news/list.php?catid=401&page=1");
        //新产品新技术
        URLlist.add("http://www.robotcn.org/news/list.php?catid=402&page=1");
        //招标咨询
        URLlist.add("http://www.robotcn.org/news/list.php?catid=421&page=1");
        //热点招标
        URLlist.add("http://www.robotcn.org/news/list.php?catid=422&page=1");
        //机器人行情
        URLlist.add("http://www.robotcn.org/quote/416/1.html");
        //法律法规
        URLlist.add("http://www.robotcn.org/quote/417/1.html");
        //焦点行情
        URLlist.add("http://www.robotcn.org/quote/418/1.html");
        //其它行情
        URLlist.add("http://www.robotcn.org/quote/419/1.html");



        //System.out.println("列表页" + URLlist.get(count));

        Spider.create(new RobotCnOrgPage())
                .startUrls(URLlist)
                //.addPipeline(new JsonFilePipeline("E:/bigData/robot3"))
                .addPipeline(new MongoDBPipeline(collection))
                .run();



/*        Spider.create(new org.csip.bigData.crawler.service.RobotCnOrgPage())
                .addUrl("http://www.robotcn.org/news/show.php?itemid=7328")
                .addPipeline(new JsonFilePipeline("/bigData/robot3"))
                //.addPipeline(new MongoDBPipeline(host, port, database, collection))
                .run();
*/
    }
}


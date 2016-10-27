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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by csip on 7/28/16.
 * 库卡机器人（kuka.robot-china.com）
 */
public class KukaRobotPage implements PageProcessor{
    private static String customTime = null;
    private static final String URL_LIST = "http://kuka\\.robot-china\\.com/news/page-\\d+.shtml";

    private static final String URL_POST = "http://kuka\\.robot-china\\.com/news/itemid-\\d+.shtml";

    private Site site = Site.me().setRetryTimes(3)
             .setSleepTime(1000).setUserAgent(UserAgentUtil.getUserAgent());

    private CleanTextUtil cleanTextUtil=new CleanTextUtil();

    @Override
    public void process(Page page) {
        //列表页
        if (page.getUrl().regex(URL_LIST).match()) {
            //List<String> links = page.getHtml().xpath("//div[@class=\"prize\"]").links().regex(URL_POST).all();
            //page.addTargetRequests(links);

            List<Selectable> listUrlList=page.getHtml().xpath("//li/h1/a").nodes();
            System.out.println(listUrlList.size());
            //遍历列表page的节点
            for(Selectable select:listUrlList) {
                //获取列表页链接
                String listUrl = select.links().regex(URL_POST).toString();
                //System.out.println("url:"+listUrl);

                //获取标题中的时间
                String listText = select.xpath("//a/text()").toString();
                //System.out.println("标题:"+listText.substring(listText.indexOf("(")+1,listText.indexOf(")")));

                String date = listText.substring(listText.lastIndexOf("(")+1,listText.lastIndexOf(")"))+" 00:00:00";
                System.out.println("date:"+date);
                SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm");

                try
                {
                    Date newDate=df.parse(date);
                    //发布时间与预定义的更新时间进行比较。
                    if(newDate.getTime()>df.parse(customTime).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中

                        //利用Request来加入publish_time属性
                        Request request=new Request(listUrl);
                        request.putExtra("publish_time", date);

                        page.addTargetRequest(request);


                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

            //System.out.println("抓取列表加载成功，长度为："+links.size());

            List<String> pageLinks = page.getHtml().xpath("//p[@class='intro_p']/a").links().regex(URL_LIST).all();
            page.addTargetRequests(pageLinks);

         //文章页
        } else if (page.getUrl().regex(URL_POST).match()){
            page.putField("id", "com.robot-china.kuka/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url",page.getUrl().toString());
            String title=page.getHtml().xpath("//div[@class='prize']/h1[1]/text()").toString();
            page.putField("title", title);
            System.out.println(title);
            //System.out.println(title.substring(title.indexOf("(")+1,title.indexOf(")")));
            page.putField("publish_time",page.getRequest().getExtra("publish_time").toString());
            System.out.println("publish_time:"+page.getRequest().getExtra("publish_time").toString());
            page.putField("source","库卡机器人(上海)");
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
            page.putField("author","库卡机器人(上海)");
            //page.putField("digest", "");

            String content1=page.getHtml().xpath("//div[@class='prize']").all().toString();
            String content = content1.replaceFirst("\\,|\\|","").replaceAll(title,"").trim();

            String source_images = page.getHtml().xpath("//div[@class='prize']//img/@src").all().toString();
            String[] imageArr = source_images.replace("[", "").replace("]", "").split(",");
            int imalength=imageArr.length;
            if(imageArr[imageArr.length-1]==null||imageArr[imageArr.length-1].equals("")){
                imalength=0;
            }
            for (int i = 0; i < imalength; i++) {
                String img_one = imageArr[i].trim();
                content = content.replace(img_one, "images[" + i + "]");
            }

            String cleanContent=CleanTextUtil.getCleanText(content);
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
            page.putField("content", ContentUtil.removeTags(content));

            page.putField("source_tag", "新闻");
            page.putField("category", ArticleCategoryUtil.instance.getCategory( title ));
            page.putField("images", page.getHtml().xpath("//div[@class='prize']//img/@src").all().toString());
            page.putField("source_content", page.getRawText());
            page.putField("from_site", "库卡机器人(上海)kuka.robot-china.com");

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
            System.out.print("抓取成功\n");
        }
    }

    @Override
    public Site getSite()
    {
        return site;
    }
    public static void main(String[] args) {
        //customTime = args[0]+" "+args[1];

        customTime = "2006-01-01 15:00";
        //读取配置文件的示例

        String collection= ParamsConfigurationUtil.instance.getParamString("mongodb.collection.KukaRobot");

        String url_one="http://kuka.robot-china.com/news/page-1.shtml";

        Spider.create(new KukaRobotPage())
                .addUrl(url_one)
                .addPipeline(new MongoDBPipeline( collection))
                //.addPipeline(new JsonFilePipeline("E://test/bigData/robot08789"))
                .run();
        }
//

}


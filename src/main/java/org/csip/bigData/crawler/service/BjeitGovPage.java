package org.csip.bigData.crawler.service;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by csip on 7/28/16.
 * 北京市经信委（www.bjeit.gov.cn/）
 */
public class BjeitGovPage implements PageProcessor {
    //private static final String URL_LIST = "http://www\\.bjeit\\.gov\\.cn\\:8080/oasearch/front/search.do\\?Query=%E6%9C%BA%E5%99%A8%E4%BA%BA\\&pageNo\\=\\d+";
    //http://www.bjeit.gov.cn:8080/oasearch/front/search.do\?pageNo\=\d+&orderField=publishdate&orderType=desc&Query=%E6%9C%BA%E5%99%A8%E4%BA%BA

    private static String customTime = null;
    private static final String URL_LIST ="http://www.bjeit.gov.cn:8080/oasearch/front/search.do\\?orderField=publishdate&orderType=desc&Query=%E6%9C%BA%E5%99%A8%E4%BA%BA&pageNo\\=\\d+";
    //private static final String URL_POST = "http://www\\.bjeit\\.gov\\.cn/(\\w+/)+\\d+.htm";
    //private static final String URL_POST = "http://www\\.bjeit\\.gov\\.cn/\\w+/\\w+/\\d+.htm";
    private static final String URL_POST = "http://www\\.bjeit\\.gov\\.cn/(\\w+/)+\\d+.htm";

    private Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setUserAgent(UserAgentUtil.getUserAgent());

    @Override
    public void process(Page page) {
        Pattern pattern = Pattern.compile(URL_POST);
        Matcher matcher = null;
        //列表页
        if (page.getUrl().regex(URL_LIST).match()) {

            //List<String> interimlist = page.getHtml().xpath("//div[@class=\"inform_search_text\"]")
                    //.links().all();
            //获取内容页的链接所在的节点
            List<Selectable> selectableList=page.getHtml().xpath("//div[@class='inform_search_text']/ul/li").nodes();

            List<String> links =new ArrayList<String>();

            //遍历每个链接所在的节点
            for(Selectable selectable:selectableList){
                //获取内容页链接
                String url = selectable.links().toString();

                String date=selectable.xpath("//dt/p/text()").toString()+" 00:00";
                SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm");

                try
                {
                    Date newDate=df.parse(date);
                    //发布时间与预定义的更新时间进行比较。
                    if(newDate.getTime()>df.parse(customTime).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        //page.addTargetRequest(url);
                        matcher = pattern.matcher(url);
                        if (matcher.matches()) {
                            links.add(url);
                        }
                        System.out.println(url);

                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            page.addTargetRequests(links);

            //当前页有内容才去抓下一页链接
            if(links.size()>0){

                String num=page.getHtml()
                        .xpath("//div[@class='paging']/div[@class='pagingrf']/a[@class='fy fymar']/@onclick")
                        .toString();
                String numb=num.substring(num.indexOf("(")+1,num.indexOf(")"));

                //String nextLink="http://www.bjeit.gov.cn:8080/oasearch/front/search.do?Query=%E6%9C%BA%E5%99%A8%E4%BA%BA&pageNo="+numb;
                String nextLink="http://www.bjeit.gov.cn:8080/oasearch/front/search.do?orderField=publishdate&orderType=desc&Query=%E6%9C%BA%E5%99%A8%E4%BA%BA&pageNo="+numb;
                page.addTargetRequest(nextLink);


                System.out.println("······································NextPageNumber:"+numb+"·········································");

            }

            //文章页
        } else if (pattern.matcher(page.getUrl().toString()).matches()) {
            page.putField("id", "cn.gov.bjeit.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url", page.getUrl().toString());

            String title = page.getHtml().xpath("//[@class='text_main']/h4[1]/text()").toString();

            if(title!=null && !(title.isEmpty())){
                //String title = page.getHtml().xpath("//[@class='text_main']/h4[1]/text()").toString();
                page.putField("title", title);
                System.out.println("title:"+title);
                String sourceInfo = page.getHtml().xpath("//[@class='text_ly']/span[1]/text()").toString();
                if (!sourceInfo.isEmpty()) {
                    String[] sourceArr = sourceInfo.split("发布日期：", 2);
                    String source = sourceArr[0].replace("来源：", "").replaceAll("\\u00A0\\u00A0","").trim();
                    String publishTime = sourceArr[1]+" 00:00:00";
                    page.putField("publish_time", publishTime);
                    if(source==null||source.isEmpty()){
                        source="北京市经济和信息化委员会";
                    }
                    //System.out.println("source:"+ source);
                    page.putField("source", source);
                    page.putField("author", source);
                }
                page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
                //page.putField("author", "北京市经信委");
                //page.putField("digest", "");

                String content = page.getHtml().xpath("//[@class='txt']")
                        //.replace("(\\.\\./)+", "http://www.bjeit.gov.cn/")
                        .all().toString();
                //去掉所有html标签的干净文本
                String cleanContent = CleanTextUtil.getCleanText(content);
                page.putField("cleanContent", cleanContent);
                try {
                    SimHash simHash = new SimHash(cleanContent, 64);
                    page.putField("simhash",simHash.get64strSimHash());
                    System.out.println(simHash.get64strSimHash());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ArrayList<String> segments = HanlpUtil.instance.getSegment(cleanContent);
                StringBuilder sb = new StringBuilder();
                for (String segment : segments) {
                    sb.append(segment).append(" ");
                }
                page.putField("chineseSegment", sb.toString().trim());

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
                content=content.replaceAll("(\\.\\./)+", "http://www.bjeit.gov.cn/");
                page.putField("content", ContentUtil.removeTags(content));
                page.putField("images", page.getHtml().xpath("//div[@class='txt']//img/@src")
                        .replace("(\\.\\./)+", "http://www.bjeit.gov.cn/").all().toString());

                //原网站的分类标签
                //String sourceCate = page.getHtml().xpath("//[@class='top-title']/div/span[3]/text()").toString();
                page.putField("source_tag", "新闻");
                //page.putField("category", "动态");
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


            }else if(title==null||title.isEmpty()){
                title = page.getHtml().xpath("//div[@class='content']/h1/text()").toString();

                if(title==null||title.isEmpty()){
                    title = page.getHtml().xpath("//div[@class='container jigou_details']/h1/text()").toString();
                }

                //String title = page.getHtml().xpath("//[@class='text_main']/h4[1]/text()").toString();
                page.putField("title", title);
                System.out.println("为空的title:"+title);

                //发布时间
                String publish_time=page.getHtml().xpath("//div[@class='content']/div/span[4]/text()").toString();
                if(publish_time==null||publish_time.isEmpty()){
                    publish_time= DateUtil.getSystemCurrentDateTime();
                }else{
                    publish_time=publish_time.replace("发布日期：","").trim()+" 00:00:00";
                }
                page.putField("publish_time",publish_time);
                System.out.println("publish_time:"+publish_time);

                //来源
                String source=page.getHtml().xpath("//div[@class='content']/div/span[3]/text()").toString();
                if(source==null||source.isEmpty()){
                    source="北京市经济和信息化委员会";
                }else if(source.replace("来源：","").trim()==null||source.replace("来源：","").trim().isEmpty()){
                    source="北京市经济和信息化委员会";
                }else{
                    source=source.replace("来源：","").trim();
                }
                page.putField("source",source);
                System.out.println("source:"+source);

                page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
                page.putField("author", source);
               // page.putField("digest", "");

                //获取内容
                List<String> contentlist = page.getHtml().xpath("//div[@class='article']").all();
                String content="";
                if(contentlist.size()==0){
                    content=page.getHtml().xpath("//div[@class='container jigou_details']/div[@id='content']")
                            .all().toString();
                            //.replace("(\\.\\./)+", "http://www.bjeit.gov.cn/");
                }else{
                    content = contentlist.toString();
                            //.replace("(\\.\\./)+", "http://www.bjeit.gov.cn/");
                }
                //填入cleancontent
               String cleanContent =  CleanTextUtil.getCleanText(content);
                page.putField("cleanContent", cleanContent);

                try {
                    SimHash simHash = new SimHash(cleanContent, 64);
                    page.putField("simhash",simHash.get64strSimHash());
                    System.out.println(simHash.get64strSimHash());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ArrayList<String> segments = HanlpUtil.instance.getSegment(cleanContent);
                StringBuilder sb = new StringBuilder();
                for (String segment : segments) {
                    sb.append(segment).append(" ");
                }
                page.putField("chineseSegment", sb.toString().trim());

                //将内容中的imageurl替换为数组形式
                String source_images = page.getHtml().xpath("//div[@class='article']//img/@src").all().toString();
                String[] imageArr = source_images.replace("[", "").replace("]", "").split(",");
                int imalength=imageArr.length;
                if(imageArr[imageArr.length-1]==null||imageArr[imageArr.length-1].equals("")){
                    imalength=0;
                }
                for (int i = 0; i < imalength; i++) {
                    String img_one = imageArr[i].trim();
                    content = content.replace(img_one, "images[" + i + "]");
                }
                content=content.replaceAll("(\\.\\./)+", "http://www.bjeit.gov.cn/");
                page.putField("content", ContentUtil.removeTags(content));

                //原网站的分类标签
                //String sourceCate = page.getHtml().xpath("//div[@class='top-title']/div/span[3]/text()").toString();
                page.putField("source_tag", "新闻");
                //page.putField("category", "动态");
                page.putField("images", page.getHtml().xpath("//div[@class='article']//img/@src")
                        .replace("(\\.\\./)+", "http://www.bjeit.gov.cn/").all().toString());
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

            page.putField("source_content", page.getRawText());
            page.putField("from_site", "北京市经信委www.bjeit.gov.cn");
            page.putField("category", ArticleCategoryUtil.instance.getCategory( title ));

            System.out.println("抓取成功！");
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {

        customTime = args[0]+" "+args[1];
        //customTime = "2016-10-14 01:00";
        String url_one;
        //读取配置文件的示例
        String collection= ParamsConfigurationUtil.instance.getParamString("mongodb.collection.BjeitGov");
        //System.out.println("collection:"+collection);

        url_one="http://www.bjeit.gov.cn:8080/oasearch/front/search.do?orderField=publishdate&orderType=desc&Query=%E6%9C%BA%E5%99%A8%E4%BA%BA&pageNo=1";
            //System.out.println(url_one);
        Spider.create(new BjeitGovPage())
                .addUrl(url_one)
                //.addPipeline(new JsonFilePipeline("F://test/bigData/robot015"))
                .addPipeline(new MongoDBPipeline(collection))
                .run();

    }
}

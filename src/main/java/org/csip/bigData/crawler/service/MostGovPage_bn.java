package org.csip.bigData.crawler.service;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.csip.bigData.crawler.dao.MongoDBPipeline;
import org.csip.bigData.crawler.util.MD5Util;
import org.csip.bigData.crawler.util.ParamsConfigurationUtil;
import org.csip.bigData.crawler.util.dataProcessUtil.*;
import org.csip.bigData.crawler.util.spiderUtil.DateUtil;
import org.csip.bigData.crawler.util.spiderUtil.PostUtil;
import org.csip.bigData.crawler.util.spiderUtil.UserAgentUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Created by csip on 7/29/16.
 * 科学技术部（www.most.gov.cn/）
 */
public class MostGovPage_bn implements PageProcessor {

    private static String customTime = null;
   private static final String URL_POST1 = "http://www\\.most\\.gov\\.cn/\\w+/\\w+/\\w+/\\d+/\\w+.htm";
    private static final String URL_POST2 = "http://www\\.most\\.gov\\.cn/\\w+/\\d+/\\w+.htm";
    private static final String URL_POST3 = "http://www\\.most\\.gov\\.cn/\\w+/\\w+/\\w+/\\w+/\\w+/\\d+/\\w+.htm";
    private static final String URL_POST4 = "http://www\\.most\\.gov\\.cn/mostinfo/xinxifenlei/\\w+/\\d+/\\w+.htm";
    private static final String URL_POST5 = "http://www\\.most\\.gov\\.cn/mostinfo/xinxifenlei/\\w+/\\w+/\\d+/\\w+.htm";
    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent(UserAgentUtil.getUserAgent());

    @Override
    public void process(Page page) {

//        page.putField("id", "cn.gov.most.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
//        page.putField("source_url", page.getUrl().toString());
//        page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
//
//        page.putField("source_content", page.getRawText());
//        page.putField("from_site", "科学技术部www.most.gov.cn");
        if ((page.getUrl().regex(URL_POST1).match() && !page.getUrl().regex(URL_POST4).match()) || page.getUrl().regex(URL_POST2).match()) {

            page.putField("id", "cn.gov.most.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url", page.getUrl().toString());
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());

            page.putField("source_content", page.getRawText());
            page.putField("from_site", "科学技术部www.most.gov.cn");

            String title = page.getHtml().xpath("//div[@id='Title']/text()").toString();
            System.out.println("标题："+title);
            page.putField("title", title);
            String sourceInfo = page.getHtml().xpath("//[@class='gray12 lh22']/text()").toString();
            if (!sourceInfo.isEmpty()) {
                String[] sourceArr = sourceInfo.split("      ", 2);
                String publishTime = sourceArr[0].trim().replace("日期：", "");
                String source = sourceArr[1];
                page.putField("publish_time", DateUtil.parseChDate(publishTime) + " 00:00:00");
                if (source.trim().isEmpty()) {
                    source = "科技部";
                }
                page.putField("source", source);
                page.putField("author", source);
            }
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());
            //page.putField("author", "科技部");
            page.putField("source_tag", "科技动态");

            String content = page.getHtml().xpath("//[@id='Zoom']").toString();
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

            String imageStr = page.getHtml().xpath("//div[@id='Zoom']//img/@src").all().toString();
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
            //原网站的分类标签
//            String sourceCate = page.getHtml().xpath("//div[@class='dqwz']/a[5]").toString();
            page.putField("images", page.getHtml().xpath("//div[@id='Zoom']//img/@src").replace("./", page.getUrl().toString().substring(0, page.getUrl().toString().lastIndexOf("/") + 1)).all().toString());

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

        } else if (page.getUrl().regex(URL_POST3).match()) {

            page.putField("id", "cn.gov.most.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url", page.getUrl().toString());
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());

            page.putField("source_content", page.getRawText());
            page.putField("from_site", "科学技术部www.most.gov.cn");

            String title = page.getHtml().xpath("//div[@id='Title']/text()").toString();
            System.out.println("标题："+title);
            page.putField("title", title);
            String sourceInfo = page.getHtml().xpath("/html/body/table[5]/tbody/tr/td/table[3]/tbody/tr[2]/td/text()").toString();
            if (null == sourceInfo) {
                sourceInfo = page.getHtml().xpath("//div[@class='gray12 lh22']/text()").toString();
                System.out.print("来源区域:" + sourceInfo);
                if (null != sourceInfo && !sourceInfo.isEmpty()) {
                    String[] sourceArr = sourceInfo.split("      ", 2);
                    String publishTime = sourceArr[0].trim().replace("日期：", "");
                    page.putField("publish_time", DateUtil.parseChDate(publishTime) + " 00:00:00");
                    if (sourceArr.length == 2) {
                        String source = sourceArr[1];
                        page.putField("source", source.replace("来源：", ""));
                        page.putField("author", source.replace("来源：", ""));
                    } else {
                        page.putField("source", "科技部");
                        page.putField("author", "科技部");
                    }
                }
                //page.putField("author", "科技部");

                String content = page.getHtml().xpath("//[@id='Zoom']").toString();
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

                String imageStr = page.getHtml().xpath("//[@class='NOBORDER']//img/@src").all().toString();
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
                page.putField("source_tag", "科技动态");

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

            } else {
                if (!sourceInfo.isEmpty()) {
                    String[] sourceArr = sourceInfo.split("www.most.gov.cn", 2);
                    String source = sourceArr[0];
                    String publishTime = sourceArr[1];
                    page.putField("publish_time", trim(publishTime));
                    if (source.trim().isEmpty()) {
                        source = "科技部";
                    }
                    page.putField("source", source);
                    page.putField("author", source);
                }
                //page.putField("author", "科技部");
                String content = page.getHtml().xpath("//[@class='NOBORDER']").toString();
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

                String imageStr = page.getHtml().xpath("//[@class='NOBORDER']//img/@src").all().toString();
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
                page.putField("source_tag", "科技动态");

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
            //原网站的分类标签
            page.putField("images", page.getHtml().xpath("//[@class='NOBORDER']//img/@src").replace("./", page.getUrl().toString().substring(0, page.getUrl().toString().lastIndexOf("/") + 1)).all().toString());
        } else if (page.getUrl().regex(URL_POST4).match() || page.getUrl().regex(URL_POST5).match()) {

            page.putField("id", "cn.gov.most.www/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            page.putField("source_url", page.getUrl().toString());
            page.putField("crawler_time", DateUtil.getSystemCurrentDateTime());

            page.putField("source_content", page.getRawText());
            page.putField("from_site", "科学技术部www.most.gov.cn");

            String title = page.getHtml().xpath("//div[@id='Title']/text()").toString();
            System.out.println("标题："+title);
            page.putField("title", title);
            String source = page.getHtml().xpath("/html/body/table[1]/tbody/tr/td/table[1]/tbody/tr[2]/td/table/tbody/tr[3]/td[2]/text()").toString();
            page.putField("source", source);
            String publishTime = page.getHtml().xpath("/html/body/table[1]/tbody/tr/td/table[1]/tbody/tr[2]/td/table/tbody/tr[3]/td[4]/text()").toString();
            String ptime = DateUtil.parseChDate(publishTime.trim());
            page.putField("publish_time", ptime + " 00:00:00");
            String author = page.getHtml().xpath("/html/body/table[1]/tbody/tr/td/table[1]/tbody/tr[2]/td/table/tbody/tr[3]/td[2]/text()").toString();
            page.putField("author", author);
            String source_tag = page.getHtml().xpath("/html/body/table[1]/tbody/tr/td/table[1]/tbody/tr[2]/td/table/tbody/tr[2]/td[4]/text()").toString();
            page.putField("source_tag", source_tag);

            String content = page.getHtml().xpath("/html/body/table[1]/tbody/tr/td/table[2]/tbody/tr[2]/td/table[1]").toString();
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

            String imageStr = page.getHtml().xpath("//tbody/tr/td/div[@id='Image']").all().toString();
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
            page.putField("images",page.getHtml().xpath("//tbody/tr/td/div[@id='Image']").all().toString());
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

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {

        customTime = args[0];
        //customTime = "2016.09.11";
        //读取配置文件的示例

        String collection= ParamsConfigurationUtil.instance.getParamString("mongodb.collection.MostGov");

        //搜索请求链接
        String reqUrl = "http://znjs.most.gov.cn/wasdemo/search";
        //初始化总页数
        int totalPage = 1;
        List<String> contentList = new ArrayList<String>();
        boolean iter=true;
//        //初步判断年月日
//        String reg = "\\d{4}\\.\\d{2}\\.\\d{2}";
        SimpleDateFormat df=new SimpleDateFormat("yyyy.MM.dd");
//        Pattern pattern = Pattern.compile(reg);
        int pageNow=1;
        while(iter) {
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("searchword", "DOCTITLE='机器人'"));
            qparams.add(new BasicNameValuePair("title", "机器人"));
            qparams.add(new BasicNameValuePair("channelid", "44374"));
            qparams.add(new BasicNameValuePair("sortfield", "-DOCRELTIME"));
            qparams.add(new BasicNameValuePair("page", String.valueOf(pageNow)));
            String postRes = PostUtil.excutePost(reqUrl, qparams);
            //抓取的页面列表

            Document document = Jsoup.parse(postRes);
//            System.out.println("tjo;joagjdod;jg;osdj");
            //获取总页数
            Elements ss = document.getElementsByClass("font_red12px");
            String totalPageStr = ss.get(1).text();
            System.out.println("总页数：" + totalPageStr);

            Elements links = document.select("td[width=530]");

            for (Element link : links) {
                String[] textList=link.text().split(" ");
                String publishTime=textList[textList.length-1];
//                System.out.println();
//                System.out.println(textList[textList.length-1]);
//                Matcher m=pattern.matcher(textList[textList.length-1]);
//                System.out.println(m.find());
//                System.out.println(m.group(1));
//                System.out.println(link.text());
//                System.out.println(link);

                try {

                    Date newDate=df.parse(publishTime);
                    if(newDate.getTime()>df.parse(customTime).getTime())
                    {

                        Elements hreflinks = link.select("a[target$=_blank]");
                        for (Element l : hreflinks) {
                            String linkHref = l.attr("href");
                            contentList.add(linkHref);
                        }
                    }
                    else
                    {
                        iter=false;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }


            }
            pageNow+=1;

        }
        Spider.create(new MostGovPage_bn())
                .startUrls(contentList)
                .addPipeline(new MongoDBPipeline(collection))
                .thread(1)
                .run();

    }
}

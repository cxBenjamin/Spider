package org.csip.bigData.crawler.service;

import org.apache.http.NameValuePair;
import org.csip.bigData.crawler.dao.MongoDBPipeline;
import org.csip.bigData.crawler.util.MD5Util;
import org.csip.bigData.crawler.util.ParamsConfigurationUtil;
import org.csip.bigData.crawler.util.dataProcessUtil.*;
import org.csip.bigData.crawler.util.spiderUtil.DateUtil;
import org.csip.bigData.crawler.util.spiderUtil.PostUtil;
import org.csip.bigData.crawler.util.spiderUtil.UserAgentUtil;
import org.jsoup.Jsoup;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by pyj on 2016/8/3.
 * ofweek机器人网 http://robot.ofweek.com/
 *      --机器人配件  http://robot.ofweek.com/CAT-8321201-JQRPJ.html
 *      --工业机器人  http://robot.ofweek.com/CAT-8321202-GYJQR.html
 *      --服务机器人  http://robot.ofweek.com/CAT-8321203-FWQJQR.html
 *      --特种机器人  http://robot.ofweek.com/CAT-8321204-TZJQR.html
 *      --其它       http://robot.ofweek.com/CAT-8321206-QT.html
 *      --会展       http://robot.ofweek.com/exhibition-898800.html  （待弄）
 *      --行业研究报告 http://research.ofweek.com/list-898800-robot.html   （报告无链接下载，是否还要抓取？）
 *      --专题       http://robot.ofweek.com/topic/robot.html  （待弄）
 *      以上共计2万5千多条
 *      还有部分未分类(目前不到10条，主要在教育和农业里面)
 */

public class OfweekPage implements PageProcessor {

    private static String nextPage="下一页";
    public static final List<String> URLlist=new ArrayList<String>();
    static String updateTime=null;

    //列表页规则(仅适合12346页面)
    public static final String URL_LIST = "http://robot.ofweek.com/CAT-\\d+-\\w+-?\\d*.html";

    //详情页
    public static final String URL_POST = "http://robot.ofweek.com/20\\d{2}-\\d{2}/ART-\\d+-\\d+-\\d+.html";

    enum Contents_Image {
        CONTENTS,IMAGE,SOURCE_CONTENTS,CLEANCONTENTS
    }

    public static int number=0;

    private Site site = Site
            .me()
            .setDomain("robot.ofweek.com")
            .setSleepTime(1000)
            .setUserAgent(UserAgentUtil.getUserAgent());


    //将list里面的字符串拼接起来
    public String Strconcat(List<String> list){

        String str="";
        for(String c : list){
            str = str+c;
        }

        return str;

    }


    /* 取文章多页内容（包括第一页，第二页等）
     * page是页面内容首页
     */
    public EnumMap<Contents_Image,Object> getContent_Image(Page page) {

        EnumMap<Contents_Image,Object> content_image=new EnumMap<Contents_Image, Object>(Contents_Image.class);

        StringBuffer Content=new StringBuffer();
        StringBuffer ImageContent=new StringBuffer();
        StringBuffer SourceContent=new StringBuffer();

        int count=0;

        //页面首页内容和首页图片链接
        List<String> contentList = page.getHtml().xpath("//div[@id='articleC']/p").all();
        String content = Strconcat(contentList);

        //用于存image替换后的content
        String imageInsteadContent = content;

        String image="";
        List<String> imageList = page.getHtml().xpath("//div[@id='articleC']/p/img/@src").all();

        String imageStr =imageList.toString();
        String[] imageArr = imageStr.replace("[", "").replace("]", "").split(",");
        int imalength=imageArr.length;
        if(imageArr[imageArr.length-1]==null||imageArr[imageArr.length-1].equals("")){
            imalength=0;
        }
        for (int i = 0; i < imalength; i++) {
            String img_one = imageArr[i].trim();
            //System.out.println("img:"+img_one);
            //int positon = Arrays.binarySearch(imageArr, img_one);
            imageInsteadContent = imageInsteadContent.replace(img_one, "images[" + count + "]");
            count++;
        }

        String source_content = page.getRawText();

        Content.append(content);
        ImageContent.append(imageInsteadContent);
        SourceContent.append(source_content);

        //获取http://robot.ofweek.com/2016-08/ART-8321203-8230-30018834_3.html
        String xPath="//div[@class='page']/span[@id='nextPage']/a/@href";
        String pagelink= page.getUrl().toString();
        String nextLink=getNextLinkofPage(pagelink,page.getHtml(),xPath);

        /*
        *在此进行内容页的多页抓取操作
        *主要调用PostUtil中方法，通过httpClient进行下载页面
        *循环终止条件以是否能够获取到内容页当前页下一页链接来判断
        */
        while(!(nextLink.equals("NULL"))) {

            System.out.println("获取下一页链接："+nextLink);


            // 获取下一页页面内容
            try{
                Thread.sleep(1000);
                //Thread.sleep((int) (Math.random() * 15)*1000+15000);
                //System.out.println((int) (Math.random() * 15)*1000+15000);
                List<NameValuePair> qparams= new ArrayList<NameValuePair>();
                String subpage = PostUtil.excutePost(nextLink,qparams);
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~下一页~~~~~~~~~~~~：\n"+subpage);
                Html html = new Html(Jsoup.parse(subpage));

                //获取内容页list格式
                List<String> nextContentList=html.xpath("//div[@id='articleC']/p").all();
                String nextContent = Strconcat(nextContentList);

                List<String> nextImage=html.xpath("//div[@id='articleC']/p/img/@src").all();
                imageList.addAll(nextImage);

                String nextImageInsteadContent = nextContent;

                String imageStri =nextImage.toString().trim();
                String[] imageArra = imageStri.replace("[", "").replace("]", "").split(",");
                int imaAlength=imageArra.length;
                if(imageArra[imageArra.length-1]==null||imageArra[imageArra.length-1].equals("")){
                    imaAlength=0;
                }
                for (int i = 0; i < imaAlength; i++) {
                    String img_one = imageArra[i].trim();
                    //System.out.println("img:"+img_one);
                    //int positon = Arrays.binarySearch(imageArr, img_one);
                    nextImageInsteadContent = nextImageInsteadContent.replace(img_one, "images[" + count + "]");
                    count++;
                }
                String nextSourceContent=subpage;
                //将当页内容拼接到这篇文章中
                Content.append(nextContent);
                ImageContent.append(nextImageInsteadContent);
                SourceContent.append(nextSourceContent);

                //获取当前页的下一页链接
                nextLink=getNextLinkofPage(nextLink,html,xPath);
            }catch (InterruptedException e){
                e.printStackTrace();
            }


        }
        image = imageList.toString();
        content_image.put(Contents_Image.CONTENTS,ImageContent.toString());
        content_image.put(Contents_Image.IMAGE,image);
        content_image.put(Contents_Image.SOURCE_CONTENTS,SourceContent.toString());
        content_image.put(Contents_Image.CLEANCONTENTS,Content.toString());
        return content_image;
    }

    /* 获取当前文章详情页页面下方页数链接
    * xPath
    * regex
    */
    public String getNextLinkofPage(String pagelink,Html html,String xPath) {

        // 获取下一页链接
        String nextlink=html.xpath(xPath).toString();

        if(nextlink==null||nextlink.isEmpty()) {
            return "NULL";
        } else {
            if(!nextlink.contains("http")) {
                nextlink=pagelink.replaceAll("ART-\\d+-\\d+-\\d+_?\\d?.html",nextlink);
            }return nextlink;
        }
    }

    //    private boolean isUpdate(Date urlDate,Date inputDate)
//    {
//
//    }
    @Override
    public void process(Page page) {
        //列表页 ： 人工一页一页的抓
        if (page.getUrl().regex(URL_LIST).match()) {
            //获取内容页的链接所在的节点
            List<Selectable> selectableList=page.getHtml().xpath("//div[@class=\"list_model\"]").nodes();
            int nodeNum=selectableList.size();
            //用于计数当页符合规则链接数，如果当页链接全部抓取，则需要抓取下一页链接
            int count=0;
            //遍历每个链接所在的节点，根据日期规则，选择符合规则链接并加入页面访问列表中
            for(Selectable selectable:selectableList) {
                //获取内容页链接
                String url = selectable.links().regex(URL_POST).toString();
                //获取内容页的发布时间。
                String date=selectable.xpath("//span[@class=\"date\"]/text()").toString().replace("|","").trim();
                SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm");
                try
                {
                    Date newDate=df.parse(date);
                    //发布时间与预定义的更新时间进行比较。
//                    String standTime=updateTime+" 00:00";
                    if(newDate.getTime()>df.parse(updateTime).getTime()) {
                        //如果是更新的，就把这个内容页的链接加入到队列中，
                        page.addTargetRequest(url);
                        System.out.println(url);

                        count++;
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }

            //只有在当页链接全部符合更新规则时，才抓取下一页的链接
            if(count==nodeNum){
                List<Selectable> listUrlList=page.getHtml().xpath("//a[@target=\"_self\"]").nodes();
                //遍历列表page的节点
                for(Selectable select:listUrlList) {
                    //获取列表页链接
                    String listUrl=select.xpath("//a/@href").toString();
                    //获取名字（1,2,3，。。。，下一页）
                    String listText=select.xpath("//a/text()").toString();
                    //如果是“下一页”，就把这个链接加入到队列中。
                    if(listText.equals(nextPage)) {
                        page.addTargetRequest(listUrl);
                        System.out.println("listUrl:"+listUrl);
                        System.out.println("listText:"+listText);
                    }

                }
            }


            //文章页
        } else if (page.getUrl().regex(URL_POST).match()){
            number++;
            System.out.println("····························抓取计数:"+number);
            //id
            page.putField("id", "com.ofweek.robot/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            //System.out.println("~~id:"+"com.ofweektest.robot/" + MD5Util.GetMD5Code(page.getUrl().toString()));
            //原文来源url
            String source_url=page.getUrl().toString();
            page.putField("source_url",source_url);
            //System.out.println("~~source_link:"+source_url);
            //标题
            String title= page.getHtml().xpath("//div[@class='article_left']/h1/text()").toString();
            page.putField("title",title );
            System.out.println("title:"+title);
            //发布时间
            String publish_time=page.getHtml().xpath("//div[@class='tag_left']/span[1]/text()").toString();
            page.putField("publish_time",publish_time);
            System.out.println("publish_time:"+publish_time);
            //抓取时间
            String crawler_time= DateUtil.getSystemCurrentDateTime();
            page.putField("crawler_time",crawler_time);
            //System.out.println("~~crawler_time:"+crawler_time);
            //来源
            String source=page.getHtml().xpath("//div[@class='tag_left']/span[2]/text()").toString().replace("来源：","").trim();
            if(source==null||source.isEmpty()){
                source=page.getHtml().xpath("//div[@class='tag_left']/span[2]/a/text()").toString().replace("来源：","").trim();
            }
            page.putField("source",source);
//            System.out.println("source:"+source);
            //作者
            page.putField("author",source);
            page.putField("source_tag", "行业新闻");
            //System.out.println("author:"+source);
            //摘要
            String digest=page.getHtml().xpath("//div[@class='simple']/p/text()").toString();
            if(digest.length()<=200){
                page.putField("digest",CleanTextUtil.getCleanText(digest));
            }else {
                String dig = CleanTextUtil.getCleanText(digest);
                page.putField("digest",dig.substring(0,200));
            }

            //System.out.println("~~digest:"+digest);

            EnumMap<Contents_Image,Object> contents_image=getContent_Image(page);
            String Content=contents_image.get(Contents_Image.CLEANCONTENTS).toString();

            String cleanContent = CleanTextUtil.getCleanText(Content);
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

            String content=contents_image.get(Contents_Image.CONTENTS).toString();
            page.putField("content", ContentUtil.removeTags(content));
            //System.out.println("~~content:"+content);
            //source_tag网站分类标签
            page.putField("tag", "行业新闻");//?
            //System.out.println("~~source_tag:"+"行业新闻");
            //分类
            page.putField("category", ArticleCategoryUtil.instance.getCategory( title ));
            //System.out.println("~~category:"+"动态");
            //图片链接
            String image_link=contents_image.get(Contents_Image.IMAGE).toString();
            page.putField("images",image_link);
            //System.out.println("~~image:"+image_link);
            //原文内容
            String source_content=contents_image.get(Contents_Image.SOURCE_CONTENTS).toString();
            page.putField("source_content",source_content);
            //爬取网站
            String from_site=page.getHtml().xpath("//div[@class='position gray']/h2[1]/a/text()").toString();
            page.putField("from_site",from_site+"robot.ofweek.com");
            //System.out.println("~~from_site:"+(from_site+"robot.ofweek.com"));

            System.out.println("抓取成功/n");
            //关键词
            ArrayList<String> sourceTagList=new ArrayList<String>();
//            sourceTagList.add("资讯");
//            sourceTagList.add("政策");
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


    public static void main(String[] args){
        //System.out.println(args[0]);
        updateTime=args[0]+" "+args[1];
        //updateTime = "2016-10-21 00:00";
        //读取配置文件的示例
        String collection = ParamsConfigurationUtil.instance.getParamString("mongodb.collection.OFweek");
        URLlist.add("http://robot.ofweek.com/CAT-8321201-JQRPJ.html");
        URLlist.add("http://robot.ofweek.com/CAT-8321202-GYJQR.html");
        URLlist.add("http://robot.ofweek.com/CAT-8321203-FWQJQR.html");
        URLlist.add("http://robot.ofweek.com/CAT-8321204-TZJQR.html");
        URLlist.add("http://robot.ofweek.com/CAT-8321206-QT.html");
        Spider.create(new OfweekPage()).startUrls(URLlist)
                .addPipeline(new MongoDBPipeline(collection))
                //.addPipeline(new JsonFilePipeline("E://test/bigData/robot082"))
                .run();
    }


}

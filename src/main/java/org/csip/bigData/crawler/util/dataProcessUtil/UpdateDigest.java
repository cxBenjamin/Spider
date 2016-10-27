package org.csip.bigData.crawler.util.dataProcessUtil;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Arrays;

/**
 * Created by a on 2016/10/18.
 */
public class UpdateDigest {
    private static CleanTextUtil cleanTextUtil=new CleanTextUtil();
    public static void main(String[] args) {
        ConfigurationUtil con=new ConfigurationUtil();
        String ip = "54.223.50.219";
        int port = con.getParamInteger("mongodb.port");
        String userName=con.getParamString("mongodb.userName");
        char[] passwd=con.getParamString("mongodb.passwd").toCharArray();
        String database=con.getParamString("mongodb.database");
        MongoCredential credential = MongoCredential.createCredential(userName, database, passwd);
        MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port), Arrays.asList(credential));
        MongoDatabase mongoDatabase=mongoClient.getDatabase(database);
        long start = System.currentTimeMillis();
        System.out.println(start);
        //""miit"开始的数据规整
        //"BjeitGov","CriaMei","KukaRobot","MiitGov","MostGov","OFweek","RobotChina","RobotCn","RobotCnOrg"
        String[] colls={"BjeitGov","CriaMei","KukaRobot","MiitGov","MostGov","OFweek","RobotChina","RobotCn","RobotCnOrg","SzRobot"};
//        for(int i=0;i<colls.length;i++) {
//            System.out.println("i================="+colls[i]);
            MongoCollection<Document> coll = mongoDatabase.getCollection("SzRobot");
            MongoCursor<Document> cursor = coll.find().iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();

//                String content = doc.getString("content");
//                content = cleanTextUtil.getCleanText(content);
//                doc.put("cleanContent", content);
                String digest1 = doc.getString("digest");
                String digest = null;
                if(digest1.length()>=200){
                    digest  = digest1.substring(0,200);
                }else{
                    digest = digest1;
                }

                doc.put("digest",digest)    ;

                coll.replaceOne(
                        Filters.eq("_id", doc.get("_id")),
                        doc);


            }
//       }
        long end = System.currentTimeMillis();
        System.out.println(end);
        System.out.println("total time is:"+String.valueOf((end-start)/1000));
        mongoClient.close();
    }


}

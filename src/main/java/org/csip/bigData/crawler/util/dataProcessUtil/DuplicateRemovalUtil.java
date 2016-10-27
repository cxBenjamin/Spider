package org.csip.bigData.crawler.util.dataProcessUtil;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.math.BigInteger;
import java.util.*;

/**
 * Created by a on 2016/10/18.
 */
public class DuplicateRemovalUtil {
    private static CleanTextUtil cleanTextUtil = new CleanTextUtil();


    public static int hammingDistance(BigInteger code1, BigInteger code2) {
        BigInteger x = code1.xor(code2);
        int tot = 0;

        while (x.signum() != 0) {
            tot += 1;
            x = x.and(x.subtract(new BigInteger("1")));
        }
        return tot;
    }

    public static void main(String[] args) {
        ConfigurationUtil con = new ConfigurationUtil();
        String ip = "54.223.50.219";
        int port = con.getParamInteger("mongodb.port");
        String userName = con.getParamString("mongodb.userName");
        char[] passwd = con.getParamString("mongodb.passwd").toCharArray();
        String database = con.getParamString("mongodb.database");
        MongoCredential credential = MongoCredential.createCredential(userName, database, passwd);
        MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port), Arrays.asList(credential));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
        long start = System.currentTimeMillis();
        System.out.println(start);

        String[] colls = {"BjeitGov", "CriaMei", "KukaRobot", "MiitGov", "MostGov", "OFweek", "RobotChina", "RobotCn", "RobotCnOrg", "SzRobot"};
        int count = 0;
        for (int i = 0; i < colls.length - 1; i++) {
//            HashMap<String,BigInteger> bigIntegers = new HashMap<String,BigInteger>();

            MongoCollection<Document> coll = mongoDatabase.getCollection(colls[i]);
            System.out.println(i + "=================" + colls[i]);
            MongoCursor<Document> cursor = coll.find().iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String simhashStr = doc.getString("simhash");
                BigInteger simhashCode = new BigInteger(simhashStr);
                for (int j = i + 1; j < colls.length; j++) {
                    MongoCollection<Document> nextColl = mongoDatabase.getCollection(colls[j]);
                    System.out.println(j + "======nextcoll=========" + colls[j]);
                    MongoCursor<Document> nextCursor = nextColl.find().iterator();

                    while (nextCursor.hasNext()) {
                        Document nextDoc = nextCursor.next();
                        String nextSimhashStr = nextDoc.getString("simhash");
                        BigInteger nextSimhashCode = new BigInteger(nextSimhashStr);
                        int distance = DuplicateRemovalUtil.hammingDistance(simhashCode, nextSimhashCode);
                        if (distance < 3) {
                            doc.append("flag", "similarity");
                            count++;
                            coll.replaceOne(
                                    Filters.eq("_id", doc.get("_id")),
                                    doc);
                            System.out.println("相似。。。。。。。。。。。" + nextColl.getNamespace());
                        }
                    }
                }
            }

        }

        System.out.println("相似的文章有：" + count + "个");

        long end = System.currentTimeMillis();
        System.out.println(end);
        System.out.println("total time is:" + String.valueOf((end - start) / 1000));
        mongoClient.close();
    }


}

package org.csip.bigData.crawler.util.dataProcessUtil;

import org.junit.Test;

/**
 * Created by bun@csip.org.cn on 2016/9/22.
 */
public class ArticleCategoryUtilTest {
    @Test
    public void getCategory() throws Exception {

        String text="法规【重磅】中国位居2016全球制造业竞争力榜首 强力五国正在崛起";
        System.out.println(ArticleCategoryUtil.instance.getCategory(text));
    }

}
package org.csip.bigData.crawler.util.dataProcessUtil;

/**
 * Created by a on 2016/10/18.
 */
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
/**
 * Created by Administrator on 2016/8/30.
 */
public class ConfigurationUtil {
    CompositeConfiguration config=null;
    {
        config= new CompositeConfiguration();
        try {
            config.addConfiguration(new PropertiesConfiguration("params.properties"));
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
    public String getParamString(String param){
//        String host=
        return config.getString(param);
    }
    public int getParamInteger(String param){
        return config.getInt(param);
    }
}



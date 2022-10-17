package org.apache.shardingsphere.presto;

import com.facebook.airlift.configuration.Config;

/**
 * 对接ShardingSphere的配置类
 *
 * @author Eric
 * @since <pre>2022/9/20</pre>
 */
public class ShardingSphereConfig {

    private String ruleLocation;

    private String dbType = "mysql";

    public String getRuleLocation() {
        return ruleLocation;
    }

    @Config("shardingsphere-rule-location")
    public void setRuleLocation(String ruleLocation) {
        this.ruleLocation = ruleLocation;
    }

    public String getDbType() {
        return dbType;
    }

    @Config("shardingsphere-db-type")
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

}

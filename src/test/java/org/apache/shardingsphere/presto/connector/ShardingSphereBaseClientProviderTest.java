package org.apache.shardingsphere.presto.connector;


import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;

/**
 * @author Eric
 * @since <pre>2022/9/21</pre>
 */
public class ShardingSphereBaseClientProviderTest {


    public static void main(String[] args) throws IOException, SQLException {
        final String file = ShardingSphereBaseClientProviderTest.class.getResource("/config-cc.yaml").getFile();
        final File f = new File(file);

        final DataSource ds = YamlShardingSphereDataSourceFactory.createDataSource(f);

        System.out.println(ds);
    }
}
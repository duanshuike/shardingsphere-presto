package org.apache.shardingsphere.presto.init;

import javax.sql.DataSource;
import java.io.File;

import com.facebook.presto.plugin.jdbc.BaseJdbcConfig;
import com.facebook.presto.plugin.jdbc.JdbcConnectorId;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.presto.ShardingSphereConfig;
import org.apache.shardingsphere.presto.jdbc.ShardingSphereBaseClient;
import org.apache.shardingsphere.presto.jdbc.ShardingSphereMySqlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 根据DB类型生成相应的ShardingSphereBaseClient
 *
 * @author Eric
 * @since <pre>2022/9/20</pre>
 */
public class ShardingSphereBaseClientProvider implements Provider<ShardingSphereBaseClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardingSphereBaseClientProvider.class);

    private ShardingSphereConfig shardingSphereConfig;

    private JdbcConnectorId connectorId;

    @Inject
    public ShardingSphereBaseClientProvider(ShardingSphereConfig shardingSphereConfig, JdbcConnectorId connectorId) {
        this.shardingSphereConfig = shardingSphereConfig;
        this.connectorId = connectorId;
    }

    public ShardingSphereDataSource createShardingSphereDataSource() {
        File yamlFile = new File(shardingSphereConfig.getRuleLocation());
        DataSource ds = null;
        try {
            ds = YamlShardingSphereDataSourceFactory.createDataSource(yamlFile);

            return (ShardingSphereDataSource) ds;
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public ShardingSphereBaseClient get() {

        new ShardingSphereServiceRegister().register();

        final ShardingSphereDataSource dataSource = createShardingSphereDataSource();
        final String dbType = shardingSphereConfig.getDbType();

        if ("mysql".equals(dbType)) {
            return new ShardingSphereMySqlClient(dataSource, connectorId, new BaseJdbcConfig());
        }
        else {
            throw new UnsupportedOperationException("Unsupported db type: " + dbType);
        }
    }
}

package org.apache.shardingsphere.presto.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.facebook.airlift.log.Logger;
import com.facebook.presto.plugin.jdbc.BaseJdbcClient;
import com.facebook.presto.plugin.jdbc.BaseJdbcConfig;
import com.facebook.presto.plugin.jdbc.JdbcColumnHandle;
import com.facebook.presto.plugin.jdbc.JdbcConnectorId;
import com.facebook.presto.plugin.jdbc.JdbcIdentity;
import com.facebook.presto.plugin.jdbc.JdbcSplit;
import com.facebook.presto.plugin.jdbc.JdbcTableHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.SchemaTableName;
import org.apache.shardingsphere.data.pipeline.core.util.ReflectionUtil;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.presto.sql.ShardingSphereSqlBuilder;

/**
 * 基于ShardingSphere的JdbcClient
 *
 * @author Eric
 * @since <pre>2022/9/20</pre>
 */
public class ShardingSphereBaseClient extends BaseJdbcClient {

    private static final Logger log = Logger.get(ShardingSphereBaseClient.class);

    private ContextManager contextManager;

    private String databaseName;

    public ShardingSphereBaseClient(ShardingSphereDataSource dataSource, JdbcConnectorId connectorId, BaseJdbcConfig config,
                                    String identifierQuote) {
        super(connectorId, config, identifierQuote, new ShardingSphereConnectionFactory(dataSource));

        try {
            contextManager = ReflectionUtil.getFieldValue(dataSource, "contextManager", ContextManager.class);
            databaseName = ReflectionUtil.getFieldValue(dataSource, "databaseName", String.class);
        }
        catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected Collection<String> listSchemas(Connection connection)
    {
//        final Set<String> result = contextManager.getMetaDataContexts().getMetaData().getDatabases().keySet();
        return Collections.singleton(databaseName);
    }

    @Override
    public JdbcTableHandle getTableHandle(JdbcIdentity identity, SchemaTableName schemaTableName)
    {
        return new JdbcTableHandle(
                connectorId,
                schemaTableName,
                null,
                null,
                schemaTableName.getTableName());
    }


    @Override
    public PreparedStatement buildSql(ConnectorSession session, Connection connection, JdbcSplit split, List<JdbcColumnHandle> columnHandles)
            throws SQLException
    {
        final PreparedStatement ps = new ShardingSphereSqlBuilder(identifierQuote)
                .buildSql(
                        this,
                        session,
                        connection,
                        split.getCatalogName(),
                        split.getSchemaName(),
                        split.getTableName(),
                        columnHandles,
                        split.getTupleDomain(),
                        split.getAdditionalPredicate());
        return ps;
    }


    public String getDatabaseName() {
        return databaseName;
    }
}

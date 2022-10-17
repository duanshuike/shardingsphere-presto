package org.apache.shardingsphere.presto.jdbc;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.facebook.presto.plugin.jdbc.ConnectionFactory;
import com.facebook.presto.plugin.jdbc.JdbcIdentity;
import com.google.common.base.Preconditions;

/**
 * 基于ShardingSphere的ConnectionFactory
 *
 * @author Eric
 * @since <pre>2022/9/20</pre>
 */
public class ShardingSphereConnectionFactory implements ConnectionFactory {

    private DataSource dataSource;

    public ShardingSphereConnectionFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection openConnection(JdbcIdentity identity) throws SQLException {
        final Connection connection = dataSource.getConnection();
        Preconditions.checkState(connection != null, "ShardingSphereDataSource returned null connection");
        return connection;
    }

    @Override
    public void close() throws SQLException {
        if (dataSource instanceof Closeable) {
            try {
                ((Closeable) dataSource).close();
            }
            catch (IOException e) {
                //ignore
            }
        }
    }
}

package org.apache.shardingsphere.presto.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.facebook.presto.common.type.RealType.REAL;
import static com.facebook.presto.common.type.TimeWithTimeZoneType.TIME_WITH_TIME_ZONE;
import static com.facebook.presto.common.type.TimestampType.TIMESTAMP;
import static com.facebook.presto.common.type.TimestampWithTimeZoneType.TIMESTAMP_WITH_TIME_ZONE;
import static com.facebook.presto.common.type.VarbinaryType.VARBINARY;
import static com.facebook.presto.common.type.Varchars.isVarcharType;
import static com.facebook.presto.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static com.facebook.presto.spi.StandardErrorCode.ALREADY_EXISTS;
import static com.facebook.presto.spi.StandardErrorCode.NOT_SUPPORTED;
import static com.mysql.jdbc.SQLError.SQL_STATE_ER_TABLE_EXISTS_ERROR;
import static com.mysql.jdbc.SQLError.SQL_STATE_SYNTAX_ERROR;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.plugin.jdbc.BaseJdbcConfig;
import com.facebook.presto.plugin.jdbc.JdbcColumnHandle;
import com.facebook.presto.plugin.jdbc.JdbcConnectorId;
import com.facebook.presto.plugin.jdbc.JdbcIdentity;
import com.facebook.presto.plugin.jdbc.JdbcTableHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorTableMetadata;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.collect.ImmutableList;
import com.mysql.jdbc.Statement;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;

/**
 * ShardingSphere对接MySQL时的实现
 *
 * @author Eric
 * @since <pre>2022/9/20</pre>
 */
public class ShardingSphereMySqlClient extends ShardingSphereBaseClient {


    public ShardingSphereMySqlClient(ShardingSphereDataSource dataSource, JdbcConnectorId connectorId, BaseJdbcConfig config) {
        super(dataSource, connectorId, config, "`");
    }

    @Override
    public List<SchemaTableName> getTableNames(JdbcIdentity identity, Optional<String> schema)
    {
        String targetSchema = schema.orElseGet(this::getDatabaseName).toLowerCase(Locale.ENGLISH);
        try (Connection connection = connectionFactory.openConnection(identity)) {
            try (java.sql.Statement ps = connection.createStatement(); ResultSet rs = ps.executeQuery("show tables")) {
                ImmutableList.Builder<SchemaTableName> list = ImmutableList.builder();
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    list.add(new SchemaTableName(targetSchema, tableName.toLowerCase(ENGLISH)));
                }
                return list.build();
            }
        }
        catch (SQLException e) {
            throw new PrestoException(JDBC_ERROR, e);
        }
    }


    @Override
    public PreparedStatement getPreparedStatement(Connection connection, String sql)
            throws SQLException
    {
        PreparedStatement statement = connection.prepareStatement(sql);
        if (statement.isWrapperFor(Statement.class)) {
            statement.unwrap(Statement.class).enableStreamingResults();
        }
        //新增
        else {
            statement.setFetchSize(Integer.MIN_VALUE);
        }
        return statement;
    }

    @Override
    protected ResultSet getTables(Connection connection, Optional<String> schemaName, Optional<String> tableName)
            throws SQLException
    {
        // MySQL maps their "database" to SQL catalogs and does not have schemas
        DatabaseMetaData metadata = connection.getMetaData();
        Optional<String> escape = Optional.ofNullable(metadata.getSearchStringEscape());
        return metadata.getTables(
                schemaName.orElse(null),
                null,
                escapeNamePattern(tableName, escape).orElse(null),
                new String[] {"TABLE", "VIEW"});
    }

    @Override
    protected String getTableSchemaName(ResultSet resultSet)
            throws SQLException
    {
        // MySQL uses catalogs instead of schemas
        return resultSet.getString("TABLE_CAT");
    }

    @Override
    protected String toSqlType(Type type)
    {
        if (REAL.equals(type)) {
            return "float";
        }
        if (TIME_WITH_TIME_ZONE.equals(type) || TIMESTAMP_WITH_TIME_ZONE.equals(type)) {
            throw new PrestoException(NOT_SUPPORTED, "Unsupported column type: " + type.getDisplayName());
        }
        if (TIMESTAMP.equals(type)) {
            return "datetime";
        }
        if (VARBINARY.equals(type)) {
            return "mediumblob";
        }
        if (isVarcharType(type)) {
            VarcharType varcharType = (VarcharType) type;
            if (varcharType.isUnbounded()) {
                return "longtext";
            }
            if (varcharType.getLengthSafe() <= 255) {
                return "tinytext";
            }
            if (varcharType.getLengthSafe() <= 65535) {
                return "text";
            }
            if (varcharType.getLengthSafe() <= 16777215) {
                return "mediumtext";
            }
            return "longtext";
        }

        return super.toSqlType(type);
    }

    @Override
    public void createTable(ConnectorSession session, ConnectorTableMetadata tableMetadata)
    {
        try {
            createTable(tableMetadata, session, tableMetadata.getTable().getTableName());
        }
        catch (SQLException e) {
            if (SQL_STATE_ER_TABLE_EXISTS_ERROR.equals(e.getSQLState())) {
                throw new PrestoException(ALREADY_EXISTS, e);
            }
            throw new PrestoException(JDBC_ERROR, e);
        }
    }

    @Override
    public void renameColumn(JdbcIdentity identity, JdbcTableHandle handle, JdbcColumnHandle jdbcColumn, String newColumnName)
    {
        try (Connection connection = connectionFactory.openConnection(identity)) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (metadata.storesUpperCaseIdentifiers()) {
                newColumnName = newColumnName.toUpperCase(ENGLISH);
            }
            String sql = format(
                    "ALTER TABLE %s RENAME COLUMN %s TO %s",
                    quoted(handle.getCatalogName(), handle.getSchemaName(), handle.getTableName()),
                    quoted(jdbcColumn.getColumnName()),
                    quoted(newColumnName));
            execute(connection, sql);
        }
        catch (SQLException e) {
            // MySQL versions earlier than 8 do not support the above RENAME COLUMN syntax
            if (SQL_STATE_SYNTAX_ERROR.equals(e.getSQLState())) {
                throw new PrestoException(NOT_SUPPORTED, format("Rename column not supported in catalog: '%s'", handle.getCatalogName()), e);
            }
            throw new PrestoException(JDBC_ERROR, e);
        }
    }

    @Override
    protected void renameTable(JdbcIdentity identity, String catalogName, SchemaTableName oldTable, SchemaTableName newTable)
    {
        // MySQL doesn't support specifying the catalog name in a rename; by setting the
        // catalogName parameter to null it will be omitted in the alter table statement.
        super.renameTable(identity, null, oldTable, newTable);
    }
}

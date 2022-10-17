package org.apache.shardingsphere.presto;

import com.facebook.presto.plugin.jdbc.JdbcPlugin;

/**
 * Presto对接ShardingSphere的插件
 *
 * @author Eric
 * @since <pre>2022/7/22</pre>
 */
public class ShardingSpherePlugin extends JdbcPlugin {

    public ShardingSpherePlugin() {
        super("shardingsphere", new ShardingSphereModule());
    }
}

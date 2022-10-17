package org.apache.shardingsphere.presto;

import static com.facebook.airlift.configuration.ConfigBinder.configBinder;

import com.facebook.airlift.configuration.AbstractConfigurationAwareModule;
import com.facebook.presto.plugin.jdbc.JdbcClient;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import org.apache.shardingsphere.presto.init.ShardingSphereBaseClientProvider;

/**
 * ShardingSphere插件模块
 *
 * @author Eric
 * @since <pre>2022/9/20</pre>
 */
public class ShardingSphereModule extends AbstractConfigurationAwareModule {

    @Override
    protected void setup(Binder binder) {
        binder.bind(JdbcClient.class).toProvider(ShardingSphereBaseClientProvider.class)
                .in(Scopes.SINGLETON);
        configBinder(binder).bindConfig(ShardingSphereConfig.class);
    }
}

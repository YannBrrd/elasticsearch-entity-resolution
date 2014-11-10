package org.yaba.entity.script;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;


/**
 */
@ClusterScope(scope = Scope.TEST, numDataNodes = 0)
public abstract class AbstractSearchScriptTests extends ElasticsearchIntegrationTest {

    @Override
    protected final Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.settingsBuilder()
                .put("gateway.type", "none")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put(super.nodeSettings(nodeOrdinal))
                .put("plugins." + PluginsService.LOAD_PLUGIN_FROM_CLASSPATH, true)
                .build();
    }
}

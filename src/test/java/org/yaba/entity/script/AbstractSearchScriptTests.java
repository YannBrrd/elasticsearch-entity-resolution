package org.yaba.entity.script;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;


/**
 */
@ClusterScope(scope = Scope.SUITE, numDataNodes = 0)
public class AbstractSearchScriptTests extends ElasticsearchIntegrationTest {

    @Override
    protected final Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.settingsBuilder()
                .put("gateway.type", "none")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put(super.nodeSettings(nodeOrdinal))
                .build();
    }
}

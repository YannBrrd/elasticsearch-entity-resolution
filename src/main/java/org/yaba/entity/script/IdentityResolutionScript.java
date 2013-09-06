package org.yaba.entity.script;

import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.util.concurrent.UncheckedExecutionException;
import org.elasticsearch.common.xcontent.support.XContentMapValues;

import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("unchecked")
public class IdentityResolutionScript extends AbstractSearchScript {

    private final static ESLogger logger = Loggers
            .getLogger(IdentityResolutionScript.class);

    /**
     * Native scripts are build using factories that are registered in the
     * {@link org.elasticsearch.examples.nativescript.plugin.NativeScriptExamplesPlugin#onModule(org.elasticsearch.script.ScriptModule)}
     * method when plugin is loaded.
     */
    public static class Factory implements NativeScriptFactory {

        /**
         * This method is called for every search on every shard.
         * 
         * @param params
         *            list of script parameters passed with the query
         * @return new native script
         */
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            // Mandatory array parameter

            if (params.get("params") == null) {
                throw new ElasticSearchIllegalArgumentException(
                        "Missing the entity parameter");
            }

            boolean isArray = XContentMapValues.isArray(params.get("entity"));

            ArrayList<Map<String, Object>> parameters = null;

            if (isArray) {
                parameters = (ArrayList<Map<String, Object>>) params
                        .get("entity");
                logger.info("Duke parameters: " + parameters);
            }

            return new IdentityResolutionScript(parameters);

        }

    }

    private IdentityResolutionScript(ArrayList<Map<String, Object>> params) {
        logger.info("------> Duke params " + params);

        Iterator<Map<String, Object>> it = params.iterator();

        while (it.hasNext()) {
            Map<String, Object> map = it.next();
            Set<String> mapKeys = map.keySet();
            Iterator<String> mapIt = mapKeys.iterator();
            while (mapIt.hasNext()) {
                String key = mapIt.next();
                logger.info(key + " : " + map.get(key) + " : " + map.get(key).getClass());
            }
        }

    }

    @Override
    public Object run() {
        // TODO Auto-generated method stub
        return  null;
    }
    
    @Override
    public float runAsFloat() {
        // TODO Auto-generated method stub
        return  1L;
    }
    

}

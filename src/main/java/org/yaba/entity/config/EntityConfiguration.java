package org.yaba.entity.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

public class EntityConfiguration {

    private final static ESLogger logger = Loggers
            .getLogger(EntityConfiguration.class);

    public EntityConfiguration(ArrayList<Map<String, Object>> params) {
        Iterator<Map<String, Object>> it = params.iterator();

        while (it.hasNext()) {
            Map<String, Object> map = it.next();
            Set<String> mapKeys = map.keySet();
            Iterator<String> mapIt = mapKeys.iterator();
            while (mapIt.hasNext()) {
                String key = mapIt.next();
                logger.info(key + " : " + map.get(key) + " : "
                        + map.get(key).getClass());
            }
        }
    }
}

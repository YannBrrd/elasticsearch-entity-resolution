package org.yaba.entity.config;

import java.util.*;

public class EntityConfiguration {

    private Map<String, Object> parameters = null;

    public EntityConfiguration(List<Map<String, Object>> params) {
        Iterator<Map<String, Object>> it = params.iterator();

        parameters = new HashMap<String, Object>();

        while (it.hasNext()) {
            Map<String, Object> map = it.next();
            if (!(map.get("field") == null)) {
                parameters.put((String) map.get("field"), map);
            }
            /*
             * Set<String> mapKeys = map.keySet(); Iterator<String> mapIt =
             * mapKeys.iterator(); while (mapIt.hasNext()) { String key =
             * mapIt.next(); logger.info(key + " : " + map.get(key) + " : " +
             * map.get(key).getClass());
             */
        }
    }

    @SuppressWarnings("unchecked")
    public final Map<String, Object> getConfiguratio(String key) {
        return (Map<String, Object>) parameters.get(key);
    }
}

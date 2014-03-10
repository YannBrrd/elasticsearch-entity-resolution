package org.yaba.entity.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EntityConfiguration {

    private Map<String, Object> parameters = null;

    public EntityConfiguration(List<Map<String, Object>> params) {
        Iterator<Map<String, Object>> it = params.iterator();

        parameters = new HashMap<>();

        while (it.hasNext()) {
            Map<String, Object> map = it.next();
            if (!(map.get("field") == null)) {
                parameters.put((String) map.get("field"), map);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final Map<String, Object> getConfiguratio(String key) {
        return (Map<String, Object>) parameters.get(key);
    }
}

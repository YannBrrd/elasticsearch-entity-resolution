/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

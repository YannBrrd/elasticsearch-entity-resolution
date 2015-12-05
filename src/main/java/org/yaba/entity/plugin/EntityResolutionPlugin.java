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

package org.yaba.entity.plugin;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;
import org.yaba.entity.script.EntityResolutionScript;

public class EntityResolutionPlugin extends Plugin {
    @Override
    public final String name() {
        return "entity-resolution-plugin";
    }

    @Override
    public final String description() {
        return "Bayesian based entity resolution plugin";
    }

    public void onModule(ScriptModule module) {
        // Register each script that we defined in this plugin
        module.registerScript("entity-resolution", EntityResolutionScript.Factory.class);
    }
}
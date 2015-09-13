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
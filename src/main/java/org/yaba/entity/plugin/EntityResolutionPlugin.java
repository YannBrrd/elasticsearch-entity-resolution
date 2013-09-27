package org.yaba.entity.plugin;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;
import org.yaba.entity.script.EntityResolutionScript;

public class EntityResolutionPlugin extends AbstractPlugin {
    @Override
    public final String name() {
        return "entity-resolution-plugin";
    }

    @Override
    public final String description() {
        return "Bayesian based entity resolution plugin";
    }
    
    public final void onModule(ScriptModule module) {
        // Register each script that we defined in this plugin
        module.registerScript("entity-resolution", EntityResolutionScript.Factory.class);
    }
}
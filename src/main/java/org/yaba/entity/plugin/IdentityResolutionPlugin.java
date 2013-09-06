package org.yaba.entity.plugin;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;
import org.yaba.entity.script.IdentityResolutionScript;

public class IdentityResolutionPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "identity-resolution-plugin";
    }

    @Override
    public String description() {
        return "Bayesian based identity resolution plugin";
    }
    
    public void onModule(ScriptModule module) {
        // Register each script that we defined in this plugin
        module.registerScript("identity-resolution", IdentityResolutionScript.Factory.class);
    }
}
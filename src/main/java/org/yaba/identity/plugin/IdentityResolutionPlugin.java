package org.yaba.identity.plugin;

import org.elasticsearch.plugins.AbstractPlugin;

public class IdentityResolutionPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "identity-resolution-plugin";
    }

    @Override
    public String description() {
        return "Bayesian based identity resolution plugin";
    }
}
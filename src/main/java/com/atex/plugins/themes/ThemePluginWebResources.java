package com.atex.plugins.themes;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.api.client.repackaged.com.google.common.base.Optional;
import com.polopoly.plugin.PluginWebResource;
import com.polopoly.plugin.PluginWebResources;

/**
 * A wrapper for {@link PluginWebResources} which allow us to keep the existing
 * resource list as it is and create a "virtual copy".
 *
 * @author mnova
 */
public class ThemePluginWebResources implements PluginWebResources {

    private ConcurrentMap<String, Boolean> removeMap = new ConcurrentHashMap<String, Boolean>();

    final PluginWebResources webResources;

    public ThemePluginWebResources(final PluginWebResources webResources) {
        this.webResources = webResources;
    }

    @Override
    public long getIdentifier() {
        return webResources.getIdentifier();
    }

    @Override
    public Collection<PluginWebResource> getAll() {
        return webResources
                .getAll()
                .stream()
                .filter( r -> !isRemoved(r) )
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginWebResource> filter(final String... strings) {
        return webResources
                .filter(strings)
                .stream()
                .filter( r -> !isRemoved(r) )
                .collect(Collectors.toList());
    }

    public void markResourceAsRemoved(final PluginWebResource resource) {
        removeMap.put(getUniqueKey(resource), Boolean.TRUE);
    }

    private String getUniqueKey(final PluginWebResource resource) {
        return resource.getContentId().getContentIdString() + "-" + resource.getIdentifier();
    }

    private boolean isRemoved(final PluginWebResource resource) {
        return Optional
                .fromNullable(removeMap.get(getUniqueKey(resource)))
                .or(false);
    }

}

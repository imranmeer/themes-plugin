package com.atex.plugins.themes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.ContentReference;
import com.polopoly.cm.VersionedContentId;
import com.polopoly.cm.app.policy.SingleValued;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CMRuntimeException;
import com.polopoly.cm.client.Content;
import com.polopoly.cm.collections.ContentListUtil;
import com.polopoly.cm.policy.Policy;
import com.polopoly.plugin.PluginManagerPolicy;
import com.polopoly.plugin.PluginPolicy;
import com.polopoly.plugin.PluginWebResources;

/**
 * A PluginManagerPolicy based on the product one with the ability to exclude plugins, css and javascripts.
 *
 * @author mnova
 */
public class ThemePluginManagerPolicy extends PluginManagerPolicy {

    private static Logger LOG = Logger.getLogger(ThemePluginManagerPolicy.class.getName());

    private static final String EXCLUDE_PATTERN = "excludePattern";
    private static final String DEDUPLICATION_PATTERN = "deduplicationPattern";
    private static final String EXCLUDEDPLUGINS = "excludedplugins";

    private ReadWriteLock webResourceLock = new ReentrantReadWriteLock();
    private WebResourceCacheKey cacheKey = new WebResourceCacheKey();
    private ThemePluginWebResources cachedResources;

    @Override
    public PluginWebResources getWebResources() {
        boolean stale;
        try {
            webResourceLock.readLock().lock();
            stale = staleCache();
        } finally {
            webResourceLock.readLock().unlock();
        }

        if (stale) {
            try {
                webResourceLock.writeLock().lock();
                cachedResources = new ThemePluginWebResources(
                        super.getWebResources(),
                        getExcludePattern(),
                        getDeDuplicationPattern(),
                        getExcludedPlugins());
                cacheKey = new WebResourceCacheKey(newCacheKey(), newPatternsCacheKey(), newExcludedCacheKey());
            } finally {
                webResourceLock.writeLock().unlock();
            }
        }

        return cachedResources;
    }

    public String getExcludePattern() {
        return getChildValue(EXCLUDE_PATTERN, "");
    }

    public String getDeDuplicationPattern() {
        return getChildValue(DEDUPLICATION_PATTERN, "");
    }

    public List<PluginPolicy> getExcludedPlugins()
    {
        try {
            final List<PluginPolicy> plugins = Lists.newArrayList();
            for (ContentReference ref : ContentListUtil.iterable(getContentList(EXCLUDEDPLUGINS))) {
                ContentId pluginId = ref.getReferredContentId();
                try {
                    Policy policy = getCMServer().getPolicy(pluginId);
                    if (policy instanceof PluginPolicy) {
                        plugins.add((PluginPolicy) policy);
                    } else {
                        String plugin = pluginId.getContentIdString();
                        Content content = policy.getContent();
                        if (content.getExternalId() != null) {
                            plugin += String.format(" (extid=%s)", content.getExternalId().getExternalId());
                        }
                        if (content.getName() != null) {
                            plugin += String.format(" (name=%s)", content.getName());
                        }
                        LOG.log(Level.WARNING, "Invalid plugin " + plugin + " got policy " + policy.getClass().getName() + " instead of PluginPolicy");
                    }
                } catch (CMException cme) {
                    LOG.log(Level.WARNING, "Unable to get plugin: " + pluginId, cme);
                }
            }
            return plugins;

        } catch (CMException cme) {
            throw new RuntimeException("Unable to get list of plugins", cme);
        }
    }

    protected boolean staleCache() {
        return cacheKey.staleCache(newCacheKey(), newPatternsCacheKey(), newExcludedCacheKey());
    }

    protected String newPatternsCacheKey() {
        return getExcludePattern() + "-" + getDeDuplicationPattern();
    }

    protected Set<VersionedContentId> newExcludedCacheKey() {
        Set<VersionedContentId> newKey = new HashSet<VersionedContentId>();

        try {
            for (ContentReference ref : ContentListUtil.iterable(getContentList(EXCLUDEDPLUGINS))) {
                newKey.add(getCMServer().translateSymbolicContentId(ref.getReferredContentId()));
            }
        } catch (CMException e) {
            throw new CMRuntimeException("Failed to read list of plugin references from plugin manager", e);
        }
        newKey.add(getContentId());
        return Collections.unmodifiableSet(newKey);
    }

    protected String getChildValue(final String name, final String defaultValue) {
        String value = null;

        try {
            final SingleValued e = (SingleValued) this.getChildPolicy(name);
            if (e != null) {
                value = e.getValue();
            }
        } catch (ClassCastException e) {
            logger.log(Level.WARNING, name + " in " + this.getContentId() + " has unsupported policy.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting child value", e);
        }

        return ((value != null) ? value : defaultValue);
    }

    public class WebResourceCacheKey {
        private Set<VersionedContentId> resourcesIds = null;
        private String patterns = null;
        private Set<VersionedContentId> excludedIds = null;

        public WebResourceCacheKey() {
        }

        public WebResourceCacheKey(final Set<VersionedContentId> resourcesIds,
                                   final String patterns,
                                   final Set<VersionedContentId> excludedIds) {
            this.resourcesIds = resourcesIds;
            this.patterns = patterns;
            this.excludedIds = excludedIds;
        }

        public boolean staleCache(final Set<VersionedContentId> newResourcesIds,
                                  final String newPatterns,
                                  final Set<VersionedContentId> newExcludedIds) {
            if (resourcesIds == null) {
                return true;
            }
            if (patterns == null) {
                return true;
            }
            if (excludedIds == null) {
                return true;
            }
            return (!resourcesIds.equals(newResourcesIds) ||
                    (!patterns.equals(newPatterns)) ||
                    (!excludedIds.equals(newExcludedIds)));
        }
    }

}

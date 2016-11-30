package com.atex.plugins.themes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.polopoly.plugin.PluginFilesPolicy;
import com.polopoly.plugin.PluginPolicy;
import com.polopoly.plugin.PluginWebResource;
import com.polopoly.plugin.PluginWebResources;

/**
 * A wrapper for {@link PluginWebResources} which allow us to keep the existing
 * resource list as it is and create a "virtual copy".
 *
 * @author mnova
 */
public class ThemePluginWebResources implements PluginWebResources {

    private final static List<String> NOKEY = Lists.newArrayList("NOKEY", "NOKEY", "NOKEY");
    private final static List<String> JSKEY = Lists.newArrayList("type=javascript", "location=undefined", "scope=internal");

    private ConcurrentMap<List<String>, List<String>> removeMap = new ConcurrentHashMap<>();
    private ConcurrentMap<List<String>, List<PluginWebResource>> cachedFilterings = new ConcurrentHashMap<>();

    private final PluginWebResources webResources;
    private final String excludePattern;
    private final String deDuplicationPattern;
    private final List<PluginPolicy> excludedPlugins;

    public ThemePluginWebResources(final PluginWebResources webResources, final String excludePattern,
                                   final String deDuplicationPattern, final List<PluginPolicy> excludedPlugins) {
        this.webResources = webResources;
        this.excludePattern = excludePattern;
        this.deDuplicationPattern = deDuplicationPattern;
        this.excludedPlugins = excludedPlugins;
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
                .filter( r -> !isExcludedResource(r) && !isDuplicatedResource(NOKEY, r) )
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginWebResource> filter(final String... filter) {
        final List<String> key = Arrays.asList(filter);
        List<PluginWebResource> resources = cachedFilterings.get(key);
        if (resources == null) {

            // for the de-duplication of resources we should use a cache key
            // that will be the same for javascript in the footer or in the
            // header.

            final List<String> filteringKey = getCachedDuplicationKey(filter);
            resources = webResources
                    .filter(filter)
                    .stream()
                    .filter( r -> !isExcludedResource(r) && !isDuplicatedResource(filteringKey, r))
                    .collect(Collectors.toList());
            cachedFilterings.putIfAbsent(key, resources);
        }

        return resources;
    }

    public List<String> getCachedDuplicationKey(final String... filter) {
        final List<String> key = Arrays.asList(filter);
        if (key.size() == 3 && key.get(0).equals("type=javascript")) {
            if (!key.get(2).equals("scope=external")) {
                return JSKEY;
            }
        }
        return key;
    }

    private boolean isExcludedResource(final PluginWebResource resource) {
        for (final Pattern re : getExcludePatternList()) {
            final String resourceIdentifier = resource.getIdentifier();
            if (re.matcher(resourceIdentifier).find()) {
                return true;
            }
        }
        if (excludedPlugins.size() > 0) {
            for (final PluginPolicy plugin : excludedPlugins) {
                final PluginFilesPolicy files = plugin.getFilesContent();
                if ((files != null) && files.getContentId().equalsIgnoreVersion(resource.getContentId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDuplicatedResource(final List<String> key, final PluginWebResource resource) {
        final List<Pattern> patternList = getDeDuplicationPatternList();
        if (patternList.size() > 0) {
            final List<String> patternsAlreadyAdded = removeMap.putIfAbsent(key, Lists.newArrayList());
            for (final Pattern re : patternList) {
                final String resourceIdentifier = resource.getIdentifier();
                if (re.matcher(resourceIdentifier).find()) {
                    final String pattern = re.pattern();
                    if (patternsAlreadyAdded.contains(pattern)) {
                        return true;
                    } else {
                        patternsAlreadyAdded.add(pattern);
                    }

                    return false;
                }
            }
        }
        return false;
    }

    private List<Pattern> getExcludePatternList() {
        return patternToList(excludePattern);
    }

    private List<Pattern> getDeDuplicationPatternList() {
        return patternToList(deDuplicationPattern);
    }

    private List<Pattern> patternToList(final String pattern) {
        return Lists.newArrayList(
                Iterables.transform(
                        Splitter
                                .onPattern("\r\n|\r|\n")
                                .omitEmptyStrings()
                                .split(pattern),
                        new Function<String, Pattern>() {
                            @Nullable
                            @Override
                            public Pattern apply(@Nullable final String s) {
                                return Pattern.compile(s);
                            }
                        }));
    }

}

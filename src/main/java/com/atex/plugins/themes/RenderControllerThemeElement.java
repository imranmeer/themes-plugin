package com.atex.plugins.themes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.model.ModelPathUtil;
import com.polopoly.plugin.PluginManagerPolicy;
import com.polopoly.plugin.PluginWebResource;
import com.polopoly.render.CacheInfo;
import com.polopoly.render.RenderRequest;
import com.polopoly.siteengine.dispatcher.ControllerContext;
import com.polopoly.siteengine.model.TopModel;
import com.polopoly.siteengine.mvc.RenderControllerBase;

/**
 * Element code controller. Populates a model with custom code.
 */
public class RenderControllerThemeElement extends RenderControllerBase {

    private static Logger LOG = Logger.getLogger(RenderControllerThemeElement.class.getName());

    private static final ExternalContentId PLUGINS_CONTENT_ID = new ExternalContentId("p.siteengine.Plugins.d");
    private static final String DEDUP_ALREADYADDED_ATTR = RenderControllerThemeElement.class.getName() + ".dedupe";

    @Override
    public void populateModelAfterCacheKey(RenderRequest request, TopModel m, CacheInfo cacheInfo, ControllerContext context) {
        PolicyCMServer cmServer = getCmClient(context).getPolicyCMServer();

        // To add plugins web resources with exclusion of resources name
        // that matched exclusion pattern defined by user in GUI

        if (m.getLocal().getAttribute("webResources") == null) {

            final ThemeElementPolicy themePolicy = (ThemeElementPolicy) ModelPathUtil.getBean(context.getContentModel());

            try {
                final PluginManagerPolicy pluginManagerPolicy = this.getPluginManager(cmServer);
                final ThemePluginWebResources webResources = new ThemePluginWebResources(pluginManagerPolicy.getWebResources());

                final Collection<PluginWebResource> filteredCollection = webResources.getAll();

                final List<Pattern> exclusionTokens = themePolicy.getExcludePatternList();
                for (final Pattern token : exclusionTokens) {
                    final Iterator<PluginWebResource> iterator = filteredCollection.iterator();
                    while (iterator.hasNext()) {
                        final PluginWebResource webResource = iterator.next();
                        final String resourceIdentifier = webResource.getIdentifier();

                        if (token.matcher(resourceIdentifier).find()) {
                            LOG.log(Level.FINE, "removing " + webResource.getContentId().getContentIdString() + resourceIdentifier + " matches " + token.pattern());

                            // we are concatenating the resources we have to remove
                            // the resource from the global list since the concatenated
                            // file is served by a different servlet.

                            if (pluginManagerPolicy.isConcatenateResources()) {
                                iterator.remove();
                            } else {
                                webResources.markResourceAsRemoved(webResource);
                            }
                        }
                    }
                }

                final List<Pattern> deduplicationList = themePolicy.getDeDuplicationPatternList();
                if (deduplicationList.size() > 0) {

                    // the list must be shared across the request since we may have the same
                    // files in the header or in the footer.

                    String fileType = Optional
                            .fromNullable(Strings.emptyToNull((String) request.getAttribute("filetype")))
                            .or("all");

                    // javascript in the footer or in the header must use the same list so a js added in the
                    // header will not be re-added in the footer.

                    if (fileType.equals("jsfoot")) {
                        fileType = "jshead";
                    }

                    Map<String, List<String>> patternsAlreadyAddedMap = (Map<String, List<String>>) request.getAttribute(DEDUP_ALREADYADDED_ATTR);
                    if (patternsAlreadyAddedMap == null) {
                        patternsAlreadyAddedMap = Maps.newHashMap();
                    }
                    final List<String> patternsAlreadyAdded = patternsAlreadyAddedMap.computeIfAbsent(fileType, k -> Lists.newArrayList());
                    for (final Pattern deduplication : deduplicationList) {
                        final Iterator<PluginWebResource> iterator = filteredCollection.iterator();
                        while (iterator.hasNext()) {
                            final PluginWebResource webResource = iterator.next();
                            final String resourceIdentifier = webResource.getIdentifier();

                            if (deduplication.matcher(resourceIdentifier).find()) {
                                final String pattern = deduplication.pattern();
                                if (patternsAlreadyAdded.contains(pattern)) {
                                    LOG.log(Level.FINE, "removing " + webResource.getContentId().getContentIdString() + resourceIdentifier + " pattern " + pattern + " already added");

                                    // we are concatenating the resources we have to remove
                                    // the resource from the global list since the concatenated
                                    // file is served by a different servlet.

                                    if (pluginManagerPolicy.isConcatenateResources()) {
                                        iterator.remove();
                                    } else {
                                        webResources.markResourceAsRemoved(webResource);
                                    }

                                } else {
                                    patternsAlreadyAdded.add(pattern);
                                }
                            }
                        }

                    }
                    request.setAttribute(DEDUP_ALREADYADDED_ATTR, patternsAlreadyAddedMap);
                }

                LOG.log(Level.FINE, "Setting webresources in local scope");
                m.getLocal().setAttribute("useConcatenation", pluginManagerPolicy.isConcatenateResources());
                m.getLocal().setAttribute("webResources", webResources);
            } catch (CMException e) {
                LOG.log(Level.WARNING, "Error adding web resources", e);
            }
        }

        try {
            final String filetype = (String) request.getAttribute("filetype");

            if (filetype != null) {

                final ThemeElementPolicy p = (ThemeElementPolicy) cmServer.getPolicy(context.getContentId());

                final List<WebFileResource> allfiles = new ArrayList<WebFileResource>();
                final List<WebFileResource> files = p.getFiles(filetype);

                if (CollectionUtils.isNotEmpty(files)) {
                    allfiles.addAll(files);
                }

                final ThemeElementPolicy basep = p.getBaseThemePolicy();
                if (basep != null) {
                    final List<WebFileResource> basefiles = basep.getFiles(filetype);
                    if (basefiles != null && basefiles.size() > 0) {
                        allfiles.addAll(basefiles);
                    }
                }

                m.getLocal().setAttribute("theme", p.getContentId().getContentIdString());
                m.getLocal().setAttribute("files", allfiles);
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getLocalizedMessage(), e);
        }

    }

    private PluginManagerPolicy getPluginManager(PolicyCMServer cmServer) throws CMException {
        return (PluginManagerPolicy) cmServer.getPolicy(PLUGINS_CONTENT_ID);
    }

}
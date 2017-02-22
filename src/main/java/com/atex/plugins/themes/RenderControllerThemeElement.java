package com.atex.plugins.themes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;

import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.model.ModelPathUtil;
import com.polopoly.plugin.PluginManagerPolicy;
import com.polopoly.plugin.PluginWebResources;
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

    @Override
    public void populateModelAfterCacheKey(RenderRequest request, TopModel m, CacheInfo cacheInfo, ControllerContext context) {
        PolicyCMServer cmServer = getCmClient(context).getPolicyCMServer();

        // To add plugins web resources with exclusion of resources name
        // that matched exclusion pattern defined by user in GUI

        if (m.getLocal().getAttribute("webResources") == null) {
            try {
                final PluginManagerPolicy pluginManagerPolicy = this.getPluginManager(cmServer);
                final PluginWebResources webResources = pluginManagerPolicy.getWebResources();

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

                final ThemeElementPolicy themePolicy = (ThemeElementPolicy) ModelPathUtil.getBean(context.getContentModel());

                final List<WebFileResource> allfiles = new ArrayList<WebFileResource>();
                final List<WebFileResource> files = themePolicy.getFiles(filetype);

                if (CollectionUtils.isNotEmpty(files)) {
                    allfiles.addAll(files);
                }

                final ThemeElementPolicy basep = themePolicy.getBaseThemePolicy();
                if (basep != null) {
                    final List<WebFileResource> basefiles = basep.getFiles(filetype);
                    if (basefiles != null && basefiles.size() > 0) {
                        allfiles.addAll(basefiles);
                    }
                }

                m.getLocal().setAttribute("theme", themePolicy.getContentId().getContentIdString());
                m.getLocal().setAttribute("files", allfiles);
                m.getLocal().setAttribute("isPrintCssExist", themePolicy.isPrintCssExist());
                m.getLocal().setAttribute("isScreenCssExist", themePolicy.isScreenCssExist());
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getLocalizedMessage(), e);
        }

    }

    private PluginManagerPolicy getPluginManager(PolicyCMServer cmServer) throws CMException {
        return (PluginManagerPolicy) cmServer.getPolicy(PLUGINS_CONTENT_ID);
    }

}
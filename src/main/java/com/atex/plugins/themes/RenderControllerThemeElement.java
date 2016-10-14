package com.atex.plugins.themes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.polopoly.model.ModelPathUtil;
import com.polopoly.plugin.PluginWebResource;
import org.apache.commons.collections.CollectionUtils;

import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.render.CacheInfo;
import com.polopoly.render.RenderRequest;
import com.polopoly.siteengine.dispatcher.ControllerContext;
import com.polopoly.siteengine.model.TopModel;
import com.polopoly.siteengine.mvc.RenderControllerBase;

import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.plugin.PluginManagerPolicy;
import com.polopoly.plugin.PluginWebResources;

/**
 * Element code controller. Populates a model with custom code.
 * 
 */
public class RenderControllerThemeElement extends RenderControllerBase {

	private static Logger LOG = Logger.getLogger(RenderControllerThemeElement.class.getName());
	private static final ExternalContentId PLUGINS_CONTENT_ID = new ExternalContentId("p.siteengine.Plugins.d");

	@Override
	public void populateModelAfterCacheKey(RenderRequest request, TopModel m, CacheInfo cacheInfo, ControllerContext context) {
		PolicyCMServer cmServer = getCmClient(context).getPolicyCMServer();

		/*
		To add plugins web resources with exclusion of resources name
		that matched exclusion pattern defined by user in GUI
		*/
		if(m.getLocal().getAttribute("webResources") == null) {

			try {
				PluginManagerPolicy e = this.getPluginManager(cmServer);
				PluginWebResources webResources = e.getWebResources();

				Collection<PluginWebResource> filteredCollection = webResources.getAll();

				String exclusion = (String)ModelPathUtil.get(context.getContentModel(), "excludePattern/value");
				if(exclusion!="" && exclusion!=null){
					StringTokenizer exclusionTokens = new StringTokenizer(exclusion,";");
					while (exclusionTokens.hasMoreTokens()) {
						String token = exclusionTokens.nextToken();

						Iterator<PluginWebResource> iterator = filteredCollection.iterator();
						while (iterator.hasNext()) {
							PluginWebResource webResource = iterator.next();
							String resourceIdentifier = webResource.getIdentifier();

							if (resourceIdentifier.contains(token)) {
								iterator.remove();
							}
						}
					}
				}

				LOG.log(Level.FINE, "Setting webresources in GLOBAL scope");
				m.getLocal().setAttribute("useConcatenation", Boolean.valueOf(e.isConcatenateResources()));
				m.getLocal().setAttribute("webResources", webResources);
			} catch (CMException var9) {
				LOG.log(Level.WARNING, "Error adding web resources", var9);
			}
		}

		try {
			String filetype = (String) request.getAttribute("filetype");

			if (filetype != null) {

				ThemeElementPolicy p = (ThemeElementPolicy) cmServer.getPolicy(context.getContentId());

				ArrayList<WebFileResource> allfiles = new ArrayList<WebFileResource>();
				ArrayList<WebFileResource> files = p.getFiles(filetype);

				if (CollectionUtils.isNotEmpty(files)) {
					allfiles.addAll(files);
				}

				ThemeElementPolicy basep = p.getBaseThemePolicy();
				if (basep != null) {
					ArrayList<WebFileResource> basefiles = basep.getFiles(filetype);
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
		return (PluginManagerPolicy)cmServer.getPolicy(PLUGINS_CONTENT_ID);
	}

}
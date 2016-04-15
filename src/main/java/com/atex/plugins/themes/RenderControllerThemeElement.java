package com.atex.plugins.themes;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;

import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.render.CacheInfo;
import com.polopoly.render.RenderRequest;
import com.polopoly.siteengine.dispatcher.ControllerContext;
import com.polopoly.siteengine.model.TopModel;
import com.polopoly.siteengine.mvc.RenderControllerBase;

/**
 * Element code controller. Populates a model with custom code.
 * 
 */
public class RenderControllerThemeElement extends RenderControllerBase {

	private static Logger LOG = Logger.getLogger(RenderControllerThemeElement.class.getName());

	@Override
	public void populateModelAfterCacheKey(RenderRequest request, TopModel m, CacheInfo cacheInfo, ControllerContext context) {
		try {
			String filetype = (String) request.getAttribute("filetype");

			if (filetype != null) {
				PolicyCMServer cmServer = getCmClient(context).getPolicyCMServer();

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

}

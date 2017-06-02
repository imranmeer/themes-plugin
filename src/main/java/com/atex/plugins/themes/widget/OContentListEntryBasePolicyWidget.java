package com.atex.plugins.themes.widget;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.DefaultMajorNames;
import com.polopoly.cm.app.ContentSession;
import com.polopoly.cm.app.util.UntitledListEntryToContentIdTitle;
import com.polopoly.cm.app.widget.OStandardContentListEntryPolicyWidget;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.collections.ContentListRead;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.cm.policy.PolicyUtil;
import com.polopoly.orchid.OrchidException;
import com.polopoly.orchid.context.Device;
import com.polopoly.orchid.context.OrchidContext;
import com.polopoly.statistics.client.StatisticsClient;
import com.polopoly.statistics.pageviews.PageViews;
import com.polopoly.statistics.pageviews.PageViewsManager;
import com.polopoly.statistics.time.TimeResolution;
import com.polopoly.util.LocaleUtil;

import javax.servlet.ServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OContentListEntryBasePolicyWidget extends OStandardContentListEntryPolicyWidget {

    private static final long serialVersionUID = 3862788076072604912L;

    protected static String CLASS = OContentListEntryBasePolicyWidget.class.getName();
    protected static Logger logger = Logger.getLogger(CLASS);
    
    protected String _editInfo;

    protected ContentListRead _contentList;
    protected PolicyCMServer _cmServer;
    protected UntitledListEntryToContentIdTitle untitledToContentIdTitle =
            new UntitledListEntryToContentIdTitle();

    public void initSelf(OrchidContext oc) throws OrchidException
    {
        super.initSelf(oc);
        _contentList = entryContainer.getContentList();
        _cmServer = getContentSession().getPolicyCMServer();
        
        if (contentIcon != null) {
            contentIcon.setStylesheetClass("icon");
        }

        ContentPolicy contentPolicy = (ContentPolicy) getPolicy();

        if (contentPolicy != null) {
            try {
                _editInfo =
                        getDateString("cm.label.Created", "cm.label.Modified",
                                getCreationDate(contentPolicy), getCommittedDate(contentPolicy), oc);

                ContentId contentId = contentPolicy.getContentId().getContentId();

                if (contentLink != null) {
                    contentLink
                            .setTitle("Id: " + contentId.getContentIdString() + ". " + _editInfo);
                }
                
                String label =
                        untitledToContentIdTitle.createTitle(contentLink.getLabel(), contentId);
                contentLink.setLabel(label);

                String inputTemplateName =
                        contentPolicy.getInputTemplate().getComponent("polopoly.Client", "label");
                if (contentIcon != null) {
                    contentIcon.setTitle(inputTemplateName);
                    contentIcon.setAltText(inputTemplateName);
                }

            } catch (Exception cme) {
                logger.logp(Level.WARNING, CLASS, "initSelf", "Could not get input template", cme);
            }
        }
    }

    protected void renderToolbox(Device device, OrchidContext oc) throws OrchidException,
            IOException
    {
        // print tools
        device.println("<div class='tools'>");
        renderToolButtons(device, oc);
        device.println("</div>");
    }

    protected void renderToolButtons(Device device, OrchidContext oc) throws OrchidException,
            IOException
    {
        if (entryContainer.showCopyButton() && copyContentButton != null) {
            copyContentButton.render(oc);
        }

        if (entryContainer.showActionButtons()) {
            if (getContentSession().getMode() == ContentSession.VIEW_MODE) {
                Boolean showActionButtonsInViewMode =
                        PolicyUtil.getParameterAsBooleanObject("showActionButtonsInViewMode",
                                Boolean.TRUE, entryContainer.getPolicy());
                if (showActionButtonsInViewMode.booleanValue()) {
                    removeButton.render(oc);
                }
            } else {
                removeButton.render(oc);
            }
        }

        if (entryContainer.showCheckbox() && checkbox != null) {
            checkbox.render(oc);
        }
    }

    protected void renderStatistics(Device device, OrchidContext oc) throws OrchidException,
            IOException
    {
        ContentId uvContentId = contentId.getContentId();
        try {
            StatisticsData stats = getStatisticsData(oc);

            if (stats != null && stats.isStatisticsAvailable()) {
                int hourViews = stats.getHourPageViews(uvContentId);
                int dayViews = stats.getDayPageViews(uvContentId);

                String viewsHourString =
                        LocaleUtil.format("cm.widget.ContentListEntryBase.ViewsThisHour", oc
                                .getMessageBundle());

                String viewsDayString =
                        LocaleUtil.format("cm.widget.ContentListEntryBase.ViewsThisDay", oc
                                .getMessageBundle());

                device.println("<p>" + hourViews + " " + viewsHourString + ". " + dayViews + " "
                        + viewsDayString + ".</p>");
            }
        } catch (CMException cme) {
            logger.logp(Level.WARNING, CLASS, "initSelf", "Could not get statistics for "
                    + uvContentId, cme);
        }
    }

    protected void renderHeader(Device device, OrchidContext oc) throws OrchidException,
            IOException
    {
        device.println("<div class='customEntry'>");
        device.println("<div class='customContainer'>");

        renderToolbox(device, oc);

        device.println("<h2>");
        if (showContentInfo) {
            if (contentLockInfo.getLockInfo() != null && contentLockInfo.isLocked()) {
                contentLockInfo.render(oc);
            }
            contentIcon.render(oc);
            contentLink.render(oc);
        }
        device.println("</h2>");
    }

    protected void renderEditInfo(Device device, OrchidContext oc) throws OrchidException,
            IOException
    {
        device.println("<p class='meta'>" + _editInfo + "</p>");
    }

    protected void renderFooter(Device device, OrchidContext oc) throws OrchidException,
            IOException
    {
        device.println("</div>");
        device.println("</div>");
    }

    public void localRender(OrchidContext oc) throws OrchidException, IOException
    {
        Device device = oc.getDevice();
        renderHeader(device, oc);

        renderEditInfo(device, oc);

        renderStatistics(device, oc);
        renderFooter(device, oc);
    }



	protected StatisticsData getStatisticsData(OrchidContext oc) throws CMException
    {
        // Check if we have data in scope already, otherwise create
        ServletRequest request = oc.getDevice().getRequest();
        String dataAttribute = CLASS + ".statistics";

        if (_contentList != null) {
            dataAttribute += "." + _contentList.getContentListStorageGroup();
        }

        StatisticsData data = (StatisticsData) request.getAttribute(dataAttribute);
        if (data == null) {
            ContentId uvContentId = contentId.getContentId();

            if (uvContentId.getMajor() == _cmServer.getMajorByName(DefaultMajorNames.ARTICLE)) {
                data = createStatisticsData(oc);
                request.setAttribute(dataAttribute, data);
            }
        }

        return data;
    }

    protected StatisticsData createStatisticsData(OrchidContext oc)
    {
        StatisticsClient statisticsClient =
                (StatisticsClient) oc.getApplication().getApplicationComponent(
                        StatisticsClient.DEFAULT_COMPOUND_NAME);
        PageViewsManager pageViewsManager = statisticsClient.getPageViewsManager();

        StatisticsData data = new StatisticsData();

        for (int i = 0; _contentList != null && i < _contentList.size(); i++) {
            try {
                ContentId referredContentId =
                        _contentList.getEntry(i).getReferredContentId().getContentId();

                if (referredContentId.getMajor() == _cmServer
                        .getMajorByName(DefaultMajorNames.ARTICLE)) {

                    String path = "/";
                    String id = referredContentId.getContentIdString();

                    PageViews hourPageViews =
                            pageViewsManager.getPageViews("article", new TimeResolution(
                                    TimeResolution.HOUR), true);
                    int hourHits = hourPageViews.getPageViews(path, id);
                    data.setHourPageViews(referredContentId, new Integer(hourHits));

                    PageViews dayPageViews =
                            pageViewsManager.getPageViews("article", new TimeResolution(
                                    TimeResolution.DAY), true);
                    int dayHits = dayPageViews.getPageViews(path, id);
                    data.setDayPageViews(referredContentId, new Integer(dayHits));
                }
            } catch (CMException e) {
                logger.logp(Level.WARNING, CLASS, "localRender",
                        "Error getting stats for content list.", e);
            }
        }

        return data;
    }

    protected class StatisticsData {
        private Map allHourPageViews = new HashMap();
        private Map allDayPageViews = new HashMap();

        // Statistics is considered available if at least one article
        // in list has at least one hit today
        private boolean statisticsAvailable = false;

        boolean isStatisticsAvailable()
        {
            return statisticsAvailable;
        }

        public void setHourPageViews(ContentId referredContentId, Integer hourPageViews)
        {
            allHourPageViews.put(referredContentId, hourPageViews);
        }

        public void setDayPageViews(ContentId referredContentId, Integer dayPageViews)
        {
            if (dayPageViews.intValue() > 0) {
                this.statisticsAvailable = true;
            }
            allDayPageViews.put(referredContentId, dayPageViews);
        }

        int getHourPageViews(ContentId id)
        {
            return getPageViews(allHourPageViews, id);
        }

        int getDayPageViews(ContentId id)
        {
            return getPageViews(allDayPageViews, id);
        }

        private int getPageViews(Map map, ContentId id)
        {
            int result = 0;

            Integer views = (Integer) map.get(id.getContentId());
            if (views != null) {
                result = views.intValue();
            }

            return result;
        }
    }
}

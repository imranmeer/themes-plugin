package com.atex.plugins.themes.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.ContentReference;
import com.polopoly.cm.app.ContentCreator;
import com.polopoly.cm.app.Editor;
import com.polopoly.cm.app.SessionEvent;
import com.polopoly.cm.app.SessionListener;
import com.polopoly.cm.app.SessionListenerRegistry;
import com.polopoly.cm.app.Viewer;
import com.polopoly.cm.app.impl.SessionListenerRegistryImpl;
import com.polopoly.cm.app.orchid.event.Actions;
import com.polopoly.cm.app.orchid.event.impl.EventDataUtil;
import com.polopoly.cm.app.orchid.model.impl.InputTemplateCategoryModel;
import com.polopoly.cm.app.util.PolicyWidgetUtil;
import com.polopoly.cm.app.widget.ComplexPolicyWidget;
import com.polopoly.cm.app.widget.ContentListEntryContainer;
import com.polopoly.cm.app.widget.OAbstractPolicyWidget;
import com.polopoly.cm.app.widget.OComplexFieldPolicyWidget;
import com.polopoly.cm.app.widget.OContentListAwareEditorPolicyWidget;
import com.polopoly.cm.app.widget.OContextContentViewWidget;
import com.polopoly.cm.app.widget.OContextTopPolicyWidget;
import com.polopoly.cm.app.widget.ODuplicatorPolicyWidget;
import com.polopoly.cm.app.widget.OPolicyWidget;
import com.polopoly.cm.app.widget.impl.AutoInsertInfo;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.ContentListAware;
import com.polopoly.cm.policy.Policy;
import com.polopoly.cm.policy.PolicyUtil;
import com.polopoly.orchid.OrchidException;
import com.polopoly.orchid.context.OrchidContext;
import com.polopoly.orchid.event.EventData;
import com.polopoly.orchid.event.OrchidEvent;
import com.polopoly.orchid.event.WidgetEventListener;
import com.polopoly.orchid.util.MessageUtil;
import com.polopoly.orchid.util.WidgetUtil;
import com.polopoly.orchid.widget.OSubmitButton;
import com.polopoly.orchid.widget.OWidget;
import com.polopoly.util.LocaleUtil;

public class OSimpleContentCreatorPolicyWidget extends OComplexFieldPolicyWidget implements Editor, Viewer {
    public static final String APPEND_LIST_NAME_PREFIX = "appendListNamePrefix";
    private List sessionListeners = new ArrayList();
    private SessionListenerRegistry sessionListenerRegistry;
    private OSubmitButton createButton;
    private int createMajor;
    private ContentId inputTemplateId;
    private OPolicyWidget inContextWidget;
    private String createdContextName;
    private boolean editInContext;
    private boolean useSecurityParentForCreateCheck = false;
    private String autoInsertContentListRelativeName;

    public OSimpleContentCreatorPolicyWidget() {
        super("contentCreator");
    }

    public void initSelf(OrchidContext oc) throws OrchidException {
        this.createMajor = PolicyUtil.getParameterAsInt("major", 1, getPolicy());

        this.inputTemplateId = PolicyUtil.getIdParameter("inputTemplateId", getPolicy());

        this.autoInsertContentListRelativeName = PolicyUtil.getParameter("autoInsertContentListRelativeName", getPolicy());

        this.useSecurityParentForCreateCheck = PolicyUtil.getParameterAsBoolean("useSecurityParentForCreateCheck", false,
                getPolicy());

        this.editInContext = PolicyUtil.getParameterAsBoolean("editInContext", false, getPolicy());

        this.createdContextName = PolicyUtil.getParameter("createdContextName", "orchid_CONTENTCREATOR", getPolicy());

        String createLabel = PolicyUtil.getParameter("createLabel", "cm.action.Create", getPolicy());

        this.createButton = new OSubmitButton();
        this.createButton.setLabel(LocaleUtil.format(createLabel, oc.getMessageBundle()));
        try {
            this.createButton.setVisible(InputTemplateCategoryModel.hasPermission(this.createMajor, getContentSession()
                    .getPolicyCMServer(), getSecurityParentId(), getContentSession().getUserId(),
                    this.useSecurityParentForCreateCheck));
        } catch (CMException e) {
            this.createButton.setVisible(false);
            logger.log(Level.WARNING, "Permission check error", e);
        }
        addAndInitChild(oc, this.createButton);

        this.createButton.addSubmitListener(new WidgetEventListener() {
            public void processEvent(OrchidContext oc, OrchidEvent oe) throws OrchidException {
                try {
                    Policy policy = getContentCreator().createContent(createMajor, inputTemplateId);

                    ContentId createdId = policy.getContentId();
                    if ((sessionListenerRegistry != null) && (sessionListeners != null)) {
                        Iterator iter = sessionListeners.iterator();
                        while (iter.hasNext()) {
                            SessionListener sessionListener = (SessionListener) iter.next();
                            if (sessionListener != null) {
                                sessionListenerRegistry.addListener(createdId, sessionListener);
                            }
                        }
                    }
                    boolean hasContextInputTemplate = PolicyWidgetUtil.hasContextInputTemplate(createdContextName, policy);
                    if ((!editInContext) || (!hasContextInputTemplate)) {
                        EventData eventData = EventDataUtil.createContentEventData("editNew", createdId, true);
                        if (Actions.isValid(oc, eventData, getContentSession().getUserId(), getContentSession()
                                .getPolicyCMServer())) {
                            Map eventDataParams = PolicyWidgetUtil.getContentSessionParameters(getPolicy(), getContentSession()
                                    .getUserData());
                            if (eventDataParams != null) {
                                eventDataParams.put("insertionContentListName", getInsertInContentListName());

                                Iterator iter = eventDataParams.keySet().iterator();
                                while (iter.hasNext()) {
                                    String key = (String) iter.next();
                                    Object value = eventDataParams.get(key);
                                    if ((value instanceof String)) {
                                        eventData.setValue(key, (String) value);
                                    } else if ((value instanceof ContentId)) {
                                        eventData.setValue(key, ((ContentId) value).getContentIdString());
                                    }
                                }
                            }
                            Actions.sendEventData(oc, OSimpleContentCreatorPolicyWidget.this, eventData);
                        }
                    }
                    if (sessionListenerRegistry != null) {
                        ((SessionListenerRegistryImpl) sessionListenerRegistry).notifyListeners(createdId,
                                "OContentCreator.created");
                    }
                } catch (Exception e) {
                    OAbstractPolicyWidget.logger.logp(Level.WARNING, OAbstractPolicyWidget.CLASS, "processEvent",
                            "Could not create content", e);
                }
            }
        });
        if (this.editInContext) {
            listenAtCreatedContent(getSessionListenerRegistry(), new SessionListener() {
                public void processEvent(SessionEvent event) {
                    try {
                        OrchidContext oc = WidgetUtil.getOrchidContext();
                        if ("OContentCreator.created".equals(event.getAction())) {
                            Policy policy = getContentSession().getPolicyCMServer().getPolicy(event.getSourceContentId());
                            if (!PolicyWidgetUtil.hasContextInputTemplate(createdContextName, policy)) {
                                return;
                            }
                            inContextWidget = ((OPolicyWidget) PolicyWidgetUtil.getEditor(createdContextName, policy,
                                    OContextTopPolicyWidget.class));

                            inContextWidget.setContentSession(getContentSession());
                            if ((inContextWidget instanceof OContextContentViewWidget)) {
                                OContextContentViewWidget contextWidget = (OContextContentViewWidget) inContextWidget;

                                OPolicyWidget contentListWidget = getSiblingWidget(autoInsertContentListRelativeName);
                                contextWidget.setInitInEditMode(true);
                                if ((autoInsertContentListRelativeName != null) && (getContentSession().getMode() != 2)) {
                                    String contentListName = getInsertInContentListName();
                                    if ((contentListWidget instanceof ContentListAware)) {
                                        contentListName = ((ContentListAware) contentListWidget).getContentList()
                                                .getContentListStorageGroup();
                                    } else if ((contentListWidget instanceof ContentListEntryContainer)) {
                                        contentListName = ((ContentListEntryContainer) contentListWidget).getContentList()
                                                .getContentListStorageGroup();
                                    } else {
                                        Policy contentListWidgetPolicy = contentListWidget.getPolicy();
                                        if ((contentListWidgetPolicy instanceof ContentListAware)) {
                                            ContentListAware contentListAware = (ContentListAware) contentListWidgetPolicy;
                                            contentListName = contentListAware.getContentList().getContentListStorageGroup();
                                        }
                                    }
                                    contextWidget.setAutoInsertInfo(new AutoInsertInfo(contentListName, getName()));
                                }
                            }
                            addAndInitChildPolicyWidget(oc, inContextWidget);
                            if (getContentSession().getMode() == 2) {
                                discardChild(inContextWidget);

                                insertContent(oc, policy.getContentId());
                                inContextWidget = null;
                            }
                        } else if ("saveAndFastInsert".equals(event.getAction())
                                || "continueInsertionAndView".equals(event.getAction())) {
                            insertContent(oc, event.getSourceContentId());
                        }
                    } catch (Exception e) {
                        OAbstractPolicyWidget.logger.logp(Level.WARNING, OAbstractPolicyWidget.CLASS, "processEvent",
                                "Could not perform content list insertion", e);
                    }
                }
            });
        }
    }

    public ContentCreator getContentCreator() {
        return (ContentCreator) getPolicy();
    }

    private ContentId getSecurityParentId() {
        ContentId id = PolicyUtil.getIdForAliasParameter("securityParentAlias",
                PolicyUtil.getIdParameter("securityParent", getPolicy()), getPolicy(), getContentSession().getUserData());

        return id;
    }

    protected String getInsertInContentListName() {
        if (this.autoInsertContentListRelativeName != null) {
            try {
                return getPolicy().getParentPolicy().getChildPolicy(this.autoInsertContentListRelativeName).getPolicyName();
            } catch (Exception e) {
                logger.logp(Level.WARNING, CLASS, "getInsertInContentListName", "Could not find sibling policy: "
                        + this.autoInsertContentListRelativeName, e);
            }
        }
        String listName = PolicyUtil.getParameter("insertionContentListName", getPolicy());
        if (listName == null) {
            listName = PolicyUtil.getParameter("defaultInsertionContentListName", getPolicy());
        }
        if (listName == null) {
            listName = PolicyUtil.getParameter("insertInContentList", getPolicy());
        }
        if ((listName != null) && (PolicyUtil.getParameterAsBoolean("appendListNamePrefix", false, getPolicy()))) {
            String listNamePrefix = null;
            try {
                Policy parent = getPolicy().getParentPolicy();
                if (parent != null) {
                    listNamePrefix = parent.getPolicyName();
                }
            } catch (Exception e) {
                logger.logp(Level.WARNING, CLASS, "getInsertInContentListName", "Could not compute listNamePrefix", e);
            }
            if (listNamePrefix != null) {
                listName = listNamePrefix + "/" + listName;
            }
        }
        return listName;
    }

    public void releaseSelf() {
        this.inContextWidget = null;
    }

    public void listenAtCreatedContent(SessionListenerRegistry sessionListenerRegistry, SessionListener sessionListener) {
        this.sessionListenerRegistry = sessionListenerRegistry;
        this.sessionListeners.add(sessionListener);
    }

    protected void insertContent(OrchidContext oc) throws OrchidException {
        ContentId contentId = (ContentId) getContentSession().removeValue(getName());

        insertContent(oc, contentId);
    }

    private void insertContent(OrchidContext oc, ContentId contentId) throws OrchidException {
        if ((contentId != null) && (this.autoInsertContentListRelativeName != null) && (getContentSession().getMode() == 2)) {
            OPolicyWidget listWidget = getSiblingWidget(this.autoInsertContentListRelativeName);
            if (listWidget != null) {
                try {
                    if ((listWidget instanceof OContentListAwareEditorPolicyWidget)) {
                        ((OContentListAwareEditorPolicyWidget) listWidget).add(oc, new ContentReference(contentId.getContentId(),
                                null), 0);
                    } else if ((listWidget instanceof ContentListEntryContainer)) {
                        ((ContentListEntryContainer) listWidget)
                                .addEntry(new ContentReference(contentId.getContentId(), null), 0);
                    } else if (listWidget instanceof OContentSingleSelectPolicyWidget) {
                        ((OContentSingleSelectPolicyWidget) listWidget).selectContent(contentId.getContentId(), oc);
                    }
                } catch (Exception e) {
                    logger.logp(Level.WARNING, CLASS, "test", "Exception when publishing " + contentId + " in content list", e);

                    String message = LocaleUtil.format("cm.msg.InsertionFailed", oc.getMessageBundle());

                    MessageUtil.addErrorMessage(oc, message);
                }
            }
        }
    }

    private OPolicyWidget getSiblingWidget(String widgetName) {
        Iterator siblingWidgets = Collections.EMPTY_LIST.iterator();
        OWidget parentWidget = getParent();
        if ((parentWidget instanceof ODuplicatorPolicyWidget.ODuplicatorEntry)) {
            siblingWidgets = ((ODuplicatorPolicyWidget.ODuplicatorEntry) parentWidget).getEntryElements().iterator();
        } else {
            parentWidget = (OWidget) getParentPolicyWidget();
            if ((parentWidget instanceof ComplexPolicyWidget)) {
                siblingWidgets = ((ComplexPolicyWidget) parentWidget).getChildPolicyWidgets();
            }
        }
        if (siblingWidgets != null) {
            while (siblingWidgets.hasNext()) {
                OPolicyWidget child = (OPolicyWidget) siblingWidgets.next();
                String childName = child.getRelativeName();
                if (childName.equals(widgetName)) {
                    return child;
                }
            }
        }
        return null;
    }

    public void updateSelf(OrchidContext oc) throws OrchidException {
        insertContent(oc);
        if ((this.inContextWidget != null) && ((this.inContextWidget instanceof OContextContentViewWidget))
                && (((OContextContentViewWidget) this.inContextWidget).isFinished())) {
            discardChild(this.inContextWidget);
            this.inContextWidget = null;
        }
        this.createButton.setEnabled(this.inContextWidget == null);
    }

    public void localRender(OrchidContext oc) throws IOException, OrchidException {
        this.createButton.render(oc);
        if (this.inContextWidget != null) {
            this.inContextWidget.render(oc);
        }
    }
}

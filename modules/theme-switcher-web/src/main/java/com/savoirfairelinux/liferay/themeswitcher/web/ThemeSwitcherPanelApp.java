package com.savoirfairelinux.liferay.themeswitcher.web;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.application.list.BasePanelApp;
import com.liferay.application.list.PanelApp;
import com.liferay.application.list.constants.PanelCategoryKeys;
import com.liferay.portal.kernel.model.Portlet;
import com.savoirfairelinux.liferay.themeswitcher.meta.ThemeSwitcherPortletKeys;

/**
 * Module used to integrate the theme switcher configuration portlet into Liferay's left panel
 */
@Component(
    service = PanelApp.class,
    immediate = true,
    property = {
        "panel.category.key=" + PanelCategoryKeys.SITE_ADMINISTRATION_CONFIGURATION,
        "service.ranking:Integer=100"
    }
)
public class ThemeSwitcherPanelApp extends BasePanelApp {

    @Override
    public String getPortletId() {
        return ThemeSwitcherPortletKeys.PORTLET_NAME;
    }

    @Override
    @Reference(
        target = "(javax.portlet.name=" + ThemeSwitcherPortletKeys.PORTLET_NAME + ")",
        unbind = "-"
    )
    public void setPortlet(Portlet portlet) {
        super.setPortlet(portlet);
    }

}

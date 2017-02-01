package com.savoirfairelinux.themeswitch.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.PropsKeys;
import com.savoirfairelinux.liferay.themeswitcher.service.ThemeSwitcherService;

/**
 * This action is responsible for doing the actual theme switch on public pages
 */
@Component(
    immediate = true,
    service = LifecycleAction.class,
    property = {"key=" + PropsKeys.SERVLET_SERVICE_EVENTS_PRE}
)
public class ThemeSwitcherAction extends Action {

    private ThemeSwitcherService themeSwitcherService;

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {
        try {
            this.themeSwitcherService.switchTheme(request);
        } catch (PortalException e) {
            throw new ActionException(e);
        }
    }

    @Reference
    public void setThemeSwitcherService(ThemeSwitcherService themeSwitcherService) {
        this.themeSwitcherService = themeSwitcherService;
    }


}

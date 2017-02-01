package com.savoirfairelinux.liferay.themeswitcher.service;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * The theme switcher service provides the logic to determine whether or not a user can see the classic theme even
 * though a custom one has been specified.
 */
public interface ThemeSwitcherService {

    /**
     * Indicates whether or not the user can switch the themes between the classic one and the one defined in the layout
     * settings. A user must be logged in and be an administrator to perform this action.
     *
     * Also, theme switching only occurs on public pages. Not control panel ones.
     *
     * @param request The HTTP servlet request
     * @return True if the user can switch the theme, false otherwize
     *
     * @throws PortalException If we are unable to obtain all the required information
     */
    public void switchTheme(HttpServletRequest request) throws PortalException;

}

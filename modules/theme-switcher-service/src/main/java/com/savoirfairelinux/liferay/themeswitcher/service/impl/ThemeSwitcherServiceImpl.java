package com.savoirfairelinux.liferay.themeswitcher.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ColorScheme;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.Theme;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.PortletPreferencesLocalService;
import com.liferay.portal.kernel.service.ThemeLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ColorSchemeFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.savoirfairelinux.liferay.themeswitcher.meta.ThemeSwitcherPortletKeys;
import com.savoirfairelinux.liferay.themeswitcher.service.ThemeSwitcherService;
import com.savoirfairelinux.liferay.themeswitcher.service.configuration.ThemeSwitcherConfiguration;

/**
 * Default implementation of the theme switcher service
 */
@Component(
    immediate = true,
    service = ThemeSwitcherService.class
)
public class ThemeSwitcherServiceImpl implements ThemeSwitcherService {

    private static final Log LOG = LogFactoryUtil.getLog(ThemeSwitcherServiceImpl.class);

    private static final Pattern URL_PATTERN = Pattern.compile("^/(([a-z]{2}(_[A-Z]{2})?/)?web(/.*)?)?$");

    private GroupLocalService groupService;
    private Portal portal;
    private PortletPreferencesLocalService preferencesService;
    private ThemeLocalService themeService;

    @Override
    public void switchTheme(HttpServletRequest request) throws PortalException {
        User user = this.portal.getUser(request);
        String currentUrl = this.portal.getCurrentURL(request);

        if(URL_PATTERN.matcher(currentUrl).matches() && user != null) {
            this.evaluateSwitch(request, user);
        }

    }

    /**
     * Evaluates whether or not the theme should be switched and performs the switch if needed
     *
     * @param request The request
     * @param user The user
     */
    private void evaluateSwitch(HttpServletRequest request, User user) {
        long companyId = this.portal.getCompanyId(request);

        try {
            String groupUuid = this.groupService.getGroup(this.portal.getScopeGroupId(request)).getUuid();
            long controlPanelPlid = this.portal.getControlPanelPlid(companyId);
            List<Role> userRoles = user.getRoles();

            PortletPreferences preferences = this.preferencesService.getPreferences(
                companyId,
                PortletKeys.PREFS_OWNER_ID_DEFAULT,
                PortletKeys.PREFS_OWNER_TYPE_LAYOUT,
                controlPanelPlid,
                ThemeSwitcherPortletKeys.PORTLET_NAME,
                StringPool.BLANK
            );

            String enabledKey = ThemeSwitcherConfiguration.getKeyEnabled(groupUuid);
            String themeKey = ThemeSwitcherConfiguration.getKeyThemeId(groupUuid);
            String roleKey = ThemeSwitcherConfiguration.getKeyRoleUuid(groupUuid);

            boolean enabled = GetterUtil.getBoolean(preferences.getValue(enabledKey, ThemeSwitcherConfiguration.DEFAULT_ENABLED));
            String fallbackThemeId = preferences.getValue(themeKey, ThemeSwitcherConfiguration.DEFAULT_THEME_ID);
            List<String> roleUuids = Arrays.asList(preferences.getValues(roleKey, ThemeSwitcherConfiguration.DEFAULT_ROLE_UUIDS));

            boolean hasAtLeastOneSelectedRole = userRoles.stream()
                .map(userRole -> roleUuids.contains(userRole.getUuid()))
                .reduce(false, (hasFirstRole, hasSecondRole) -> (hasFirstRole || hasSecondRole));
            boolean isThemeBlank = StringPool.BLANK.equals(fallbackThemeId);

            if(enabled && !isThemeBlank && hasAtLeastOneSelectedRole) {
                this.doSwitch(companyId,request, fallbackThemeId);
            }

        } catch(PortalException e) {
            LOG.error("Error while fetching preferences", e);
        }
    }

    /**
     * Performs the theme switch. At this point, it has been determined that the switch can be performed.
     *
     * @param companyId The instance ID
     * @param request The HTTP request
     * @param fallbackThemeId The fallback theme ID
     */
    private void doSwitch(long companyId, HttpServletRequest request, String fallbackThemeId) {
        Theme currentTheme = (Theme) request.getAttribute(WebKeys.THEME);
        if(!currentTheme.getThemeId().equals(fallbackThemeId)) {
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            Theme fallBackTheme = this.themeService.getTheme(companyId, fallbackThemeId);
            if(fallBackTheme != null) {
                themeDisplay.setLookAndFeel(fallBackTheme, this.findDefaultColorScheme(fallBackTheme));
                request.setAttribute(WebKeys.THEME, fallBackTheme);
            } else {
                LOG.error("Theme \"" + fallbackThemeId + "\" not found");
            }
        }
    }

    /**
     * Finds the default color scheme of the given theme
     * @param theme The theme to scan for the color scheme
     * @return The default colorscheme of the theme or the default global colorscheme if the theme doesn't have any
     */
    private ColorScheme findDefaultColorScheme(Theme theme) {
        ColorScheme returnedScheme;
        List<ColorScheme> themeSchemes = theme.getColorSchemes();

        if(!themeSchemes.isEmpty()) {
            returnedScheme = themeSchemes.stream()
                .filter(colorScheme -> colorScheme.isDefaultCs())
                .findFirst()
                .orElse(ColorSchemeFactoryUtil.getDefaultRegularColorScheme());
        } else {
            returnedScheme = ColorSchemeFactoryUtil.getDefaultRegularColorScheme();
        }

        return returnedScheme;
    }

    /******************************************************************************************************************/
    /*                                                SERVICE REFERENCES                                              */
    /******************************************************************************************************************/

    @Reference
    public void setGroupService(GroupLocalService groupService) {
        this.groupService = groupService;
    }

    @Reference
    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    @Reference
    public void setPreferencesService(PortletPreferencesLocalService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @Reference
    public void setThemeService(ThemeLocalService themeService) {
        this.themeService = themeService;
    }

}

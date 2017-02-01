package com.savoirfairelinux.liferay.themeswitcher.web;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.Theme;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.ThemeLocalService;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.savoirfairelinux.liferay.themeswitcher.meta.ThemeSwitcherPortletKeys;
import com.savoirfairelinux.liferay.themeswitcher.service.configuration.ThemeSwitcherConfiguration;

/**
 * This portlet is used to change the theme switcher's behaviour on different sites
 */
@Component(
    service = Portlet.class,
    immediate = true,
    property = {
        // liferay-portlet.xml
        "com.liferay.portlet.control-panel-entry-category=",
        "com.liferay.portlet.instanceable=false",
        "com.liferay.portlet.requires-namespaced-parameters=true",
        "com.liferay.portlet.display-category=category.hidden",
        // portlet.xml
        "javax.portlet.name=" + ThemeSwitcherPortletKeys.PORTLET_NAME,
        "javax.portlet.display-name=Theme switcher configuration",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.supports.mime-type=text/html",
        "javax.portlet.security-role-ref=administrator,power-user,user",
        "javax.portlet.expiration-cache=0"
    }
)
public class ThemeSwitcherPortlet extends GenericPortlet {

    private static final String VIEW = "/WEB-INF/jsp/view.jsp";

    private static final String REQUEST_ATTR_ROLES = "roles";
    private static final String REQUEST_ATTR_THEMES = "themes";

    private static final String REQUEST_PARAM_REDIRECT = "redirect";
    private static final String REQUEST_PARAM_ENABLED = "enabled";
    private static final String REQUEST_PARAM_SELECTED_ROLES = "selectedRoles";
    private static final String REQUEST_PARAM_SELECTED_THEME = "selectedTheme";

    private static final Pattern PATTERN_UUID = Pattern.compile("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
    private static final Pattern PATTERN_THEME_ID = Pattern.compile("^[a-zA-Z0-9\\-\\_]+_WAR_[a-zA-Z0-9\\-\\_]+$");

    private static final String SESSION_ERROR_ROLE_ID_INVALID = "role.id.invalid";
    private static final String SESSION_ERROR_THEME_ID_INVALID = "theme.id.invalid";

    private static final String SESSION_MESSAGE_SAVE_SUCCESSFUL = "save.successful";

    private static final Log LOG = LogFactoryUtil.getLog(ThemeSwitcherPortlet.class);

    private Portal portal;
    private RoleLocalService roleService;
    private ThemeLocalService themeService;

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        ThemeDisplay themeDisplay = this.getThemeDisplay(request);
        long companyId = themeDisplay.getCompanyId();
        String groupUuid = themeDisplay.getScopeGroup().getUuid();

        PortletPreferences prefs = request.getPreferences();
        String prefKeyEnabled = ThemeSwitcherConfiguration.getKeyEnabled(groupUuid);
        String prefKeyThemeId = ThemeSwitcherConfiguration.getKeyThemeId(groupUuid);
        String prefKeyRoleUuid = ThemeSwitcherConfiguration.getKeyRoleUuid(groupUuid);

        List<Theme> themes = this.themeService.getThemes(companyId);
        List<Role> roles = this.roleService.getRoles(companyId);

        boolean failures = false;

        // Save whether the switcher is enabled or not
        boolean enabled = ParamUtil.getBoolean(request, REQUEST_PARAM_ENABLED, false);
        prefs.setValue(prefKeyEnabled, Boolean.toString(enabled));

        // Save the theme
        String themeId = ParamUtil.get(request, REQUEST_PARAM_SELECTED_THEME, ThemeSwitcherConfiguration.DEFAULT_THEME_ID);
        if(this.isThemeIdValid(themes, themeId)) {
            prefs.setValue(prefKeyThemeId, themeId);
        } else {
            SessionErrors.add(request, SESSION_ERROR_THEME_ID_INVALID);
            failures = true;
        }

        // Save the roles
        String[] selectedRoles = ParamUtil.getStringValues(request, REQUEST_PARAM_SELECTED_ROLES, ThemeSwitcherConfiguration.DEFAULT_ROLE_UUIDS);
        if(this.areRolesValid(companyId, roles, selectedRoles)) {
            prefs.setValues(prefKeyRoleUuid, selectedRoles);
        } else {
            SessionErrors.add(request, SESSION_ERROR_ROLE_ID_INVALID);
            failures = true;
        }

        // Notify the user of failures
        if(!failures) {
            SessionMessages.add(request, SESSION_MESSAGE_SAVE_SUCCESSFUL);
        }

        // Store preferences and redirect if needed
        if(!failures) {
            prefs.store();
            response.sendRedirect(request.getParameter(REQUEST_PARAM_REDIRECT));
        } else {
            this.portal.copyRequestParameters(request, response);
        }
    }

    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        ThemeDisplay themeDisplay = this.getThemeDisplay(request);
        PermissionChecker permissionChecker = themeDisplay.getPermissionChecker();
        PortletPreferences preferences = request.getPreferences();
        long companyId = themeDisplay.getCompanyId();
        String groupUuid = themeDisplay.getScopeGroup().getUuid();

        // Get the roles
        String roleResourceName = Role.class.getName();
        List<Role> roles = this.roleService.getRoles(companyId).stream().filter(
            role -> permissionChecker.hasPermission(0l, roleResourceName, role.getPrimaryKey(), ActionKeys.VIEW)
        ).collect(toList());

        // Get the themes
        List<Theme> themes = this.themeService.getThemes(companyId);

        // Get the preferences, if any
        String prefKeyEnabled = ThemeSwitcherConfiguration.getKeyEnabled(groupUuid);
        String prefKeyThemeId = ThemeSwitcherConfiguration.getKeyThemeId(groupUuid);
        String prefKeyRoleUuid = ThemeSwitcherConfiguration.getKeyRoleUuid(groupUuid);

        String enabled = preferences.getValue(prefKeyEnabled, ThemeSwitcherConfiguration.DEFAULT_ENABLED);
        String selectedTheme = preferences.getValue(prefKeyThemeId, StringPool.BLANK);
        List<String> selectedRoles = Arrays.asList(preferences.getValues(prefKeyRoleUuid, new String[0]));

        request.setAttribute(REQUEST_PARAM_ENABLED, enabled);
        request.setAttribute(REQUEST_ATTR_ROLES, roles);
        request.setAttribute(REQUEST_ATTR_THEMES, themes);

        request.setAttribute(REQUEST_PARAM_SELECTED_ROLES, selectedRoles);
        request.setAttribute(REQUEST_PARAM_SELECTED_THEME, selectedTheme);

        this.getPortletContext().getRequestDispatcher(VIEW).include(request, response);
    }

    /**
     * Indicates if the given theme ID corresponds to a Liferay theme
     *
     * @param themes The Liferay themes
     * @param themeId The theme ID (format will be validated in this method)
     * @return True if the given theme ID corresponds to a Liferay theme
     */
    private boolean isThemeIdValid(List<Theme> themes, String themeId) {
        boolean valid = false;
        if(PATTERN_THEME_ID.matcher(themeId).matches()) {
            if(themes.stream().filter(theme -> theme.getThemeId().equals(themeId)).findFirst().orElse(null) != null) {
                valid = true;
            } else {
                LOG.error("Invalid theme " + themeId);
            }
        }
        return valid;
    }

    /**
     * Indicates if the given role UUIDs correspond to Liferay roles
     *
     * @param companyId The current instance ID
     * @param roles The Liferay roles
     * @param roleUuids The role UUIDs that must be validated
     * @return True if the given role UUIDs correspond to Liferay roles
     */
    private boolean areRolesValid(long companyId, List<Role> roles, String[] roleUuids) {
        boolean valid = true;

        for(String roleUuid : roleUuids) {
            // Fail if a role doesn't work out
            if(!PATTERN_UUID.matcher(roleUuid).matches() || this.roleService.fetchRoleByUuidAndCompanyId(roleUuid, companyId) == null) {
                valid = false;
                LOG.error("Invalid role " + roleUuid);
            }
        }

        return valid;
    }

    /**
     * Returns the theme display object contained in the request
     *
     * @param request The request
     * @return The theme display object contained in the request
     */
    private ThemeDisplay getThemeDisplay(PortletRequest request) {
        return (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
    }

    /******************************************************************************************************************/
    /*                                              SERVICE REFERENCES                                                */
    /******************************************************************************************************************/

    @Reference
    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    @Reference
    public void setRoleService(RoleLocalService roleService) {
        this.roleService = roleService;
    }

    @Reference
    public void setThemeService(ThemeLocalService themeService) {
        this.themeService = themeService;
    }

}

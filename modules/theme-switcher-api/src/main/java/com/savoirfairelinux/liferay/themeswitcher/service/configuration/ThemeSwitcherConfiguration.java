package com.savoirfairelinux.liferay.themeswitcher.service.configuration;

import static java.lang.String.format;

import com.liferay.portal.kernel.util.StringPool;

/**
 * Helper class for the Theme Switcher configuration
 */
public class ThemeSwitcherConfiguration {

    public static final String DEFAULT_ENABLED = "false";
    public static final String[] DEFAULT_ROLE_UUIDS = new String[0];
    public static final String DEFAULT_THEME_ID = StringPool.BLANK;

    private static final String FORMAT_KEY_ENABLED = "enabled-%s";
    private static final String FORMAT_KEY_THEME_ID = "themeId-%s";
    private static final String FORMAT_KEY_ROLE_UUID = "roleUuid-%s";

    /**
     * @param groupUuid The site UUID
     * @return the configuration key corresponding to the given site UUID
     */
    public static String getKeyEnabled(String groupUuid) {
        return format(FORMAT_KEY_ENABLED, groupUuid);
    }

    /**
     * @param groupUuid The site UUID
     * @return The configuration key corresponding to the given site UUID
     */
    public static String getKeyThemeId(String groupUuid) {
        return format(FORMAT_KEY_THEME_ID, groupUuid);
    }

    /**
     * @param groupUuid The site UUID
     * @return The configuration key corresponding to the given site UUID
     */
    public static String getKeyRoleUuid(String groupUuid) {
        return format(FORMAT_KEY_ROLE_UUID, groupUuid);
    }


}

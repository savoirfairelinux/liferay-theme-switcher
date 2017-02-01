<%@ include file="init.jsp" %>

<portlet:renderURL var="redirect" />
<portlet:actionURL var="actionUrl" />

<liferay-ui:error key="role.id.invalid" message="session.error.role.id.invalid" />
<liferay-ui:error key="theme.id.invalid" message="session.error.theme.id.invalid" />
<liferay-ui:success key="save.successful" message="session.message.save.successful" />

<aui:form cssClass="container-fluid-1280" action="${actionUrl}" method="POST">
    <aui:input type="hidden" name="redirect" value="${redirect}" />

    <p><liferay-ui:message key="instructions.enable" /></p>
    <%-- This is a workaround for aui:input checkbox which always put the "checked" attribute --%>
    <label for="${ns}enabled"><liferay-ui:message key="label.enabled" />
        <c:choose>
            <c:when test="${enabled eq 'true'}">
                <input class="field" type="checkbox" id="${ns}enabled" name="${ns}enabled" value="true" checked="checked" />
            </c:when>
            <c:otherwise>
                <input class="field" type="checkbox" id="${ns}enabled" name="${ns}enabled" value="true" />
            </c:otherwise>
        </c:choose>
    </label>

    <p><liferay-ui:message key="instructions.roles" /></p>
    <aui:select name="selectedRoles" multiple="true" required="false" label="label.roles">
        <c:forEach items="${roles}" var="role">
            <aui:option value="${role.uuid}" label="${role.name}" selected="${selectedRoles.contains(role.uuid)}" />
        </c:forEach>
    </aui:select>

    <p><liferay-ui:message key="instructions.themes" /></p>
    <aui:select name="selectedTheme" multiple="false" required="true" label="label.theme">
        <c:forEach items="${themes}" var="theme">
            <aui:option value="${theme.themeId}" label="${theme.name}" selected="${theme.themeId eq selectedTheme}" />
        </c:forEach>
    </aui:select>

    <aui:button type="submit" label="button.save" />
</aui:form>
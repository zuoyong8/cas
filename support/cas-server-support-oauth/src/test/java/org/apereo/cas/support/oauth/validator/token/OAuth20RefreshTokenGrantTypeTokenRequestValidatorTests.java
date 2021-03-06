package org.apereo.cas.support.oauth.validator.token;

import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.services.RegisteredServiceAccessStrategyAuditableEnforcer;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.authenticator.Authenticators;
import org.apereo.cas.ticket.refreshtoken.RefreshToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.CollectionUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link OAuth20RefreshTokenGrantTypeTokenRequestValidatorTests}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
public class OAuth20RefreshTokenGrantTypeTokenRequestValidatorTests {
    private static final String SUPPORTING_SERVICE_TICKET = "RT-SUPPORTING";
    private static final String NON_SUPPORTING_SERVICE_TICKET = "RT-NON-SUPPORTING";
    private static final String PROMISCUOUS_SERVICE_TICKET = "RT-PROMISCUOUS";

    private TicketRegistry ticketRegistry;
    private OAuth20TokenRequestValidator validator;

    private void registerTicket(final String name) {
        val oauthCode = mock(RefreshToken.class);
        when(oauthCode.getId()).thenReturn(name);
        when(oauthCode.isExpired()).thenReturn(false);
        when(oauthCode.getAuthentication()).thenReturn(RegisteredServiceTestUtils.getAuthentication());
        when(ticketRegistry.getTicket(eq(name))).thenReturn(oauthCode);
    }

    @BeforeEach
    public void before() {
        val servicesManager = mock(ServicesManager.class);

        val supportingService = RequestValidatorTestUtils.getService(
                RegisteredServiceTestUtils.CONST_TEST_URL,
                RequestValidatorTestUtils.SUPPORTING_CLIENT_ID,
                RequestValidatorTestUtils.SUPPORTING_CLIENT_ID,
                RequestValidatorTestUtils.SHARED_SECRET,
                CollectionUtils.wrapSet(OAuth20GrantTypes.REFRESH_TOKEN));
        val nonSupportingService = RequestValidatorTestUtils.getService(
                RegisteredServiceTestUtils.CONST_TEST_URL2,
                RequestValidatorTestUtils.NON_SUPPORTING_CLIENT_ID,
                RequestValidatorTestUtils.NON_SUPPORTING_CLIENT_ID,
                RequestValidatorTestUtils.SHARED_SECRET,
                CollectionUtils.wrapSet(OAuth20GrantTypes.PASSWORD));
        val promiscuousService = RequestValidatorTestUtils.getPromiscuousService(
                RegisteredServiceTestUtils.CONST_TEST_URL3,
                RequestValidatorTestUtils.PROMISCUOUS_CLIENT_ID,
                RequestValidatorTestUtils.PROMISCUOUS_CLIENT_ID,
                RequestValidatorTestUtils.SHARED_SECRET);
        when(servicesManager.getAllServices()).thenReturn(CollectionUtils.wrapList(supportingService,
                nonSupportingService, promiscuousService));

        this.ticketRegistry = mock(TicketRegistry.class);

        registerTicket(SUPPORTING_SERVICE_TICKET);
        registerTicket(NON_SUPPORTING_SERVICE_TICKET);
        registerTicket(PROMISCUOUS_SERVICE_TICKET);

        this.validator = new OAuth20RefreshTokenGrantTypeTokenRequestValidator(
            new RegisteredServiceAccessStrategyAuditableEnforcer(), servicesManager,
            this.ticketRegistry, new WebApplicationServiceFactory());
    }

    @Test
    public void verifyOperation() {
        val request = new MockHttpServletRequest();

        val profile = new CommonProfile();
        profile.setClientName(Authenticators.CAS_OAUTH_CLIENT_BASIC_AUTHN);
        profile.setId(RequestValidatorTestUtils.SUPPORTING_CLIENT_ID);
        val session = request.getSession(true);
        session.setAttribute(Pac4jConstants.USER_PROFILES, profile);

        val response = new MockHttpServletResponse();
        request.setParameter(OAuth20Constants.GRANT_TYPE, OAuth20GrantTypes.REFRESH_TOKEN.getType());
        request.setParameter(OAuth20Constants.CLIENT_ID, RequestValidatorTestUtils.SUPPORTING_CLIENT_ID);
        request.setParameter(OAuth20Constants.CLIENT_SECRET, RequestValidatorTestUtils.SHARED_SECRET);
        request.setParameter(OAuth20Constants.REFRESH_TOKEN, SUPPORTING_SERVICE_TICKET);

        assertTrue(this.validator.validate(new J2EContext(request, response)));

        profile.setId(RequestValidatorTestUtils.NON_SUPPORTING_CLIENT_ID);
        session.setAttribute(Pac4jConstants.USER_PROFILES, profile);
        request.setParameter(OAuth20Constants.CLIENT_ID, RequestValidatorTestUtils.NON_SUPPORTING_CLIENT_ID);
        request.setParameter(OAuth20Constants.CLIENT_SECRET, RequestValidatorTestUtils.SHARED_SECRET);
        request.setParameter(OAuth20Constants.REFRESH_TOKEN, NON_SUPPORTING_SERVICE_TICKET);
        assertFalse(this.validator.validate(new J2EContext(request, response)));

        profile.setId(RequestValidatorTestUtils.PROMISCUOUS_CLIENT_ID);
        session.setAttribute(Pac4jConstants.USER_PROFILES, profile);
        request.setParameter(OAuth20Constants.CLIENT_ID, RequestValidatorTestUtils.PROMISCUOUS_CLIENT_ID);
        request.setParameter(OAuth20Constants.CLIENT_SECRET, RequestValidatorTestUtils.SHARED_SECRET);
        request.setParameter(OAuth20Constants.REFRESH_TOKEN, PROMISCUOUS_SERVICE_TICKET);
        assertTrue(this.validator.validate(new J2EContext(request, response)));
    }
}

/*
 * Copyright © 2017 ForgeRock, AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions Copyrighted 2017 Charan Mann
 *
 * openig-uma-ext: Created by Charan Mann on 10/7/16 , 12:55 PM.
 */

package org.forgerock.openig.uma;

import org.forgerock.http.Handler;
import org.forgerock.http.MutableUri;
import org.forgerock.http.oauth2.OAuth2;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Responses;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.openig.heap.GenericHeaplet;
import org.forgerock.openig.heap.HeapException;
import org.forgerock.openig.http.EndpointRegistry;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValueFunctions.uri;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.json.resource.http.CrestHttp.newHttpHandler;
import static org.forgerock.openig.util.CrestUtil.newCrestApplication;
import static org.forgerock.openig.util.JsonValues.evaluated;
import static org.forgerock.openig.util.JsonValues.requiredHeapObject;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

/**
 * An {@link UmaSharingService} provides core UMA features to OpenIG when acting as an UMA Resource Server.
 * <p>
 * <p>It is linked to a single UMA Authorization Server and needs to be pre-registered as an OAuth 2.0 client on that
 * AS.
 * <p>
 * <p>It is also the place where protected application knowledge is described: each item of the {@code resources}
 * array describe a resource set (that can be composed of multiple endpoints) that share the same set of scopes.
 * <p>
 * <p>Each resource contains a {@code pattern} used to define which one of them to use when a {@link Share} is
 * {@linkplain #createShare(Context, CreateRequest, String) created}. A resource also contains a list of {@code actions} that
 * defines the set of scopes to require when a requesting party request comes in.
 * <p>
 * <pre>
 *     {@code {
 *           "name": "UmaServiceExt",
 *           "type": "UmaServiceExt",
 *           "config": {
 *               "protectionApiHandler": "ClientHandler",
 *               "authorizationServerUri": "http://openam135.sample.com:8080/openam/",
 *               "realm": "/employees",
 *               "clientId": "OpenIG_RS",
 *               "clientSecret": "password",
 *               "ldapHost": "192.168.56.122",
 *               "ldapPort": 3389,
 *               "ldapAdminId": "cn=Directory Manager",
 *               "ldapAdminPassword": "cangetindj",
 *               "ldapBaseDN": "dc=openig,dc=forgerock,dc=org"
 *       }
 *     }
 * </pre>
 * <p>
 * Along with the {@code UmaService}, a REST endpoint is deployed in OpenIG's API namespace:
 * {@literal /openig/api/system/objects/../objects/[name-of-the-uma-service-object]/share}.
 * The dotted segment depends on your deployment (like which RouterHandler hosts the route that
 * in turns contains this object).
 */
public class UmaSharingServiceExt {

    private final Handler protectionApiHandler;
    private final URI authorizationServer;
    private final URI introspectionEndpoint;
    private final URI ticketEndpoint;
    private final URI resourceSetEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String realm;
    private LDAPManager ldapManager;


    /**
     * Constructs an UmaSharingService bound to the given {@code authorizationServer} and dedicated to protect resource
     * sets described by the given {@code templates}.
     *
     * @param protectionApiHandler used to call the resource set endpoint
     * @param authorizationServer  Bound UMA Authorization Server
     * @param clientId             OAuth 2.0 Client identifier
     * @param clientSecret         OAuth 2.0 Client secret
     * @throws URISyntaxException when the authorization server URI cannot be "normalized" (trailing '/' append if required)
     */
    public UmaSharingServiceExt(final Handler protectionApiHandler,
                                String realm,
                                final URI authorizationServer,
                                final String clientId,
                                final String clientSecret,
                                final LDAPManager ldapManager)
            throws URISyntaxException {
        this.protectionApiHandler = protectionApiHandler;
        this.authorizationServer = appendTrailingSlash(authorizationServer);

        this.realm = realm;
        this.introspectionEndpoint = authorizationServer.resolve("oauth2" + realm + "/introspect");
        this.ticketEndpoint = authorizationServer.resolve("uma" + realm + "/permission_request");
        this.resourceSetEndpoint = authorizationServer.resolve("oauth2" + realm + "/resource_set");
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.ldapManager = ldapManager;
    }

    /**
     * Append a trailing {@literal /} if missing.
     *
     * @param uri URI to be "normalized"
     * @return a URI with a trailing {@literal /}
     * @throws URISyntaxException should never happen
     */
    private static URI appendTrailingSlash(final URI uri) throws URISyntaxException {
        if (!uri.getPath().endsWith("/")) {
            MutableUri mutable = new MutableUri(uri);
            mutable.setRawPath(uri.getRawPath().concat("/"));
            return mutable.asURI();
        }
        return uri;
    }

    /**
     * Creates a Share that will be used to protect the given {@code resourcePath}.
     *
     * @param context       Context chain used to keep a relationship between requests (tracking)
     * @param createRequest CreateRequest
     * @return the created {@link Share} asynchronously
     * @see <a href="https://docs.kantarainitiative.org/uma/draft-oauth-resource-reg.html#rfc.section.2">Resource Set
     * Registration</a>
     */
    public Promise<ShareExt, UmaException> createShare(final Context context,
                                                       final CreateRequest createRequest, final String userId) {
        final String uri = createRequest.getContent().get("uri").asString();
        final String name = createRequest.getContent().get("name").asString();
        String type = createRequest.getContent().get("type").asString();
        List<Object> scopes = createRequest.getContent().get("scopes").asList();

        final String pat = OAuth2.getBearerAccessToken(((HttpContext) context.getParent()).getHeaderAsString("Authorization"));

        if (isShared(name, uri, userId)) {
            // We do not accept re-sharing or post-creation resource_set configuration
            return newExceptionPromise(new UmaException(format("Share already exists with similar name: %s or uri: %s ", name, uri)));
        }

        return createResourceSet(context, pat, resourceSet(name, scopes, type))
                .then(new Function<Response, ShareExt, UmaException>() {
                    @Override
                    public ShareExt apply(final Response response) throws UmaException {
                        if (response.getStatus() == Status.CREATED) {
                            try {
                                JsonValue value = json(response.getEntity().getJson());
                                ShareExt share = new ShareExt(value.get("_id").asString(), name, pat, uri, value.get("user_access_policy_uri").asString(), userId, realm, clientId);
                                ldapManager.addShare(share);
                                return share;
                            } catch (IOException e) {
                                throw new UmaException("Cannot register resource_set in OpenIG LDAP", e);
                            }
                        }
                        throw new UmaException("Cannot register resource_set in AS: " + response.getEntity());
                    }
                }, Responses.<ShareExt, UmaException>noopExceptionFunction());
    }

    /**
     * Check the share already exists with same share name or URI for a given user / realm / OAuth Client
     *
     * @param name
     * @param uri
     * @param userId
     * @return true if matching share exists; false otherwise
     */
    private boolean isShared(String name, String uri, String userId) {
        try {
            // Pass 1
            ShareExt matchingShareExt = new ShareExt(name, null, userId, realm, clientId);
            if (ldapManager.getShare(matchingShareExt).size() != 0) {
                return true;
            }

            // Pass 2
            matchingShareExt = new ShareExt(null, uri, userId, realm, clientId);
            if (ldapManager.getShare(matchingShareExt).size() != 0) {
                return true;
            }

            //TODO Better option is to optimize LDAP filter for above search
        } catch (LdapException e) {
            return false;
        }
        return false;
    }

    private Promise<Response, NeverThrowsException> createResourceSet(final Context context,
                                                                      final String pat,
                                                                      final JsonValue data) {
        Request request = new Request();
        request.setMethod("POST");
        request.setUri(resourceSetEndpoint);
        request.getHeaders().put("Authorization", format("Bearer %s", pat));
        request.getHeaders().put("Accept", "application/json");

        request.setEntity(data.asMap());

        return protectionApiHandler.handle(context, request);
    }

    private JsonValue resourceSet(final String name, final List<Object> scopes, final String type) {
        return json(object(field("name", name),
                field("scopes", scopes), field("type", type)));
    }

    /**
     * Find a {@link ShareExt}.
     *
     * @param request the incoming requesting party request
     * @return a {@link ShareExt} to be used to protect the resource access
     * @throws UmaException when no {@link ShareExt} can handle the request.
     */
    public ShareExt findShare(Request request) throws UmaException {

        // Need to find which Share to use
        String requestPath = request.getUri().getPath();
        String userId = request.getForm().getFirst("userId");

        ShareExt matchShareExt = new ShareExt(null, requestPath, userId, realm, clientId);

        try {
            Set<ShareExt> shares = ldapManager.getShare(matchShareExt);
            if (shares.size() != 0) {
                return shares.iterator().next();
            }
        } catch (LdapException e) {
            throw new UmaException(format("Can't find any shared resource for %s", requestPath));
        }
        throw new UmaException(format("Can't find any shared resource for %s", requestPath));
    }

    /**
     * Removes the previously created Share from the registered shares. In effect, the resources is no more
     * shared/protected
     *
     * @param shareId share identifier
     * @return the removed Share instance if found, {@code null} otherwise.
     */
    public ShareExt removeShare(String shareId, final String userId) {

        try {
            ShareExt shareExt = getShare(shareId, userId);

            if (null != shareExt) {
                ldapManager.removeShare(shareId);
            }
            return shareExt;
        } catch (LdapException e) {
            return null;
        }

    }

    /**
     * Returns the {@link ShareExt} with the given {@code id}.
     *
     * @param shareId Share identifier
     * @return the {@link ShareExt} with the given {@code id} (or {@code null} if none was found).
     */
    public ShareExt getShare(final String shareId, final String userId) {
        ShareExt matchShareExt = new ShareExt(null, null, userId, realm, clientId);
        matchShareExt.setId(shareId);

        try {
            Set<ShareExt> shares = ldapManager.getShare(matchShareExt);
            if (shares.size() != 0) {
                return shares.iterator().next();
            }
        } catch (LdapException e) {
            return null;
        }
        return null;
    }

    /**
     * Returns a copy of the list of currently managed shares.
     *
     * @return a copy of the list of currently managed shares.
     */
    public Set<ShareExt> listShares(String userId) {

        ShareExt matchShareExt = new ShareExt(null, null, userId, realm, clientId);

        try {
            return ldapManager.getShare(matchShareExt);
        } catch (LdapException e) {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * Returns the UMA authorization server base Uri.
     *
     * @return the UMA authorization server base Uri.
     */
    public URI getAuthorizationServer() {
        return authorizationServer;
    }

    /**
     * Returns the UMA Permission Request endpoint Uri.
     *
     * @return the UMA Permission Request endpoint Uri.
     */
    public URI getTicketEndpoint() {
        return ticketEndpoint;
    }

    /**
     * Returns the OAuth 2.0 Introspection endpoint Uri.
     *
     * @return the OAuth 2.0 Introspection endpoint Uri.
     */
    public URI getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    /**
     * Returns the client identifier used to identify this RS as an OAuth 2.0 client.
     *
     * @return the client identifier used to identify this RS as an OAuth 2.0 client.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the client secret.
     *
     * @return the client secret.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    public Handler getProtectionApiHandler() {
        return protectionApiHandler;
    }


    /**
     * Creates and initializes an UMA service in a heap environment.
     */
    public static class Heaplet extends GenericHeaplet {

        private static final Logger logger = LoggerFactory.getLogger(UmaSharingServiceExt.Heaplet.class);

        private static String startsWithSlash(final String realm) {
            String nonNullRealm = realm != null ? realm : "/";
            return nonNullRealm.startsWith("/") ? nonNullRealm : "/" + nonNullRealm;
        }

        @Override
        public Object create() throws HeapException {
            Handler handler = config.get("protectionApiHandler").required().as(requiredHeapObject(heap, Handler.class));
            URI uri = config.get("authorizationServerUri").as(evaluated()).required().as(uri());
            String realm = startsWithSlash(config.get("realm").defaultTo("/").asString());
            String clientId = config.get("clientId").as(evaluated()).required().asString();
            String clientSecret = config.get("clientSecret").as(evaluated()).required().asString();

            //LDAP configs
            String ldapHost = config.get("ldapHost").as(evaluated()).defaultTo("localhost").asString();
            Integer ldapPort = config.get("ldapPort").as(evaluated()).defaultTo(1389).asInteger();
            String ldapAdminId = config.get("ldapAdminId").as(evaluated()).defaultTo("cn=Directory Manager").asString();
            String ldapAdminPassword = config.get("ldapAdminPassword").as(evaluated()).required().asString();
            String ldapBaseDN = config.get("ldapBaseDN").as(evaluated()).defaultTo("dc=openig,dc=forgerock,dc=org").asString();

            try {
                LDAPManager ldapManager = new LDAPManager(ldapHost, ldapPort, ldapAdminId, ldapAdminPassword, ldapBaseDN);
                UmaSharingServiceExt service = new UmaSharingServiceExt(handler, realm,
                        uri,
                        clientId,
                        clientSecret,
                        ldapManager);
                // register admin endpoint
                Handler httpHandler = newHttpHandler(
                        newCrestApplication(newHandler(new ShareCollectionProviderExt(service)),
                                "frapi:openig:uma:share"));
                EndpointRegistry.Registration share = endpointRegistry().register("share", httpHandler);
                logger.info("UMA Share endpoint available at '{}'", share.getPath());

                return service;
            } catch (URISyntaxException e) {
                throw new HeapException("Cannot build UmaSharingService", e);
            }
        }

    }

}


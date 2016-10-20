/*
 * Copyright © 2016 ForgeRock, AS.
 *
 * This is unsupported code made available by ForgeRock for community development subject to the license detailed below.
 * The code is provided on an "as is" basis, without warranty of any kind, to the fullest extent permitted by law.
 *
 * ForgeRock does not warrant or guarantee the individual success developers may have in implementing the code on their
 * development platforms or in production configurations.
 *
 * ForgeRock does not warrant, guarantee or make any representations regarding the use, results of use, accuracy, timeliness
 * or completeness of any data or information relating to the alpha release of unsupported code. ForgeRock disclaims all
 * warranties, expressed or implied, and in particular, disclaims all warranties of merchantability, and warranties related
 * to the code, or any service or software related thereto.
 *
 * ForgeRock shall not be liable for any direct, indirect or consequential damages or costs of any type arising out of any
 * action taken by you or others related to the code.
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution License (the License).
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License at https://forgerock.org/cddlv1-0/. See the License for the specific language governing
 * permission and limitations under the License.
 *
 * Portions Copyrighted 2016 Charan Mann
 *
 * openig-uma-ext: Created by Charan Mann on 10/13/16 , 9:44 AM.
 */

package org.forgerock.openig.uma;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.header.Warning;
import org.forgerock.http.header.WarningHeader;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openig.heap.GenericHeapObject;
import org.forgerock.openig.heap.GenericHeaplet;
import org.forgerock.openig.heap.HeapException;
import org.forgerock.openig.oauth2.OAuth2;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.forgerock.http.header.WarningHeader.MISCELLANEOUS_WARNING;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openig.util.JsonValues.evaluated;
import static org.forgerock.openig.util.JsonValues.requiredHeapObject;
import static org.forgerock.util.Utils.closeSilently;

/**
 * An {@link UmaResourceServerFilter} implements a PEP (Policy Enforcement Point) and is responsible to ensure the
 * incoming requests (from requesting parties) all have a valid RPT (Request Party Token) with the required set of
 * scopes.
 * <p>
 * <pre>
 *     {@code {
 *           "type": "UmaFilter",
 *           "config": {
 *           "protectionApiHandler": "ClientHandler",
 *           "umaService": "UmaServiceExt",
 *           "scopes" : [
 *               "http://login.example.com/scopes/view"
 *           ]
 *           }
 *       }
 *     }
 * </pre>
 */
public class UmaResourceServerFilterExt extends GenericHeapObject implements Filter {

    private final UmaSharingServiceExt umaService;
    private final Handler protectionApiHandler;
    private final String realm;
    private final Set<Object> scopes;

    /**
     * Constructs a new UmaResourceServerFilter.
     *
     * @param umaService           core service to use
     * @param protectionApiHandler protectionApiHandler to use when interacting with introspection and permission request endpoints
     * @param realm                UMA realm name (can be {@code null})
     */
    public UmaResourceServerFilterExt(final UmaSharingServiceExt umaService,
                                      final Handler protectionApiHandler,
                                      final String realm, final Set<Object> scopes) {
        this.umaService = umaService;
        this.protectionApiHandler = protectionApiHandler;
        this.realm = realm;
        this.scopes = scopes;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(final Context context,
                                                          final Request request,
                                                          final Handler next) {

        try {
            // Find a Share for this request
            final ShareExt share = umaService.findShare(request);
            String rpt = OAuth2.getBearerAccessToken(request.getHeaders().getFirst("Authorization"));

            // Is there an RPT ?
            if (rpt != null) {
                // Validate the token
                return introspectToken(context, rpt, share.getPAT())
                        .thenAsync(new VerifyScopesAsyncFunction(share, context, request, next));
            }

            // Error case: ask for a ticket
            return ticket(context, share, request);

        } catch (UmaException e) {
            logger.error(e);
            // No share found
            // Make sure we return a 404
            return newResponsePromise(e.getResponse().setStatus(Status.NOT_FOUND));
        }
    }

    /**
     * Call the UMA Permission Registration Endpoint to register a requested permission with the authorization server.
     * <p>
     * <p>If the registration succeed, the obtained opaque ticket is returned to the client with an additional {@literal
     * WWW-Authenticate} header:
     * <p>
     * <pre>
     *     {@code HTTP/1.1 401 Unauthorized
     *       WWW-Authenticate: UMA realm="example",
     *                             as_uri="https://as.example.com",
     *                             ticket="016f84e8-f9b9-11e0-bd6f-0021cc6004de"
     *     }
     * </pre>
     * <p>
     * Otherwise, a {@literal 403 Forbidden} response with an informative {@literal Warning} header is produced.
     *
     * @param context  Context chain used to keep a relationship between requests (tracking)
     * @param share    represents protection information about the requested resource
     * @param incoming request used to infer the set of permissions to ask
     * @return an asynchronous {@link Response}
     * @see <a href="https://docs.kantarainitiative.org/uma/draft-uma-core-v1_0_1.html#rfc.section.3.2">Request
     * Permission Registration</a>
     */
    private Promise<Response, NeverThrowsException> ticket(final Context context,
                                                           final ShareExt share,
                                                           final Request incoming) {
        Request request = new Request();
        request.setMethod("POST");
        request.setUri(umaService.getTicketEndpoint());
        request.getHeaders().put("Authorization", format("Bearer %s", share.getPAT()));
        request.getHeaders().put("Accept", "application/json");
        request.setEntity(createPermissionRequest(share, incoming).asMap());

        return protectionApiHandler.handle(context, request)
                .then(new TicketResponseFunction());
    }

    /**
     * Builds the resource set registration {@link Request}'s JSON content.
     *
     * @param share   represents protection information about the requested resource
     * @param request request used to infer the set of permissions to ask
     * @return a JSON structure that represents a resource set registration
     * @see <a href="https://docs.kantarainitiative.org/uma/draft-oauth-resource-reg-v1_0_1.html#resource-set-desc">
     * Resource Set Descriptions</a>
     */
    private JsonValue createPermissionRequest(final ShareExt share, final Request request) {

        return json(object(field("resource_set_id", share.getResourceSetId()),
                field("scopes", array(scopes.toArray(new Object[scopes.size()])))));
    }

    private Promise<Response, NeverThrowsException> introspectToken(final Context context,
                                                                    final String token,
                                                                    final String pat) {
        Request request = new Request();
        request.setUri(umaService.getIntrospectionEndpoint());
        // Should accept a PAT as per the spec (See OPENAM-6320 / OPENAM-5928)
        //request.getHeaders().put("Authorization", format("Bearer %s", pat));
        request.getHeaders().put("Accept", "application/json");

        Form query = new Form();
        query.putSingle("token", token);
        query.putSingle("client_id", umaService.getClientId());
        query.putSingle("client_secret", umaService.getClientSecret());
        query.toRequestEntity(request);

        return protectionApiHandler.handle(context, request);
    }

    /**
     * Creates and initializes an UMA resource server filter in a heap environment.
     */
    public static class Heaplet extends GenericHeaplet {

        @Override
        public Object create() throws HeapException {
            UmaSharingServiceExt service = config.get("umaService")
                    .required()
                    .as(requiredHeapObject(heap, UmaSharingServiceExt.class));
            Handler handler = config.get("protectionApiHandler").required().as(requiredHeapObject(heap, Handler.class));
            String realm = config.get("realm").as(evaluated()).defaultTo("uma").asString();
            Set<Object> scopes = config.get("scopes").as(evaluated()).asSet();
            return new UmaResourceServerFilterExt(service, handler, realm, scopes);
        }
    }

    private class VerifyScopesAsyncFunction implements AsyncFunction<Response, Response, NeverThrowsException> {
        private final ShareExt share;
        private final Context context;
        private final Request request;
        private final Handler next;

        public VerifyScopesAsyncFunction(final ShareExt share,
                                         final Context context,
                                         final Request request,
                                         final Handler next) {
            this.share = share;
            this.context = context;
            this.request = request;
            this.next = next;
        }

        @Override
        public Promise<Response, NeverThrowsException> apply(final Response token) {

            if (Status.OK == token.getStatus()) {
                JsonValue value = null;
                try {
                    value = json(token.getEntity().getJson());
                } catch (IOException e) {
                    logger.debug("Cannot extract JSON from token introspection response, possibly malformed JSON");
                    logger.debug(e);
                    return newResponsePromise(newInternalServerError(e));
                }
                if (value.get("active").asBoolean()) {
                    // Got a valid token
                    // Need to verify embed scopes against required scopes

                    if (getScopes(value, share.getResourceSetId()).containsAll(scopes)) {
                        // All required scopes are present, continue the request processing
                        return next.handle(context, request);
                    }

                    logger.trace("Insufficient scopes encoded in RPT, asking for a new ticket");
                    // Not all of the required scopes are in the token
                    // Error case: ask for a ticket, append an error code
                    return ticket(context, share, request)
                            .thenOnResult(new ResultHandler<Response>() {
                                @Override
                                public void handleResult(final Response response) {

                                    // Update the Authorization header with a proper error code
                                    String authorization = response.getHeaders()
                                            .getFirst("WWW-Authenticate");
                                    if (authorization != null) {
                                        authorization = authorization.concat(", error=\"insufficient_scope\"");
                                        response.getHeaders().put("WWW-Authenticate", authorization);
                                    }
                                }
                            });
                }
            }

            // Error case: ask for a ticket
            return ticket(context, share, request);
        }

        private List<String> getScopes(final JsonValue value, final String resourceSetId) {
            for (JsonValue permission : value.get("permissions")) {
                if (resourceSetId.equals(permission.get("resource_set_id").asString())) {
                    return permission.get("scopes").asList(String.class);
                }
            }
            return Collections.emptyList();
        }
    }

    private class TicketResponseFunction implements Function<Response, Response, NeverThrowsException> {
        @Override
        public Response apply(final Response response) {
            try {
                if (Status.CREATED == response.getStatus()) {
                    // Create a new response with authenticate header and status code
                    try {
                        JsonValue value = json(response.getEntity().getJson());
                        Response forbidden = new Response(Status.UNAUTHORIZED);
                        String ticket = value.get("ticket").asString();
                        forbidden.getHeaders().put("WWW-Authenticate",
                                format("UMA realm=\"%s\", as_uri=\"%s\", ticket=\"%s\"",
                                        realm,
                                        umaService.getAuthorizationServer(),
                                        ticket));
                        return forbidden;
                    } catch (IOException e) {
                        // JSON parsing exception
                        // Do not process them here, handle them in the later catch-all block
                        logger.debug("Cannot extract JSON from ticket response, possibly malformed JSON");
                        logger.debug(e);
                    }
                } else {
                    logger.debug(format("Got a %s Response from '%s', was expecting a 201 Created.",
                            response.getStatus(),
                            umaService.getTicketEndpoint()));
                }

                // Properly handle 400 errors and UMA error codes
                // The PAT may need to be refreshed
                Response forbidden = new Response(Status.FORBIDDEN);
                forbidden.getHeaders().put(new WarningHeader(new Warning(MISCELLANEOUS_WARNING,
                        "-",
                        "UMA Authorization Server Unreachable")));
                return forbidden;
            } finally {
                // Close previous response object
                closeSilently(response);
            }
        }
    }
}
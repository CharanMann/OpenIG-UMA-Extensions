/*
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
 * Copyright 2016 Charan Mann
 * Portions Copyrighted 2016 ForgeRock AS
 *
 * openig-uma-ext: Created by Charan Mann on 10/12/16 , 10:51 AM.
 */

package org.forgerock.openig.uma;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;

import static java.lang.String.format;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.query.QueryFilter.alwaysTrue;

public class ShareCollectionProviderExt implements CollectionResourceProvider {

    private final UmaSharingServiceExt service;

    /**
     * Constructs a new CREST endpoint for managing {@linkplain Share shares}.
     *
     * @param service delegating service
     */
    public ShareCollectionProviderExt(final UmaSharingServiceExt service) {
        this.service = service;
    }

    private static JsonValue asJson(final ShareExt share) {
        return json(object(field("id", share.getId()),
                field("resourceURI", share.getRequestURI()),
                field("user_access_policy_uri", share.getPolicyURL()),
                field("pat", share.getPAT()),
                field("resource_set_id", share.getResourceSetId()),
                field("userId", share.getUserId()),
                field("realm", share.getRealm()),
                field("client_id", share.getClientId())));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
                                                                       final CreateRequest request) {
        if (request.getNewResourceId() != null) {
            return new NotSupportedException("Only POST-style of instance creation are supported").asPromise();
        }

        return service.createShare(context, request)
                .then(new Function<ShareExt, ResourceResponse, ResourceException>() {
                    @Override
                    public ResourceResponse apply(final ShareExt share) throws ResourceException {
                        return newResourceResponse(share.getId(), null, asJson(share));
                    }
                }, new Function<UmaException, ResourceResponse, ResourceException>() {
                    @Override
                    public ResourceResponse apply(final UmaException exception) throws ResourceException {
                        throw new BadRequestException("Failed to create a share", exception);
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context,
                                                                       final String resourceId,
                                                                       final DeleteRequest request) {
        ShareExt share = service.removeShare(resourceId);
        if (share == null) {
            return new NotFoundException(format("Share %s is unknown", resourceId)).asPromise();
        }
        return newResultPromise(newResourceResponse(resourceId, null, asJson(share)));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context,
                                                                      final String resourceId,
                                                                      final PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context,
                                                                     final QueryRequest request,
                                                                     final QueryResourceHandler handler) {

        // Reject queries with query ID, provided expressions and non "true" filter
        if (request.getQueryId() != null
                || request.getQueryExpression() != null
                || !alwaysTrue().equals(request.getQueryFilter())) {
            return new NotSupportedException("Only accept queries with filter=true").asPromise();
        }

        for (ShareExt share : service.listShares()) {
            handler.handleResource(newResourceResponse(share.getId(), null, asJson(share)));
        }

        return newResultPromise(newQueryResponse());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context,
                                                                     final String resourceId,
                                                                     final ReadRequest request) {
        ShareExt share = service.getShare(resourceId);
        return newResultPromise(newResourceResponse(resourceId, null, asJson(share)));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context,
                                                                       final String resourceId,
                                                                       final UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(final Context context,
                                                                       final ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context context,
                                                                     final String resourceId,
                                                                     final ActionRequest request) {
        return new NotSupportedException().asPromise();
    }
}

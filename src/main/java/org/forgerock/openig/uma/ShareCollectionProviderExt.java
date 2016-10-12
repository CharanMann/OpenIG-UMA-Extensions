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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;

public class ShareCollectionProviderExt extends ShareCollectionProvider {

    private final UmaSharingServiceExt service;

    /**
     * Constructs a new CREST endpoint for managing {@linkplain Share shares}.
     *
     * @param service delegating service
     */
    public ShareCollectionProviderExt(final UmaSharingServiceExt service) {
        super(service);
        this.service = service;
    }

    private static JsonValue asJson(final ShareExt share) {
        return json(object(field("id", share.getId()),
                field("resourceURI", share.getRequestURI()),
                field("user_access_policy_uri", share.getPolicyURL()),
                field("pat", share.getPat()),
                field("resource_set_id", share.getResourceSetId())));
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
}

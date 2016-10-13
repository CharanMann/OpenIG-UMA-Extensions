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
 * openig-uma-ext: Created by Charan Mann on 10/12/16 , 4:04 PM.
 */

package org.forgerock.openig.uma;

import java.util.UUID;

public class ShareExt {

    private String id;
    private String resourceSetId;
    private String PAT;
    private String requestURI;
    private String policyURL;
    private String refreshToken;
    private String realm;
    private String userId;
    private String clientId;

    /**
     * Creates ShareExt instance
     *
     * @param resourceSetId
     * @param PAT
     * @param requestURI
     * @param policyURL
     * @param realm
     * @param userId
     * @param clientId
     */
    public ShareExt(String resourceSetId, String PAT, String requestURI, String policyURL, String userId, String realm, String clientId) {
        this.id = UUID.randomUUID().toString();
        this.resourceSetId = resourceSetId;
        this.PAT = PAT;
        this.requestURI = requestURI;
        this.policyURL = policyURL;
        this.realm = realm;
        this.userId = userId;
        this.clientId = clientId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public String getPAT() {
        return PAT;
    }

    public void setPAT(String PAT) {
        this.PAT = PAT;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getPolicyURL() {
        return policyURL;
    }

    public void setPolicyURL(String policyURL) {
        this.policyURL = policyURL;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}

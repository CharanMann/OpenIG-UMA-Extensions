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
 * openig-uma-ext: Created by Charan Mann on 10/12/16 , 4:04 PM.
 */

package org.forgerock.openig.uma;

import java.util.UUID;

public class ShareExt {

    private String id;
    private String resourceSetId;
    private String resourceName;
    private String PAT;
    private String requestURI;
    private String policyURI;
    private String refreshToken;
    private String realm;
    private String userId;
    private String clientId;

    public ShareExt(String id) {
        if (null == id) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
    }

    /**
     * Creates ShareExt instance
     *
     * @param requestURI
     * @param realm
     * @param userId
     * @param realm
     * @param clientId
     */
    public ShareExt(String resourceName, String requestURI, String userId, String realm, String clientId) {
        this.resourceName = resourceName;
        this.requestURI = requestURI;
        this.realm = realm;
        this.userId = userId;
        this.clientId = clientId;
    }

    /**
     * Creates ShareExt instance
     *
     * @param resourceSetId
     * @param PAT
     * @param requestURI
     * @param policyURI
     * @param realm
     * @param userId
     * @param clientId
     */
    public ShareExt(String resourceSetId, String resourceName, String PAT, String requestURI, String policyURI, String userId, String realm, String clientId) {
        this.id = UUID.randomUUID().toString();
        this.resourceSetId = resourceSetId;
        this.resourceName = resourceName;
        this.PAT = PAT;
        this.requestURI = requestURI;
        this.policyURI = policyURI;
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

    public String getPolicyURI() {
        return policyURI;
    }

    public void setPolicyURI(String policyURI) {
        this.policyURI = policyURI;
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

}

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

public class ShareExt extends Share{

    private String id;
    private String resourceSetId;
    private String pat;
    private String requestURI;
    private String policyURL;
    private String refreshToken;

    public ShareExt(String resourceSetId, String pat, String requestURI, String policyURL) {
        super(null,null,null,null);
        this.id = UUID.randomUUID().toString();
        this.resourceSetId = resourceSetId;
        this.pat = pat;
        this.requestURI = requestURI;
        this.policyURL = policyURL;
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

    public String getPat() {
        return pat;
    }

    public void setPat(String pat) {
        this.pat = pat;
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

}

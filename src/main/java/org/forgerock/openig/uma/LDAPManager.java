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
 * openig-uma-ext: Created by Charan Mann on 10/12/16 , 2:21 PM.
 */

package org.forgerock.openig.uma;

import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.openig.ldap.LdapClient;
import org.forgerock.openig.ldap.LdapConnection;

public class LDAPManager {

    private LdapConnection ldapConnection;
    private LdapClient ldapClient;
    private String baseDN;

    public LDAPManager(String hostname, int port, String userName, String password, String baseDN) {
        try {
            this.ldapClient = LdapClient.getInstance();
            this.ldapConnection = ldapClient.connect(hostname, port);
            this.ldapConnection.bind(userName, password.toCharArray());
            this.baseDN = baseDN;
        } catch (Exception e) {
            e.printStackTrace();
            //TODO eating exception for now
        }
    }

    /**
     * Adds the UMA share in LDAP store
     *
     * @param share
     */
    void addShare(ShareExt share) throws LdapException {
        String entryDN = "umaResourceId=" + share.getId() + "," + baseDN;
        Entry entry = new LinkedHashMapEntry(entryDN)
                .addAttribute("objectclass", "top")
                .addAttribute("objectclass", "frUmaRS")
                .addAttribute("umaResourceSetId", share.getResourceSetId())
                .addAttribute("umaResourceURI", share.getRequestURI())
                .addAttribute("umaResourceName", share.getResourceName())
                .addAttribute("umaResoucePAT", share.getPAT())
                .addAttribute("umaResourcePolicyURI", share.getPolicyURI())
                .addAttribute("umaResourceUserID", share.getUserId())
                .addAttribute("umaResourceRealm", share.getRealm())
                .addAttribute("umaResourceClientId", share.getClientId());

        ldapConnection.add(entry);
    }

    /**
     * Gets the ShareExt matching the requestURI
     *
     * @param requestURI
     * @return ShareExt
     * @throws LdapException
     */
    ShareExt getShare(String requestURI, String userId, String realm, String clientId) throws LdapException {
        String filter = ldapClient.filter("(&(umaResourceURI=%s)(umaResourceUserID=%s)(umaResourceRealm=%s)(umaResourceClientId=%s))", requestURI, userId, realm, clientId);

        Entry resultEntry = ldapConnection.searchSingleEntry(baseDN, SearchScope.WHOLE_SUBTREE, filter);
        String id = resultEntry.getAttribute("umaResourceId").firstValueAsString();
        String rId = resultEntry.getAttribute("umaResourceSetId").firstValueAsString();
        String resourceName = resultEntry.getAttribute("umaResourceName").firstValueAsString();
        String pat = resultEntry.getAttribute("umaResoucePAT").firstValueAsString();
        String policyURI = resultEntry.getAttribute("umaResourcePolicyURI").firstValueAsString();
        userId = resultEntry.getAttribute("umaResourceUserID").firstValueAsString();
        realm = resultEntry.getAttribute("umaResourceRealm").firstValueAsString();
        clientId = resultEntry.getAttribute("umaResourceClientId").firstValueAsString();

        ShareExt share = new ShareExt(rId, resourceName, pat, requestURI, policyURI, userId, realm, clientId);
        share.setId(id);
        return share;
    }
}

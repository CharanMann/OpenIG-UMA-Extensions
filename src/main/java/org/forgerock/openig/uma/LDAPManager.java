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

import org.forgerock.opendj.ldap.*;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.openig.ldap.LdapClient;
import org.forgerock.openig.ldap.LdapConnection;

import java.util.HashSet;
import java.util.Set;

public class LDAPManager {

    private LdapClient ldapClient;
    private String baseDN;
    private String userName;
    private String password;
    private String hostname;
    private int port;

    public LDAPManager(String hostname, int port, String userName, String password, String baseDN) throws LdapException {
        this.userName = userName;
        this.password = password;
        this.hostname = hostname;
        this.port = port;
        this.baseDN = baseDN;

        this.ldapClient = LdapClient.getInstance();
    }

    /**
     * Adds the UMA share in LDAP store
     *
     * @param share
     */
    void addShare(ShareExt share) throws LdapException {
        LdapConnection ldapConnection = null;
        try {
            ldapConnection = ldapClient.connect(hostname, port);
            ldapConnection.bind(userName, password.toCharArray());

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
        } finally {
            if (null != ldapConnection) {
                ldapConnection.close();
            }
        }
    }

    /**
     * Gets the ShareExt matching the requestURI
     *
     * @return List of matching shares
     * @throws LdapException
     */
    Set<ShareExt> getShare(ShareExt matchingShareExt) throws LdapException {
        LdapConnection ldapConnection = null;
        try {
            ldapConnection = ldapClient.connect(hostname, port);
            String filter = constructSearchFilter(matchingShareExt);
            Set<ShareExt> shares = new HashSet<>();

            ConnectionEntryReader connectionEntryReader = ldapConnection.search(baseDN, SearchScope.WHOLE_SUBTREE, filter);
            while (connectionEntryReader.hasNext()) {
                SearchResultEntry resultEntry = connectionEntryReader.readEntry();
                String id = resultEntry.getAttribute("umaResourceId").firstValueAsString();
                String rId = resultEntry.getAttribute("umaResourceSetId").firstValueAsString();
                String requestURI = resultEntry.getAttribute("umaResourceURI").firstValueAsString();
                String resourceName = resultEntry.getAttribute("umaResourceName").firstValueAsString();
                String pat = resultEntry.getAttribute("umaResoucePAT").firstValueAsString();
                String policyURI = resultEntry.getAttribute("umaResourcePolicyURI").firstValueAsString();
                String userId = resultEntry.getAttribute("umaResourceUserID").firstValueAsString();
                String realm = resultEntry.getAttribute("umaResourceRealm").firstValueAsString();
                String clientId = resultEntry.getAttribute("umaResourceClientId").firstValueAsString();

                ShareExt share = new ShareExt(rId, resourceName, pat, requestURI, policyURI, userId, realm, clientId);
                share.setId(id);

                shares.add(share);
            }

            return shares;
        } catch (SearchResultReferenceIOException e) {
            throw LdapException.newLdapException(ResultCode.UNAVAILABLE, e);
        } finally {
            if (null != ldapConnection) {
                ldapConnection.close();
            }
        }
    }

    /**
     * Removes the share from LDAP
     *
     * @return
     * @throws LdapException
     */
    void removeShare(String id) throws LdapException {
        LdapConnection ldapConnection = null;
        try {
            ldapConnection = ldapClient.connect(hostname, port);
            ldapConnection.bind(userName, password.toCharArray());

            ldapConnection.delete("umaResourceId=" + id + "," + baseDN);
        } finally {
            if (null != ldapConnection) {
                ldapConnection.close();
            }
        }
    }

    /**
     * Constructs LDAP filter for search
     *
     * @param matchingShareExt
     * @return LDAP filter
     */
    private String constructSearchFilter(ShareExt matchingShareExt) {
        StringBuilder filter = new StringBuilder();

        filter.append("(&");

        if (matchingShareExt.getId() != null) {
            filter.append("(umaResourceId=" + matchingShareExt.getId() + ")");
        }
        if (matchingShareExt.getRequestURI() != null) {
            filter.append("(umaResourceURI=" + matchingShareExt.getRequestURI() + ")");
        }
        if (matchingShareExt.getResourceName() != null) {
            filter.append("(umaResourceName=" + matchingShareExt.getResourceName() + ")");
        }
        if (matchingShareExt.getUserId() != null) {
            filter.append("(umaResourceUserID=" + matchingShareExt.getUserId() + ")");
        }
        if (matchingShareExt.getRealm() != null) {
            filter.append("(umaResourceRealm=" + matchingShareExt.getRealm() + ")");
        }
        if (matchingShareExt.getClientId() != null) {
            filter.append("(umaResourceClientId=" + matchingShareExt.getClientId() + ")");
        }

        filter.append(")");

        return filter.toString();
    }
}

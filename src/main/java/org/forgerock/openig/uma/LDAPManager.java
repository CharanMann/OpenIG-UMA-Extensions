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

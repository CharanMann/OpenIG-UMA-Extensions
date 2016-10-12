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
import org.forgerock.openig.ldap.LdapClient;
import org.forgerock.openig.ldap.LdapConnection;

public class LDAPManager {

    private LdapConnection ldapConnection;
    private String baseDN;

    public LDAPManager(String hostname, int port, String userName, String password, String baseDN) {
        try {
            LdapClient ldapClient = LdapClient.getInstance();
            this.ldapConnection = ldapClient.connect(hostname, port);
            this.ldapConnection.bind(userName, password.toCharArray());
            this.baseDN = baseDN;
        } catch (Exception e) {
            //TODO eating exception for now
        }
    }

    /**
     * Adds the UMA share in LDAP store
     *
     * @param share
     */
    void addShare(Share share) throws LdapException {
        String entryDN = "umaResourceId=" + share.getId() + "," + baseDN;
        Entry entry = new LinkedHashMapEntry(entryDN)
                .addAttribute("objectclass", "top")
                .addAttribute("objectclass", "frUmaRS")
                .addAttribute("umaResourceSetId", share.getResourceSetId())
                .addAttribute("umaResourceURI", share.getPattern())
                .addAttribute("umaResoucePAT", share.getPAT())
                .addAttribute("umaResourcePolicyURL", share.getUserAccessPolicyUri());

        ldapConnection.add(entry);
    }
}

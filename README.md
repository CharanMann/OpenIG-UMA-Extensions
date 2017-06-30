# OpenIG-UMA-Extensions

Current OpenIG-UMA has some limitations: https://backstage.forgerock.com/#!/docs/openig/4.5/gateway-guide#uma-limitations ; this extension cover some of these limitations. <br />

OpenIG UMA Service and Filter Extensions for: <br />
1. Realm support <br />
2. Extend OpenIG-UMA REST endpoint: Authentication using PAT <br /> 
3. User friendly UMA Resource name <br />
4. Persisting UMA ResourceSet id and PAT in OpenDJ <br />

This features are not currently supported in this extension: <br />
1. Automatic refresh of PAT, handling expired PAT. We can use long-lived access tokens for testing purpose  <br />
2. Resource share patterns <br />
3. Remove share from AM using IG REST <br />


Pre-requisites :
================
* Versions used for this project: IG 5.0, AM 5.1, DS 4.0
1. OpenAM has been installed and configured. Sample routes in this example use OpenAM realm '/employees'. 
2. OpenAM UMA service has been configured as specified here: https://backstage.forgerock.com/docs/am/5.1/uma-guide
3. OpenIG has been installed and configured as UMA RS as specified here: https://backstage.forgerock.com/docs/ig/5/gateway-guide#chap-uma  
4. Maven has been installed and configured.


OpenDJ UMA RS store Installation & Configuration:
=================================================
1. Install OpenDJ under /opt/opendjrs. Refer https://backstage.forgerock.com/docs/ds/5/install-guide#chap-setup-ds <br />
   Setup params: <br />
   ============= <br />
   * Root User DN:                  cn=Directory Manager
   * Password                       cangetindj
   * Hostname:                      opendjrs.example.com
   * LDAP Listener Port:            3389
   * Administration Connector Port: 7444
   * SSL/TLS:                       disabled
   * Directory Data:                Backend Type: JE Backend, 
                                    Create New Base DN dc=openig,dc=forgerock,dc=org
   * Base DN Data: Only Create Base Entry (dc=openig,dc=forgerock,dc=org)
2. Copy 99-user.ldif schema to <OPENDJ-HOME>/config/schema.
3. Restart OpenDJ.


OpenIG Configuration:
=====================
1. Build OpenIG-UMA extension by running 'mvn clean install'. This will build openig-uma-ext-1.0.jar under /target directory.
2. Stop OpenIG. 
3. Copy openig-uma-ext-1.0.jar to <OpenIG-TomcatHome>/webapps/ROOT/WEB-INF/lib
4. Copy openig/config/* to <OPENIG_BASE> directory, Some details on this routes: <br />
   * UmaServiceExt in config.json, we can configure LDAP details here:
   ```
       {
         "name": "UmaServiceExt",
         "type": "UmaServiceExt",
         "config": {
           "protectionApiHandler": "ClientHandler",
           "authorizationServerUri": "http://openam135.sample.com:8080/openam/",
   	       "realm": "/employees",
           "clientId": "OpenIG_RS",
           "clientSecret": "password",
           "ldapHost": "192.168.56.122",
           "ldapPort": 3389,
           "ldapAdminId": "cn=Directory Manager",
           "ldapAdminPassword": "cangetindj",
           "ldapBaseDN": "dc=openig,dc=forgerock,dc=org"
         }
       }
   ```
   * UmaFilterExt config, we can configure scope required for this filter here:
   ```
        {
          "type": "UmaFilterExt",
          "config": {
            "protectionApiHandler": "ClientHandler",
            "umaService": "UmaServiceExt",
            "scopes" : [
              "http://login.example.com/scopes/view"
            ]
          }
        }
   ```
      
OpenIG Use Cases testing:
=========================
* Share resource

* Access /history/emp1 with scope:view and RPT 

* Access /history/all with scope:viewAll and RPT 

OpenIG-UMA REST endpoints:
==========================
All below REST endpoints require valid PAT in Authorization header. 
 
* Create share. UMA shares need to have unique uri and name. Note that this restriction is per uid per realm per OAuth Client. In other words if user with uid 'alice' (in realm /employees and and using OAuth Client: OpenIG_RS) has created UMA share with name: TxHistory and uri: /history/emp1, then she can't create share with name: TxHistory and uri /history/emp2 (or name: TxHistory2 and uri /history/emp1) but alice in /customer realm can create such share.
```
curl -X POST \
  http://<OpenIG-Host:Port>/openig/api/system/objects/umaserviceext/share \
  -H 'authorization: Bearer <PAT>' \
  -H 'content-type: application/json' \
  -H 'scope: view' \
  -d '{
	 "uri" : "/history/emp1",
     "name" : "TxHistory",
     "scopes" : [
         "http://apis.example.net/scopes/view",
         "http://apis.example.net/scopes/viewAll"
     ],
     "type" : "http://apis.example.net/history"
 }'
 
{
    "_id": "3c07265f-50fb-4630-b503-b35f663dbd82",
    "resourceURI": "/history/emp1",
    "user_access_policy_uri": "http://openam51.example.com:8282/openam/XUI/?realm=/employees#uma/share/746bc572-61db-4e91-a033-d2b69d0aff070",
    "pat": "fd9898cd-cac9-4d8e-90e9-b4f79fedf9bc",
    "resource_set_id": "746bc572-61db-4e91-a033-d2b69d0aff070",
    "userId": "alice",
    "realm": "/employees",
    "client_id": "OpenIG_RS"
}
```
* Read all shares:
```
curl -X GET \
  'http://<OpenIG-Host:Port>/openig/api/system/objects/umaserviceext/share?_queryFilter=true' \
  -H 'authorization: Bearer <PAT>' \
  -H 'content-type: application/json' 
  
{
    "result": [
        {
            "_id": "3c07265f-50fb-4630-b503-b35f663dbd82",
            "resourceURI": "/history/emp1",
            "user_access_policy_uri": "http://openam51.example.com:8282/openam/XUI/?realm=/employees#uma/share/746bc572-61db-4e91-a033-d2b69d0aff070",
            "pat": "fd9898cd-cac9-4d8e-90e9-b4f79fedf9bc",
            "resource_set_id": "746bc572-61db-4e91-a033-d2b69d0aff070",
            "userId": "alice",
            "realm": "/employees",
            "client_id": "OpenIG_RS"
        }
    ],
    "resultCount": 1,
    "pagedResultsCookie": null,
    "totalPagedResultsPolicy": "NONE",
    "totalPagedResults": -1,
    "remainingPagedResults": -1
}
```
* Read specific share. Note that this requires <OpenIG-ResourceId> in REST URL. 
```
curl -X GET \
  http://<OpenIG-Host:Port>/openig/api/system/objects/umaserviceext/share/<OpenIG-ResourceId> \
  -H 'authorization: Bearer <Valid PAT>' \
  -H 'content-type: application/json' 

{
    "_id": "3c07265f-50fb-4630-b503-b35f663dbd82",
    "resourceURI": "/history/emp1",
    "user_access_policy_uri": "http://openam51.example.com:8282/openam/XUI/?realm=/employees#uma/share/746bc572-61db-4e91-a033-d2b69d0aff070",
    "pat": "fd9898cd-cac9-4d8e-90e9-b4f79fedf9bc",
    "resource_set_id": "746bc572-61db-4e91-a033-d2b69d0aff070",
    "userId": "alice",
    "realm": "/employees",
    "client_id": "OpenIG_RS"
}
```
* Delete specific share. Note that this requires <OpenIG-ResourceId> in REST URL. Note that this doesn't remove resource from OpenAM
```
curl -X DELETE \
  http://<OpenIG-Host:Port>/openig/api/system/objects/umaserviceext/share/<OpenIG-ResourceId> \
  -H 'authorization: Bearer <Valid PAT>' \
  -H 'content-type: application/json' 

{
    "_id": "3c07265f-50fb-4630-b503-b35f663dbd82",
    "resourceURI": "/history/emp1",
    "user_access_policy_uri": "http://openam51.example.com:8282/openam/XUI/?realm=/employees#uma/share/746bc572-61db-4e91-a033-d2b69d0aff070",
    "pat": "fd9898cd-cac9-4d8e-90e9-b4f79fedf9bc",
    "resource_set_id": "746bc572-61db-4e91-a033-d2b69d0aff070",
    "userId": "alice",
    "realm": "/employees",
    "client_id": "OpenIG_RS"
}
```


* * *

Copyright © 2017 ForgeRock, AS.

The contents of this file are subject to the terms of the Common Development and
Distribution License (the License). You may not use this file except in compliance with the
License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
specific language governing permission and limitations under the License.

When distributing Covered Software, include this CDDL Header Notice in each file and include
the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
Header, with the fields enclosed by brackets [] replaced by your own identifying
information: "Portions copyright [year] [name of copyright owner]".

Portions Copyrighted 2017 Charan Mann

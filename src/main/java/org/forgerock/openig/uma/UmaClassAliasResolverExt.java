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
 * openig-uma-ext: Created by Charan Mann on 10/7/16 , 1:42 PM.
 */

package org.forgerock.openig.uma;

import org.forgerock.openig.alias.ClassAliasResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Register all the aliases supported by the {@literal openig-uma-ext} module.
 */
public class UmaClassAliasResolverExt implements ClassAliasResolver {
    private static final Map<String, Class<?>> ALIASES = new HashMap<>();

    static {
        ALIASES.put("UmaServiceExt", UmaSharingServiceExt.class);
        ALIASES.put("UmaFilterExt", UmaResourceServerFilterExt.class);
    }

    @Override
    public Class<?> resolve(final String alias) {
        return ALIASES.get(alias);
    }
}


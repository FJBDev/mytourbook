/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    https://github.com/kevinsawicki/eclipse-oauth2
 *****************************************************************************/
/*
 * Modified for MyTourbook by Frédéric Bard
 */
package net.tourbook.cloud.oauth2;

import net.tourbook.common.UI;

/**
 * OAuth2 utilities.
 */
public class OAuth2Utils {

   /**
    * Generate authorize url for given client
    * See {#link
    * https://www.dropbox.com/developers/documentation/http/documentation#oauth2-authorize}
    *
    * @param client
    * @return authorize url
    */
   public static String getAuthorizeUrl(final OAuth2Client client) {

      final String query = IOAuth2Constants.PARAM_REDIRECT_URI + UI.SYMBOL_EQUAL + client.getRedirectUri() + UI.SYMBOL_MNEMONIC +
            IOAuth2Constants.PARAM_CLIENT_ID + UI.SYMBOL_EQUAL + client.getId() + UI.SYMBOL_MNEMONIC +
            IOAuth2Constants.RESPONSE_TYPE + UI.SYMBOL_EQUAL + IOAuth2Constants.PARAM_TOKEN;

      return client.getAuthorizeUrl() + UI.SYMBOL_QUESTION_MARK + query;
   }
}

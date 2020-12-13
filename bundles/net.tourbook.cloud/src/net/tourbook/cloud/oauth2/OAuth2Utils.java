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

//      final List<NameValuePair> params = new ArrayList<>();
//      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_REDIRECT_URI,
//            client.getRedirectUri()));
//      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_CLIENT_ID,
//            client.getId().toString()));
//      params.add(new BasicNameValuePair(
//            IOAuth2Constants.RESPONSE_TYPE,
//            IOAuth2Constants.PARAM_TOKEN));

//      final String query = URLEncodedUtils.format(params, StandardCharsets.UTF_8);
      return client.getAuthorizeUrl() + '?';//+ query;
   }
}

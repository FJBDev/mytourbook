/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package net.tourbook.cloud.oauth2;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * OAuth2 utilities.
 */
public class OAuth2Utils {

   /**
    * Generate authorize url for given client with default scope
    *
    * @param client
    * @return authorize url
    */
   public static String getAuthorizeUrl(final OAuth2Client client) {
      return getAuthorizeUrl(client, null);
   }

   /**
    * Generate authorize url for given client with given scope
    *
    * @param client
    * @param scope
    * @return authorize url
    */
   public static String getAuthorizeUrl(final OAuth2Client client, final String scope) {
      final List<NameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_REDIRECT_URI,
            client.getRedirectUri()));
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_CLIENT_ID,
            client.getId()));
      if (scope != null && scope.length() > 0) {
         params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_SCOPE,
               scope));
      }
      //final String query = URLEncodedUtils.format(params, null);
      return client.getAuthorizeUrl() + '?';// + query;
   }
}

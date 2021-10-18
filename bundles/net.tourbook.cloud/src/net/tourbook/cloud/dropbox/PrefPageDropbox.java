/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.cloud.dropbox;

<<<<<<< HEAD
import com.sun.net.httpserver.HttpServer;
=======
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.IOAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

=======
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

import org.apache.http.client.utils.URIBuilder;
>>>>>>> refs/remotes/origin/main
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
<<<<<<< HEAD
=======
import org.eclipse.jface.preference.FieldEditorPreferencePage;
>>>>>>> refs/remotes/origin/main
import org.eclipse.jface.preference.IPreferenceStore;
<<<<<<< HEAD
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
=======
import org.eclipse.jface.util.IPropertyChangeListener;
>>>>>>> refs/remotes/origin/main
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
<<<<<<< HEAD
=======
import org.eclipse.swt.widgets.Link;
>>>>>>> refs/remotes/origin/main
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageDropbox extends PreferencePage implements IWorkbenchPreferencePage {

<<<<<<< HEAD
   public static final String      ID         = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$
=======
   //SET_FORMATTING_OFF
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN  = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_AccessToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_BUTTON_AUTHORIZE   = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Button_Authorize;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Group_CloudAccount;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT    = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_ExpiresAt;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_RefreshToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE      = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_WebPage;
   //SET_FORMATTING_ON
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
   public static final String      ClientId   = "vye6ci8xzzsuiao";                          //$NON-NLS-1$

   private IPreferenceStore        _prefStore = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;

   private HttpServer              _server;
   private ThreadPoolExecutor      _threadPoolExecutor;
=======
   public static final String      ID            = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$

   public static final String      ClientId      = "vye6ci8xzzsuiao";                          //$NON-NLS-1$

   public static final int         CALLBACK_PORT = 4917;

   private IPreferenceStore        _prefStore    = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;
>>>>>>> refs/remotes/origin/main
   /*
    * UI controls
    */
<<<<<<< HEAD
=======
   private Group                   _group;
>>>>>>> refs/remotes/origin/main
   private Label                   _labelAccessToken;
   private Label                   _labelAccessToken_Value;
   private Label                   _labelExpiresAt;
   private Label                   _labelExpiresAt_Value;
   private Label                   _labelRefreshToken;
   private Label                   _labelRefreshToken_Value;

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

<<<<<<< HEAD
            if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

               Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {

                     _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
                     _labelExpiresAt_Value.setText(computeAccessTokenExpirationDate());
                     _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

                     stopCallBackServer();

                     updateTokensInformationGroup();
                  }
               });
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
=======
      restoreState();

      _prefChangeListener = event -> {

         if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

            Display.getDefault().syncExec(() -> {

               if (!event.getOldValue().equals(event.getNewValue())) {

                  _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
                  _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
                        _prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME),
                        _prefStore.getInt(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN)));
                  _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

                  _group.redraw();

                  updateTokensInformationGroup();
               }

               if (_server != null) {
                  _server.stopCallBackServer();
               }
            });
         }
      };
>>>>>>> refs/remotes/origin/main
   }

<<<<<<< HEAD
   private String computeAccessTokenExpirationDate() {
=======
   private Composite createUI() {
>>>>>>> refs/remotes/origin/main

      return OAuth2Utils.constructLocalExpireAtDateTime(_prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME) + _prefStore.getInt(
            Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN));
   }

   private void createCallBackServer(final String codeVerifier) {

      if (_server != null) {
         stopCallBackServer();
      }

      try {
         _server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0); //$NON-NLS-1$
         final TokensRetrievalHandler tokensRetrievalHandler = new TokensRetrievalHandler(codeVerifier);
         _server.createContext("/dropboxAuthorizationCode", tokensRetrievalHandler); //$NON-NLS-1$
         _threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

         _server.setExecutor(_threadPoolExecutor);

         _server.start();

         addPrefListener();

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      GridLayoutFactory.fillDefaults().applyTo(parent);

      createUI_10_Authorize(parent);
      createUI_20_TokensInformation(parent);

      return parent;
   }

   private void createUI_10_Authorize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         /*
          * Authorize button
          */
         final Button btnAuthorizeConnection = new Button(container, SWT.NONE);
         setButtonLayoutData(btnAuthorizeConnection);
<<<<<<< HEAD
         btnAuthorizeConnection.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_Authorize);
         btnAuthorizeConnection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });
=======
         btnAuthorizeConnection.setText(PREFPAGE_CLOUDCONNECTIVITY_BUTTON_AUTHORIZE);
         btnAuthorizeConnection.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onClickAuthorize()));
>>>>>>> refs/remotes/origin/main
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(btnAuthorizeConnection);
      }
   }

   private void createUI_20_TokensInformation(final Composite parent) {

<<<<<<< HEAD
      final PixelConverter pc = new PixelConverter(parent);

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.Pref_CloudConnectivity_Dropbox_Tokens_Information_Group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         {
            _labelAccessToken = new Label(group, SWT.NONE);
            _labelAccessToken.setText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Label);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(group, SWT.WRAP);
            _labelAccessToken_Value.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelAccessToken_Value);
         }
         {
            _labelExpiresAt = new Label(group, SWT.NONE);
            _labelExpiresAt.setText(Messages.Pref_CloudConnectivity_Dropbox_ExpiresAt_Label);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
         {
            _labelRefreshToken = new Label(group, SWT.NONE);
            _labelRefreshToken.setText(Messages.Pref_CloudConnectivity_Dropbox_RefreshToken_Label);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelRefreshToken_Value);
=======
      final int textWidth = new PixelConverter(parent).convertWidthInCharsToPixels(60);

      _group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_group);
      _group.setText(PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_group);
      {
         {
            final Label labelWebPage = new Label(_group, SWT.NONE);
            labelWebPage.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(_group, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START + Messages.PrefPage_CloudConnectivity_Dropbox_WebPage_Link + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  Messages.PrefPage_CloudConnectivity_Dropbox_WebPage_Link)));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAccessToken = new Label(_group, SWT.NONE);
            _labelAccessToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN);
            _labelAccessToken.setToolTipText(Messages.PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(_group, SWT.WRAP);
            _labelAccessToken_Value.setToolTipText(Messages.PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().hint(textWidth, SWT.DEFAULT).applyTo(_labelAccessToken_Value);
         }
         {
            _labelRefreshToken = new Label(_group, SWT.NONE);
            _labelRefreshToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(_group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(textWidth, SWT.DEFAULT).applyTo(_labelRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(_group, SWT.NONE);
            _labelExpiresAt.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(_group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
>>>>>>> refs/remotes/origin/main
         }
      }
   }

   private String generateCodeChallenge(final String codeVerifier) {

      byte[] digest = null;
      try {
         final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
         final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
         messageDigest.update(bytes, 0, bytes.length);
         digest = messageDigest.digest();
      } catch (final NoSuchAlgorithmException e) {
         e.printStackTrace();
      }

      return digest == null ? null : Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
   }

   private String generateCodeVerifier() {

      final SecureRandom secureRandom = new SecureRandom();
      final byte[] codeVerifier = new byte[32];
      secureRandom.nextBytes(codeVerifier);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
   }

   @Override
   public void init(final IWorkbench workbench) {
      //Not needed
   }

   @Override
   public boolean okToLeave() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      return super.okToLeave();
   }

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Dropbox app to have access
    * to their Dropbox account.
    */
   private void onClickAuthorize() {

<<<<<<< HEAD
      final String codeVerifier = generateCodeVerifier();
      final String codeChallenge = generateCodeChallenge(codeVerifier);
=======
      if (_server != null) {
         _server.stopCallBackServer();
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      Display.getDefault().syncExec(new Runnable() {
         @Override
         public void run() {
=======
      final String codeVerifier = generateCodeVerifier();
      final String codeChallenge = generateCodeChallenge(codeVerifier);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            createCallBackServer(codeVerifier);
=======
      final DropboxTokensRetrievalHandler tokensRetrievalHandler = new DropboxTokensRetrievalHandler(codeVerifier);
      _server = new LocalHostServer(CALLBACK_PORT, "Dropbox", _prefChangeListener); //$NON-NLS-1$
      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            WEB.openUrl(
                  "https://www.dropbox.com/oauth2/authorize?" + //$NON-NLS-1$
                        IOAuth2Constants.PARAM_CLIENT_ID + UI.SYMBOL_EQUAL + ClientId +
                        "&response_type=" + IOAuth2Constants.PARAM_CODE + //$NON-NLS-1$
                        "&" + IOAuth2Constants.PARAM_REDIRECT_URI + UI.SYMBOL_EQUAL + DropboxClient.DropboxCallbackUrl + //$NON-NLS-1$
                        "&code_challenge=" + codeChallenge + //$NON-NLS-1$
                        "&code_challenge_method=S256&token_access_type=offline"); //$NON-NLS-1$
         }
      });
   }

   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();

      if (isCancel) {
         stopCallBackServer();
=======
      if (!isServerCreated) {
         return;
>>>>>>> refs/remotes/origin/main
      }

<<<<<<< HEAD
=======
      final URIBuilder authorizeUrlBuilder = new URIBuilder();
      authorizeUrlBuilder.setScheme("https"); //$NON-NLS-1$
      authorizeUrlBuilder.setHost("www.dropbox.com"); //$NON-NLS-1$
      authorizeUrlBuilder.setPath("/oauth2/authorize"); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_RESPONSE_TYPE, OAuth2Constants.PARAM_CODE);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_CLIENT_ID, ClientId);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_REDIRECT_URI, DropboxClient.DropboxCallbackUrl);
      authorizeUrlBuilder.addParameter("code_challenge", codeChallenge); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter("code_challenge_method", "S256"); //$NON-NLS-1$ //$NON-NLS-2$
      authorizeUrlBuilder.addParameter("token_access_type", "offline"); //$NON-NLS-1$ //$NON-NLS-2$
      try {
         final String authorizeUrl = authorizeUrlBuilder.build().toString();

         Display.getDefault().syncExec(() -> WEB.openUrl(authorizeUrl));
      } catch (final URISyntaxException e) {
         StatusUtil.log(e);
      }
   }

   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();

      if (isCancel && _server != null) {
         _server.stopCallBackServer();
      }

>>>>>>> refs/remotes/origin/main
      return isCancel;
   }

   @Override
   protected void performDefaults() {

      _labelAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.DROPBOX_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(UI.EMPTY_STRING);
      _labelRefreshToken_Value.setText(_prefStore.getDefaultString(Preferences.DROPBOX_REFRESHTOKEN));

      updateTokensInformationGroup();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN, _labelAccessToken_Value.getText());
         _prefStore.setValue(Preferences.DROPBOX_REFRESHTOKEN, _labelRefreshToken_Value.getText());
<<<<<<< HEAD
         if (StringUtils.isNullOrEmpty(_labelExpiresAt_Value.getText())) {
            _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, UI.EMPTY_STRING);
            _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, UI.EMPTY_STRING);
         }

         stopCallBackServer();
=======

         if (StringUtils.isNullOrEmpty(_labelExpiresAt_Value.getText())) {

            _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, UI.EMPTY_STRING);
            _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, UI.EMPTY_STRING);
         }

         if (_server != null) {
            _server.stopCallBackServer();
         }
>>>>>>> refs/remotes/origin/main
      }

      return isOK;
   }

   private void restoreState() {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
<<<<<<< HEAD
      _labelExpiresAt_Value.setText(computeAccessTokenExpirationDate());
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

      updateTokensInformationGroup();
   }

   private void stopCallBackServer() {

      if (_server != null) {
         _server.stop(0);
         _server = null;

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }
      if (_threadPoolExecutor != null) {
         _threadPoolExecutor.shutdownNow();
      }
   }

   private void updateTokensInformationGroup() {

      final boolean isAuthorized = StringUtils.hasContent(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
=======
      _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
            _prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME),
            _prefStore.getInt(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN)));
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

      updateTokensInformationGroup();
>>>>>>> refs/remotes/origin/main
   }

   private void updateTokensInformationGroup() {

      final boolean isAuthorized = StringUtils.hasContent(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
   }

}

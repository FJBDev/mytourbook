/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import com.sun.net.httpserver.HttpServer;

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
import net.tourbook.cloud.IPreferences;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageDropbox extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String      ID         = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$

   private IPreferenceStore        _prefStore = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;

   private HttpServer              _server;
   private ThreadPoolExecutor      _threadPoolExecutor;
   private MyHttpHandler           _myHttpHandler;
   /*
    * UI controls
    */
   private Text                    _textAccessToken;
   //TODO FB Same UI as Dropbox

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {
            if (event.getProperty().equals(IPreferences.DROPBOX_ACCESSTOKEN)) {

               _textAccessToken.setText(_prefStore.getString(IPreferences.DROPBOX_ACCESSTOKEN));

               stopCallBackServer();
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void createCallBackServer(final String codeVerifier) {
      try {
         _server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
         _myHttpHandler = new MyHttpHandler(codeVerifier);
         _server.createContext("/dropboxAuthorizationCode", _myHttpHandler);
         _threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

         _server.setExecutor(_threadPoolExecutor);

         _server.start();

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      addPrefListener();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      GridLayoutFactory.fillDefaults().applyTo(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         /*
          * Authorize button
          */
         final Button btnAuthorizeConnection = new Button(container, SWT.NONE);
         setButtonLayoutData(btnAuthorizeConnection);
         btnAuthorizeConnection.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_Authorize);
         btnAuthorizeConnection.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });

         /*
          * Access Token
          */
         _textAccessToken = new Text(container, SWT.BORDER);
         _textAccessToken.setEditable(false);
         _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
         _textAccessToken.setTextLimit(50);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_textAccessToken);
      }

      return container;
   }

   private String generateCodeChallenge(final String codeVerifier) {

      byte[] digest = null;
      try {
         final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
         final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
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
   public void init(final IWorkbench workbench) {}

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Dropbox app to have access
    * to their Dropbox account.
    */
   private void onClickAuthorize() {

      final String codeVerifier = generateCodeVerifier();
      final String codeChallenge = generateCodeChallenge(codeVerifier);
      createCallBackServer(codeVerifier);

      WEB.openUrl(
            "https://www.dropbox.com/oauth2/authorize?" +
                  "client_id=vye6ci8xzzsuiao" +
                  "&response_type=code" +
                  "&redirect_uri=" + MyHttpHandler.DropboxCallbackUrl +
                  "&code_challenge=" + codeChallenge +
                  "&code_challenge_method=S256&token_access_type=offline");
   }

   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();

      if (isCancel) {
         stopCallBackServer();
      }

      return isCancel;
   }

   @Override
   protected void performDefaults() {

      _textAccessToken.setText(_prefStore.getDefaultString(IPreferences.DROPBOX_ACCESSTOKEN));
      MessageDialog.openInformation(
            Display.getCurrent().getActiveShell(),
            Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieval_Title,
            "dialogMessage");
      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(IPreferences.DROPBOX_ACCESSTOKEN, _textAccessToken.getText());

         stopCallBackServer();
      }

      return isOK;
   }

   private void restoreState() {
      _textAccessToken.setText(_prefStore.getString(IPreferences.DROPBOX_ACCESSTOKEN));
   }

   private void stopCallBackServer() {

      if (_server != null) {
         _server.stop(1);
      }
      if (_threadPoolExecutor != null) {
         _threadPoolExecutor.shutdownNow();
      }
   }
}

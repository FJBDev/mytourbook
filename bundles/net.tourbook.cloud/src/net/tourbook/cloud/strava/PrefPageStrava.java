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
package net.tourbook.cloud.strava;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.cloud.oauth2.OAuth2BrowserDialog;
import net.tourbook.cloud.oauth2.OAuth2Client;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageStrava extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String ID         = "net.tourbook.cloud.PrefPageStrava";        //$NON-NLS-1$

   private IPreferenceStore   _prefStore = Activator.getDefault().getPreferenceStore();

   private String             _accessToken;
   private String             _refreshToken;

   /*
    * UI controls
    */
   private Button _buttonConnect;

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * Connect button
          */

         // As mentioned here : https://developers.strava.com/guidelines/
         // "All apps must use the Connect with Strava button for OAuth that links to
         // https://www.strava.com/oauth/authorize or https://www.strava.com/oauth/mobile/authorize.
         // No variations or modifications are acceptable."

         _buttonConnect = new Button(container, SWT.NONE);
         setButtonLayoutData(_buttonConnect);
         final Image imageConnect = Activator.getImageDescriptor(Messages.Image__Connect_With_Strava).createImage();
         _buttonConnect.setImage(imageConnect);
         _buttonConnect.setSize(48, 1);
         _buttonConnect.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });
      }
   }

   @Override
   public void init(final IWorkbench workbench) {}

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Strava app to have access
    * to their Strava account.
    */
   private void onClickAuthorize() {

      final OAuth2Client client = new OAuth2Client();

      client.setAuthorizeUrl("https://mytourbook-oauth-passeur.herokuapp.com/authorize"); //$NON-NLS-1$
      client.setRedirectUri("http://mytourbook.sourceforge.net/mytourbook"); //$NON-NLS-1$

      final OAuth2BrowserDialog oAuth2Browser = new OAuth2BrowserDialog(client, "Strava"); //$NON-NLS-1$
      //Opens the dialog
      if (oAuth2Browser.open() != Window.OK) {
         return;
      }

      _accessToken = oAuth2Browser.getAccessToken();
      _refreshToken = oAuth2Browser.getRefreshToken();
      final String dialogMessage = StringUtils.isNullOrEmpty(_accessToken) ? NLS.bind(
            Messages.Pref_CloudConnectivity_Strava_AccessToken_NotRetrieved,
            oAuth2Browser.getResponse()) : Messages.Pref_CloudConnectivity_Strava_AccessToken_Retrieved;

      MessageDialog.openInformation(
            Display.getCurrent().getActiveShell(),
            Messages.Pref_CloudConnectivity_Strava_AccessToken_Retrieval_Title,
            dialogMessage);

      UpdateButtonConnectState();
   }

   @Override
   protected void performDefaults() {

      _accessToken = _prefStore.getDefaultString(IPreferences.STRAVA_ACCESSTOKEN);
      _refreshToken = _prefStore.getDefaultString(IPreferences.STRAVA_REFRESHTOKEN);

      UpdateButtonConnectState();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(IPreferences.STRAVA_ACCESSTOKEN, _accessToken);
         _prefStore.setValue(IPreferences.STRAVA_REFRESHTOKEN, _refreshToken);
      }

      return isOK;
   }

   private void restoreState() {
      _accessToken = _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN);
      _refreshToken = _prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN);

      UpdateButtonConnectState();
   }

   private void UpdateButtonConnectState() {
      final boolean isNotAuthorized = StringUtils.isNullOrEmpty(_accessToken) || StringUtils.isNullOrEmpty(_refreshToken);
      _buttonConnect.setEnabled(isNotAuthorized);
   }
}

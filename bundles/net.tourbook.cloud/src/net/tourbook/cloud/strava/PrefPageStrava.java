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

import java.util.Map;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.cloud.oauth2.OAuth2BrowserDialog;
import net.tourbook.cloud.oauth2.OAuth2Client;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
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

   private Label  labelAthleteFullName;
   private Label  labelAthleteWebPage;
   private Link   labelAthleteWebPageLink;
   private Label  labelAthleteName;
   private Label  labelToken;
   private Label  token;
   private Label  labelRefreshToken;
   private Label  refreshToken;

   private String athleteId;

   private String constructAthleteWebPageLink(final String athleteId) {
      if (StringUtils.hasContent(athleteId)) {
         return "<a>https://www.strava.com/athletes/" + athleteId + "</a>";
      }

      return UI.EMPTY_STRING;
   }

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      createUI_10_Connect(parent);
      createUI_20_Tagging(parent);
   }

   private void createUI_10_Connect(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
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
         GridDataFactory.fillDefaults().applyTo(_buttonConnect);
         final Image imageConnect = Activator.getImageDescriptor(Messages.Image__Connect_With_Strava).createImage();
         _buttonConnect.setImage(imageConnect);
         _buttonConnect.setToolTipText("TOWRITE");
         _buttonConnect.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });

      }
   }

   private void createUI_20_Tagging(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText("Messages.Strava Account Information");
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         labelAthleteName = new Label(group, NONE);
         labelAthleteName.setText("Messages.pref_appearance_number_of_recent_tags:");
         labelAthleteName.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");

         labelAthleteFullName = new Label(group, NONE);

         labelAthleteWebPage = new Label(group, NONE);
         labelAthleteWebPage.setText("Messages.pref_appearance_number_of_recent_tags:");
         labelAthleteWebPage.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");

         labelAthleteWebPageLink = new Link(group, SWT.NONE);
         labelAthleteWebPageLink.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");
         labelAthleteWebPageLink.setEnabled(true);
         labelAthleteWebPageLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               WEB.openUrl(constructAthleteWebPageLink(athleteId));
            }
         });

         labelToken = new Label(group, NONE);
         labelToken.setText("Messages.pref_appearance_number_of_recent_tags:");
         labelToken.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");

         token = new Label(group, NONE);
         token.setText("Messages.pref_appearance_number_of_recent_tags:");
         token.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");

         labelRefreshToken = new Label(group, NONE);
         labelRefreshToken.setText("Messages.pref_appearance_number_of_recent_tags:");
         labelRefreshToken.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");

         refreshToken = new Label(group, NONE);
         refreshToken.setText("Messages.pref_appearance_number_of_recent_tags:");
         refreshToken.setToolTipText("Messages.pref_appearance_number_of_recent_tags_tooltip");
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

      final Map<String, String> responseContent = oAuth2Browser.getResponseContent();

      token.setText(_accessToken);
      refreshToken.setText(_refreshToken);
      if (responseContent.containsKey("athleteFullName")) {
         labelAthleteFullName.setText(responseContent.get("athleteFullName"));
      }
      if (responseContent.containsKey("athleteId")) {
         labelAthleteWebPageLink.setText(constructAthleteWebPageLink(responseContent.get("athleteId")));
      }

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
      labelAthleteFullName.setText(_prefStore.getDefaultString(IPreferences.STRAVA_ATHLETEFULLNAME));
      labelAthleteWebPageLink.setText(constructAthleteWebPageLink(_prefStore.getString(IPreferences.STRAVA_ATHLETEID)));

      UpdateButtonConnectState();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(IPreferences.STRAVA_ACCESSTOKEN, _accessToken);
         _prefStore.setValue(IPreferences.STRAVA_REFRESHTOKEN, _refreshToken);
         _prefStore.setValue(IPreferences.STRAVA_ATHLETEFULLNAME, labelAthleteFullName.getText());
      }

      return isOK;
   }

   private void restoreState() {
      _accessToken = _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN);
      _refreshToken = _prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN);
      labelAthleteFullName.setText(_prefStore.getString(IPreferences.STRAVA_ATHLETEFULLNAME));
      labelAthleteWebPageLink.setText(constructAthleteWebPageLink(_prefStore.getString(IPreferences.STRAVA_ATHLETEID)));

      UpdateButtonConnectState();
   }

   private void UpdateButtonConnectState() {

      final boolean isAuthorized = StringUtils.hasContent(_accessToken) && StringUtils.hasContent(_refreshToken);
      _buttonConnect.setEnabled(!isAuthorized);

      labelAthleteFullName.setEnabled(isAuthorized);
      labelAthleteWebPage.setEnabled(isAuthorized);
      labelAthleteWebPageLink.setEnabled(isAuthorized);
      labelAthleteName.setEnabled(isAuthorized);
      labelToken.setEnabled(isAuthorized);
      labelRefreshToken.setEnabled(isAuthorized);
      refreshToken.setEnabled(isAuthorized);
   }
}

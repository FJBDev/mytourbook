/*******************************************************************************
 * Copyright (C) 2020, 2023 Frédéric Bard
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
package net.tourbook.nutrition;

import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageBeverageContainers extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String      ID                      = "net.tourbook.cloud.PrefPageBeverageContainers"; //$NON-NLS-1$
   private static final int        CALLBACK_PORT           = 4918;

   private static final String     ClientId                = "55536";                                                                     //$NON-NLS-1$

   private static final String     _stravaApp_WebPage_Link = "https://www.strava.com";                                                    //$NON-NLS-1$
   // private IPreferenceStore        _prefStore              = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;



   private long                    _accessTokenExpiresAt;


   /*
    * UI controls
    */
   private Button _btnCleanup;
   private Button _chkAddWeatherIconInTitle;
   private Button _chkSendDescription;
   private Button _chkSendWeatherDataInDescription;
   private Button _chkShowHideTokens;

   private Label  _labelAccessToken;
   private Label  _labelAthleteName;
   private Label  _labelAthleteName_Value;
   private Label  _labelAthleteWebPage;
   private Label  _labelExpiresAt;
   private Label  _labelExpiresAt_Value;
   private Label  _labelRefreshToken;
   private Link   _linkAthleteWebPage;
   private Link   _linkRevokeAccess;
   private Text   _txtAccessToken_Value;
   private Text   _txtRefreshToken_Value;

   private String constructAthleteWebPageLink(final String athleteId) {

      if (StringUtils.hasContent(athleteId)) {
         return "https://www.strava.com/athletes/" + athleteId; //$NON-NLS-1$
      }

      return UI.EMPTY_STRING;
   }

   private String constructAthleteWebPageLinkWithTags(final String athleteId) {

      return UI.LINK_TAG_START + constructAthleteWebPageLink(athleteId) + UI.LINK_TAG_END;
   }

   @Override
   protected void createFieldEditors() {

      initUI();

      createUI();

      restoreState();

      enableControls();

      _prefChangeListener = event -> {
//
//         if (event.getProperty().equals(Preferences.STRAVA_ACCESSTOKEN)) {
//
//            Display.getDefault().syncExec(() -> {
//
//               if (!event.getOldValue().equals(event.getNewValue())) {
//
//                  _txtAccessToken_Value.setText(_prefStore.getString(Preferences.STRAVA_ACCESSTOKEN));
//                  _txtRefreshToken_Value.setText(_prefStore.getString(Preferences.STRAVA_REFRESHTOKEN));
//                  _accessTokenExpiresAt = _prefStore.getLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
//                  _labelExpiresAt_Value.setText(getLocalExpireAtDateTime());
//
//                  _labelAthleteName_Value.setText(_prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME));
//                  _athleteId = _prefStore.getString(Preferences.STRAVA_ATHLETEID);
//                  _linkAthleteWebPage.setText(constructAthleteWebPageLinkWithTags(_athleteId));
//
//                  enableControls();
//               }
//               if (_server != null) {
//                  _server.stopCallBackServer();
//               }
//            });
//         }
      };
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);
      createUI_10_Connect(parent);
      createUI_20_AccountInformation(parent);
      createUI_30_TourUpload(parent);
      createUI_100_AccountCleanup(parent);
   }

   private void createUI_10_Connect(final Composite parent) {

//      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//      GridLayoutFactory.fillDefaults().applyTo(container);
//      {
//         /*
//          * Connect button
//          */
//         // As mentioned here : https://developers.strava.com/guidelines/
//         // "All apps must use the Connect with Strava button for OAuth that links to
//         // https://www.strava.com/oauth/authorize or https://www.strava.com/oauth/mobile/authorize.
//         // No variations or modifications are acceptable."
//
//         final Button buttonConnect = new Button(container, SWT.NONE);
//         buttonConnect.setImage(_imageStravaConnect);
//         buttonConnect.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onClickAuthorize()));
//         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(buttonConnect);
//      }
   }

   private void createUI_100_AccountCleanup(final Composite parent) {

//      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//      GridLayoutFactory.fillDefaults().applyTo(container);
//      {
//         /*
//          * Clean-up button
//          */
//         _btnCleanup = new Button(container, SWT.NONE);
//         _btnCleanup.setText(Messages.PrefPage_CloudConnectivity_Label_Cleanup);
//         _btnCleanup.setToolTipText(Messages.PrefPage_CloudConnectivity_Label_Cleanup_Tooltip);
//         _btnCleanup.addSelectionListener(widgetSelectedAdapter(selectionEvent -> performDefaults()));
//         GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(true, true).applyTo(_btnCleanup);
//      }
   }

   private void createUI_20_AccountInformation(final Composite parent) {

//      final Group group = new Group(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
//      group.setText(Messages.PrefPage_CloudConnectivity_Group_CloudAccount);
//      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//      {
//         {
//            final Label labelWebPage = new Label(group, SWT.NONE);
//            labelWebPage.setText(Messages.PrefPage_CloudConnectivity_Label_WebPage);
//            GridDataFactory.fillDefaults().applyTo(labelWebPage);
//
//            final Link linkWebPage = new Link(group, SWT.NONE);
//            linkWebPage.setText(UI.LINK_TAG_START + _stravaApp_WebPage_Link + UI.LINK_TAG_END);
//            linkWebPage.setEnabled(true);
//            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
//                  _stravaApp_WebPage_Link)));
//            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
//         }
//         {
//            _labelAthleteName = new Label(group, SWT.NONE);
//            _labelAthleteName.setText(Messages.PrefPage_AccountInformation_Label_AthleteName);
//            GridDataFactory.fillDefaults().applyTo(_labelAthleteName);
//
//            _labelAthleteName_Value = new Label(group, SWT.NONE);
//            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelAthleteName_Value);
//         }
//         {
//            _labelAthleteWebPage = new Label(group, SWT.NONE);
//            _labelAthleteWebPage.setText(Messages.PrefPage_AccountInformation_Label_AthleteWebPage);
//            GridDataFactory.fillDefaults().applyTo(_labelAthleteWebPage);
//
//            _linkAthleteWebPage = new Link(group, SWT.NONE);
//            _linkAthleteWebPage.setEnabled(true);
//            _linkAthleteWebPage.addSelectionListener(widgetSelectedAdapter(
//                  selectionEvent -> WEB.openUrl(constructAthleteWebPageLink(_athleteId))));
//            GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkAthleteWebPage);
//         }
//         {
//            _labelAccessToken = new Label(group, SWT.NONE);
//            _labelAccessToken.setText(Messages.PrefPage_CloudConnectivity_Label_AccessToken);
//            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);
//
//            _txtAccessToken_Value = new Text(group, SWT.PASSWORD | SWT.READ_ONLY);
//            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtAccessToken_Value);
//         }
//         {
//            _labelRefreshToken = new Label(group, SWT.NONE);
//            _labelRefreshToken.setText(Messages.PrefPage_CloudConnectivity_Label_RefreshToken);
//            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);
//
//            _txtRefreshToken_Value = new Text(group, SWT.PASSWORD | SWT.READ_ONLY);
//
//            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtRefreshToken_Value);
//         }
//         {
//            _labelExpiresAt = new Label(group, SWT.NONE);
//            _labelExpiresAt.setText(Messages.PrefPage_CloudConnectivity_Label_ExpiresAt);
//            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);
//
//            _labelExpiresAt_Value = new Label(group, SWT.NONE);
//            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
//         }
//         {
//            _chkShowHideTokens = new Button(group, SWT.CHECK);
//            _chkShowHideTokens.setText(Messages.PrefPage_CloudConnectivity_Checkbox_ShowOrHideTokens);
//            _chkShowHideTokens.setToolTipText(Messages.PrefPage_CloudConnectivity_Checkbox_ShowOrHideTokens_Tooltip);
//            _chkShowHideTokens.addSelectionListener(widgetSelectedAdapter(selectionEvent -> showOrHideAllPasswords(_chkShowHideTokens
//                  .getSelection())));
//            GridDataFactory.fillDefaults().applyTo(_chkShowHideTokens);
//         }
//         {
//            _linkRevokeAccess = new Link(group, SWT.NONE);
//            _linkRevokeAccess.setText(Messages.PrefPage_CloudConnectivity_Label_RevokeAccess);
//            _linkRevokeAccess.addSelectionListener(widgetSelectedAdapter(
//                  selectionEvent -> WEB.openUrl("https://www.strava.com/settings/apps")));//$NON-NLS-1$
//            GridDataFactory.fillDefaults()
//                  .span(2, 1)
//                  .indent(0, 16)
//                  .applyTo(_linkRevokeAccess);
//         }
//      }
   }

   private void createUI_30_TourUpload(final Composite parent) {

//      final Group group = new Group(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
//      group.setText(Messages.PrefPage_CloudConnectivity_Group_TourUpload);
//      GridLayoutFactory.swtDefaults().applyTo(group);
//      {
//         {
//            /*
//             * Checkbox: Add weather icon in tour title
//             */
//            _chkAddWeatherIconInTitle = new Button(group, SWT.CHECK);
//            GridDataFactory.fillDefaults().applyTo(_chkAddWeatherIconInTitle);
//
//            _chkAddWeatherIconInTitle.setText(Messages.PrefPage_UploadConfiguration_Button_AddWeatherIconInTitle);
//         }
//         {
//            /*
//             * Checkbox: Send the tour description
//             */
//            _chkSendDescription = new Button(group, SWT.CHECK);
//            GridDataFactory.fillDefaults().applyTo(_chkSendDescription);
//
//            _chkSendDescription.setText(Messages.PrefPage_UploadConfiguration_Button_SendDescription);
//         }
//         {
//            /*
//             * Checkbox: Send the weather data in the tour description
//             */
//            _chkSendWeatherDataInDescription = new Button(group, SWT.CHECK);
//            GridDataFactory.fillDefaults().applyTo(_chkSendWeatherDataInDescription);
//
//            _chkSendWeatherDataInDescription.setText(Messages.PrefPage_UploadConfiguration_Button_SendWeatherDataInDescription);
//            _chkSendWeatherDataInDescription.setToolTipText(Messages.PrefPage_UploadConfiguration_Button_SendWeatherDataInDescription_Tooltip);
//         }
//      }
   }

   @Override
   public void dispose() {

//      UI.disposeResource(_imageStravaConnect);

      super.dispose();
   }

   private void enableControls() {

//      final boolean isAuthorized = StringUtils.hasContent(_txtAccessToken_Value.getText()) &&
//            StringUtils.hasContent(_txtRefreshToken_Value.getText());
//
//      _labelAthleteName_Value.setEnabled(isAuthorized);
//      _labelAthleteWebPage.setEnabled(isAuthorized);
//      _linkAthleteWebPage.setEnabled(isAuthorized);
//      _labelAthleteName.setEnabled(isAuthorized);
//      _labelAccessToken.setEnabled(isAuthorized);
//      _labelRefreshToken.setEnabled(isAuthorized);
//      _txtRefreshToken_Value.setEnabled(isAuthorized);
//      _labelExpiresAt_Value.setEnabled(isAuthorized);
//      _labelExpiresAt.setEnabled(isAuthorized);
//      _linkRevokeAccess.setEnabled(isAuthorized);
//      _chkShowHideTokens.setEnabled(isAuthorized);
//      _chkAddWeatherIconInTitle.setEnabled(isAuthorized);
//      _chkSendDescription.setEnabled(isAuthorized);
//      _chkSendWeatherDataInDescription.setEnabled(isAuthorized);
//      _btnCleanup.setEnabled(isAuthorized);
   }


   @Override
   public void init(final IWorkbench workbench) {
      //Not needed
   }

   private void initUI() {

      noDefaultAndApplyButton();
   }

   @Override
   public boolean okToLeave() {



      return super.okToLeave();
   }



   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();



      return isCancel;
   }

   @Override
   protected void performDefaults() {

//      _txtAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.STRAVA_ACCESSTOKEN));
//      _txtRefreshToken_Value.setText(_prefStore.getDefaultString(Preferences.STRAVA_REFRESHTOKEN));
//      _labelAthleteName_Value.setText(_prefStore.getDefaultString(Preferences.STRAVA_ATHLETEFULLNAME));
//      _athleteId = _prefStore.getDefaultString(Preferences.STRAVA_ATHLETEID);
//      _linkAthleteWebPage.setText(constructAthleteWebPageLinkWithTags(_athleteId));
//      _accessTokenExpiresAt = _prefStore.getDefaultLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
//      _labelExpiresAt_Value.setText(getLocalExpireAtDateTime());
//
//      _chkAddWeatherIconInTitle.setSelection(_prefStore.getDefaultBoolean(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE));
//      _chkSendDescription.setSelection(_prefStore.getDefaultBoolean(Preferences.STRAVA_SENDDESCRIPTION));
//      _chkSendWeatherDataInDescription.setSelection(_prefStore.getDefaultBoolean(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
//         _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, _txtAccessToken_Value.getText());
//         _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, _txtRefreshToken_Value.getText());
//         _prefStore.setValue(Preferences.STRAVA_ATHLETEFULLNAME, _labelAthleteName_Value.getText());
//         _prefStore.setValue(Preferences.STRAVA_ATHLETEID, _athleteId);
//         _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, _accessTokenExpiresAt);
//
//         _prefStore.setValue(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE, _chkAddWeatherIconInTitle.getSelection());
//         _prefStore.setValue(Preferences.STRAVA_SENDDESCRIPTION, _chkSendDescription.getSelection());
//         _prefStore.setValue(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION, _chkSendWeatherDataInDescription.getSelection());
//
//         if (_server != null) {
//            _server.stopCallBackServer();
//         }
      }

      return isOK;
   }

   private void restoreState() {

//      _txtAccessToken_Value.setText(_prefStore.getString(Preferences.STRAVA_ACCESSTOKEN));
//      _txtRefreshToken_Value.setText(_prefStore.getString(Preferences.STRAVA_REFRESHTOKEN));
//      _labelAthleteName_Value.setText(_prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME));
//      _athleteId = _prefStore.getString(Preferences.STRAVA_ATHLETEID);
//      _linkAthleteWebPage.setText(constructAthleteWebPageLinkWithTags(_athleteId));
//      _accessTokenExpiresAt = _prefStore.getLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
//      _labelExpiresAt_Value.setText(getLocalExpireAtDateTime());
//
//      _chkAddWeatherIconInTitle.setSelection(_prefStore.getBoolean(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE));
//      _chkSendDescription.setSelection(_prefStore.getBoolean(Preferences.STRAVA_SENDDESCRIPTION));
//      _chkSendWeatherDataInDescription.setSelection(_prefStore.getBoolean(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION));
   }


}
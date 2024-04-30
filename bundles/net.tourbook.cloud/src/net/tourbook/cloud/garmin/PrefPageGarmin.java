/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.cloud.garmin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.importdata.DialogEasyImportConfig;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGarmin extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   private static final String PREF_CLOUDCONNECTIVITY_ACCESSTOKEN_LABEL  = net.tourbook.cloud.Messages.Pref_CloudConnectivity_AccessToken_Label;
   private static final String PREF_CLOUDCONNECTIVITY_AUTHORIZE_BUTTON   = net.tourbook.cloud.Messages.Pref_CloudConnectivity_Authorize_Button;
   private static final String PREF_CLOUDCONNECTIVITY_CLOUDACCOUNT_GROUP = net.tourbook.cloud.Messages.Pref_CloudConnectivity_CloudAccount_Group;
   private static final String PREF_CLOUDCONNECTIVITY_REFRESHTOKEN_LABEL = net.tourbook.cloud.Messages.Pref_CloudConnectivity_RefreshToken_Label;
   private static final String PREF_CLOUDCONNECTIVITY_WEBPAGE_LABEL      = net.tourbook.cloud.Messages.Pref_CloudConnectivity_WebPage_Label;
   private static final String APP_BTN_BROWSE                            = net.tourbook.Messages.app_btn_browse;
   private static final String DIALOG_EXPORT_DIR_DIALOG_MESSAGE          = net.tourbook.Messages.dialog_export_dir_dialog_message;
   private static final String DIALOG_EXPORT_DIR_DIALOG_TEXT             = net.tourbook.Messages.dialog_export_dir_dialog_text;

   //Put this httpclient in a static class to be reused ?
   private static HttpClient       _httpClient   = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   public static final String      ID            = "net.tourbook.cloud.PrefPageGarmin";                                  //$NON-NLS-1$

   public static final int         CALLBACK_PORT = 4920;

   private IPreferenceStore        _prefStore    = Activator.getDefault().getPreferenceStore();

   private final IDialogSettings   _state        = TourbookPlugin.getState(DialogEasyImportConfig.ID);
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;
   /*
    * UI controls
    */
   private Group                   _group;
   private Label                   _labelAccessToken;
   private Label                   _labelAccessToken_Value;
   private Label                   _labelAccessTokenSecret;
   private Label                   _labelAccessTokenSecret_Value;
   private Label                   _labelDownloadFolder;
   private Combo                   _comboDownloadFolderPath;
   private Button                  _btnSelectFolder;
   private Button                  _chkUseDateFilter;
   private DateTime                _dtFilterSince;

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();

      enableControls();

      _prefChangeListener = event -> {

         if (event.getProperty().equals(Preferences.GARMIN_ACCESSTOKEN)) {

            Display.getDefault().syncExec(() -> {

               if (!event.getOldValue().equals(event.getNewValue())) {

                  _labelAccessToken_Value.setText(_prefStore.getString(Preferences.GARMIN_ACCESSTOKEN));
                  _labelAccessTokenSecret_Value.setText(_prefStore.getString(Preferences.GARMIN_ACCESSTOKEN_SECRET));

                  _group.redraw();

                  enableControls();
               }

               _server.stopCallBackServer();
            });
         }
      };
   }

   private Composite createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      createUI_10_Authorize(parent);
      createUI_20_TokensInformation(parent);
      createUI_30_Misc(parent);

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
         btnAuthorizeConnection.setText(PREF_CLOUDCONNECTIVITY_AUTHORIZE_BUTTON);
         btnAuthorizeConnection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(btnAuthorizeConnection);
      }
   }

   private void createUI_20_TokensInformation(final Composite parent) {

      final PixelConverter pc = new PixelConverter(parent);

      _group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_group);
      _group.setText(PREF_CLOUDCONNECTIVITY_CLOUDACCOUNT_GROUP);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_group);
      {
         {
            final Label labelWebPage = new Label(_group, SWT.NONE);
            labelWebPage.setText(PREF_CLOUDCONNECTIVITY_WEBPAGE_LABEL);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(_group, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START + Messages.Pref_AccountInformation_GarminConnect_WebPage_Link + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  WEB.openUrl(Messages.Pref_AccountInformation_GarminConnect_WebPage_Link);
               }
            });
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAccessToken = new Label(_group, SWT.NONE);
            _labelAccessToken.setText(PREF_CLOUDCONNECTIVITY_ACCESSTOKEN_LABEL);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(_group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelAccessToken_Value);
         }
         {
            _labelAccessTokenSecret = new Label(_group, SWT.NONE);
            _labelAccessTokenSecret.setText(PREF_CLOUDCONNECTIVITY_REFRESHTOKEN_LABEL);
            GridDataFactory.fillDefaults().applyTo(_labelAccessTokenSecret);

            _labelAccessTokenSecret_Value = new Label(_group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelAccessTokenSecret_Value);
         }
      }
   }

   private void createUI_30_Misc(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            _labelDownloadFolder = new Label(container, SWT.NONE);
            _labelDownloadFolder.setText(Messages.Pref_Combo_Workouts_Label_FolderPath);
            _labelDownloadFolder.setToolTipText(Messages.Pref_Combo_Workouts_FolderPath_Combo_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_labelDownloadFolder);

            /*
             * combo: path
             */
            _comboDownloadFolderPath = new Combo(container, SWT.SINGLE | SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboDownloadFolderPath);
            _comboDownloadFolderPath.setToolTipText(Messages.Pref_Combo_Workouts_FolderPath_Combo_Tooltip);
            _comboDownloadFolderPath.setEnabled(false);

            _btnSelectFolder = new Button(container, SWT.PUSH);
            _btnSelectFolder.setText(APP_BTN_BROWSE);
            _btnSelectFolder.setToolTipText(Messages.Pref_Combo_Workouts_FolderPath_Combo_Tooltip);
            _btnSelectFolder.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectBrowseDirectory();
               }
            });
            setButtonLayoutData(_btnSelectFolder);
         }

         {
            /*
             * Checkbox: Use a "since" date filter
             */
            _chkUseDateFilter = new Button(container, SWT.CHECK);
            _chkUseDateFilter.setText(Messages.Pref_Checkbox_Use_SinceDateFilter);
            _chkUseDateFilter.setToolTipText(Messages.Pref_Checkbox_Use_SinceDateFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_chkUseDateFilter);
            _chkUseDateFilter.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  _dtFilterSince.setEnabled(_chkUseDateFilter.getSelection());
               }
            });

            _dtFilterSince = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
            _dtFilterSince.setToolTipText(Messages.Pref_Checkbox_Use_SinceDateFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_dtFilterSince);
         }
      }
   }

   private void enableControls() {

      final boolean isAuthorized = StringUtils.hasContent(_labelAccessToken_Value.getText()) && StringUtils.hasContent(_labelAccessTokenSecret_Value
            .getText());

      _labelAccessToken.setEnabled(isAuthorized);
      _labelAccessTokenSecret.setEnabled(isAuthorized);
      _labelDownloadFolder.setEnabled(isAuthorized);
      _comboDownloadFolderPath.setEnabled(isAuthorized);
      _btnSelectFolder.setEnabled(isAuthorized);
      _chkUseDateFilter.setEnabled(isAuthorized);
      _dtFilterSince.setEnabled(isAuthorized && _chkUseDateFilter.getSelection());
   }

   private long getFilterSinceDate() {

      final int year = _dtFilterSince.getYear();
      final int month = _dtFilterSince.getMonth() + 1;
      final int day = _dtFilterSince.getDay();
      return ZonedDateTime.of(
            year,
            month,
            day,
            0,
            0,
            0,
            0,
            ZoneId.of("Etc/GMT")).toEpochSecond() * 1000; //$NON-NLS-1$
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
    * so that the user can allow the MyTourbook Garmin Connect app to have access
    * to their Garmin account.
    */
   private void onClickAuthorize() {

      //Retrieve the tokens before asking for consent

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(OAuth2Utils.createOAuthPasseurUri("/garmin/request_token"))//$NON-NLS-1$
            .build();

      GarminTokens garminTokens = null;
      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {
            garminTokens = new ObjectMapper().readValue(response.body(), GarminTokens.class);
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      if (garminTokens == null) {
         return;
      }

      final GarminTokensRetrievalHandler tokensRetrievalHandler = new GarminTokensRetrievalHandler(garminTokens.oauth_token_secret);
      _server = new LocalHostServer(CALLBACK_PORT, "Garmin", _prefChangeListener); //$NON-NLS-1$
      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);

      if (!isServerCreated) {
         return;
      }

      final String oauth_token = garminTokens.oauth_token;
      Display.getDefault().syncExec(() -> WEB.openUrl("https://connect.garmin.com/oauthConfirm?oauth_token=" + oauth_token));//$NON-NLS-1$
   }

   private void onSelectBrowseDirectory() {

      final DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(DIALOG_EXPORT_DIR_DIALOG_TEXT);
      dialog.setMessage(DIALOG_EXPORT_DIR_DIALOG_MESSAGE);

      final String selectedDirectoryName = dialog.open();

      if (selectedDirectoryName != null) {

         setErrorMessage(null);
         _comboDownloadFolderPath.setText(selectedDirectoryName);
      }
   }

   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();

      if (isCancel && _server != null) {
         _server.stopCallBackServer();
      }

      return isCancel;
   }

   @Override
   protected void performDefaults() {

      _labelAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.GARMIN_ACCESSTOKEN));
      _labelAccessTokenSecret_Value.setText(_prefStore.getDefaultString(Preferences.GARMIN_ACCESSTOKEN_SECRET));

      _comboDownloadFolderPath.setText(_prefStore.getDefaultString(Preferences.GARMIN_ACTIVITY_DOWNLOAD_FOLDER));

      _chkUseDateFilter.setSelection(_prefStore.getDefaultBoolean(Preferences.GARMIN_USE_WORKOUT_FILTER_SINCE_DATE));
      setFilterSinceDate(_prefStore.getDefaultLong(Preferences.GARMIN_WORKOUT_FILTER_SINCE_DATE));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN, _labelAccessToken_Value.getText());
         _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN_SECRET, _labelAccessTokenSecret_Value.getText());

         if (_server != null) {
            _server.stopCallBackServer();
         }

         final String downloadFolder = _comboDownloadFolderPath.getText();
         _prefStore.setValue(Preferences.GARMIN_ACTIVITY_DOWNLOAD_FOLDER, downloadFolder);
         if (StringUtils.hasContent(downloadFolder)) {

            final String[] currentDeviceFolderHistoryItems = _state.getArray(
                  DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS);
            final List<String> stateDeviceFolderHistoryItems = currentDeviceFolderHistoryItems != null ? new ArrayList<>(Arrays.asList(
                  currentDeviceFolderHistoryItems))
                  : new ArrayList<>();

            if (!stateDeviceFolderHistoryItems.contains(downloadFolder)) {
               stateDeviceFolderHistoryItems.add(downloadFolder);
               _state.put(DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS,
                     stateDeviceFolderHistoryItems.toArray(new String[stateDeviceFolderHistoryItems.size()]));
            }
         }

         _prefStore.setValue(Preferences.GARMIN_USE_WORKOUT_FILTER_SINCE_DATE, _chkUseDateFilter.getSelection());
         _prefStore.setValue(Preferences.GARMIN_WORKOUT_FILTER_SINCE_DATE, getFilterSinceDate());
      }

      return isOK;
   }

   private void restoreState() {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.GARMIN_ACCESSTOKEN));
      _labelAccessTokenSecret_Value.setText(_prefStore.getString(Preferences.GARMIN_ACCESSTOKEN_SECRET));

      _comboDownloadFolderPath.setText(_prefStore.getString(Preferences.GARMIN_ACTIVITY_DOWNLOAD_FOLDER));

      _chkUseDateFilter.setSelection(_prefStore.getBoolean(Preferences.GARMIN_USE_WORKOUT_FILTER_SINCE_DATE));
      setFilterSinceDate(_prefStore.getLong(Preferences.GARMIN_WORKOUT_FILTER_SINCE_DATE));
   }

   private void setFilterSinceDate(final long filterSinceDate) {

      final LocalDate garminActivityDownloadSinceDate = TimeTools.toLocalDate(filterSinceDate);

      _dtFilterSince.setDate(garminActivityDownloadSinceDate.getYear(),
            garminActivityDownloadSinceDate.getMonthValue() - 1,
            garminActivityDownloadSinceDate.getDayOfMonth());
   }

}

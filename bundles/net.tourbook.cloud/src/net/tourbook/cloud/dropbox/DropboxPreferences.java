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

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.ICloudPreferences;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DropboxPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   private final IPreferenceStore _prefStore = TourbookPlugin.getDefault().getPreferenceStore();

   /*
    * UI controls
    */
   private Button _btnAuthorizeConnection;
   private Text   _textAccessToken;
   private Button _btnChooseFolder;
   private Text   _textFolderPath;

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
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Authorize button
             */
            _btnAuthorizeConnection = new Button(container, SWT.NONE);
            _btnAuthorizeConnection.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_Authorize);
            _btnAuthorizeConnection.addSelectionListener(new SelectionAdapter() {

               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onClickAuthorize();
               }
            });

            /*
             * Access Token
             */
            _textAccessToken = new Text(container, SWT.BORDER);
            _textAccessToken.setEnabled(false);
            _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textAccessToken);
         }
         {
            /*
             * Choose Dropbox folder
             */
            _btnChooseFolder = new Button(container, SWT.NONE);
            _btnChooseFolder.setEnabled(false);
            _btnChooseFolder.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_ChooseFolder);
            _btnChooseFolder.addSelectionListener(new SelectionAdapter() {

               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onClickChooseFolder();
               }
            });

            /*
             * Dropbox folder path
             */
            _textFolderPath = new Text(container, SWT.BORDER);
            _textFolderPath.setEnabled(false);
            _textFolderPath.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_FolderPath_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textFolderPath);
         }
      }
   }

   private void enableControls() {

      _btnChooseFolder.setEnabled(!StringUtils.isNullOrEmpty(_textAccessToken.getText()));
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void onClickAuthorize() {

      /*
       * final OAuth2Client client = new OAuth2Client();
       * client.setId("TOINSERTATBUILDTIME"); // client_id
       * client.setSecret("TOINSERTATBUILDTIME"); // client_secret
       * client.setAccessTokenUrl("https://api.dropboxapi.com/oauth2/token");
       * client.setAuthorizeUrl("https://www.dropbox.com/oauth2/authorize");
       * final OAuth2RequestAction request = new OAuth2RequestAction(client, "repo");
       * //Opens the dialog
       * request.run();
       * final String token = request.getAccessToken();
       * final String dialogMessage = StringUtils.isNullOrEmpty(token) ?
       * Messages.Pref_CloudConnectivity_Dropbox_AccessToken_NotRetrieved
       * : Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieved;
       * if (!StringUtils.isNullOrEmpty(token)) {
       * _textAccessToken.setText(token);
       * }
       * MessageDialog.openInformation(
       * Display.getCurrent().getActiveShell(),
       * Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieval_Title,
       * dialogMessage);
       * enableControls();
       */
      // WHere to get the 20.3.0 string ???
      /*
       * final DbxRequestConfig titi = new DbxRequestConfig("mytourbook/20.3.0");
       * final DbxAppClientV2 toto = new DbxAppClientV2(titi, "vye6ci8xzzsuiao", "ovxyfwr544wrdvg");
       * final DbxAppAuthRequests tttt = toto.auth();
       * final DbxCredential credential = new DbxCredential("accesstoken",
       * 2 * DbxCredential.EXPIRE_MARGIN,
       * "refresh_token",
       * "appkey",
       * "app_secret");
       */


      final DbxRequestConfig config = DbxRequestConfig.newBuilder("mytourbook/20.3.0").build();

      final DbxClientV2 clientsss = new DbxClientV2(config,
            _prefStore.getString(ICloudPreferences.DROPBOX_ACCESSTOKEN));
      try {
         final ListFolderResult list = clientsss.files().listFolder("");
         final List<Metadata> entries = list.getEntries();
      } catch (final DbxException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }


      // Create Dropbox client
      //    final DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
   }

   protected void onClickChooseFolder() {
      //https://www.dropboxforum.com/t5/Dropbox-API-Support-Feedback/Chooser-for-directory/td-p/236634
      final DropboxFolderChooser dropboxFolderChooser = new DropboxFolderChooser(Display.getCurrent().getActiveShell());
      if (dropboxFolderChooser.open() == Window.OK) {

         final float newAltitudeValue = dropboxFolderChooser.getReturnCode();//TODO get folder
      }
   }

   @Override
   protected void performDefaults() {

      _textAccessToken.setText(_prefStore.getDefaultString(ICloudPreferences.DROPBOX_ACCESSTOKEN));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(ICloudPreferences.DROPBOX_ACCESSTOKEN, _textAccessToken.getText());
      }

      return isOK;
   }

   private void restoreState() {
      _textAccessToken.setText(_prefStore.getString(ICloudPreferences.DROPBOX_ACCESSTOKEN));

      enableControls();
   }

}

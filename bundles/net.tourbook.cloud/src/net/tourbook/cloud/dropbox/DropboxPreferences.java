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

import net.tourbook.cloud.oauth2.OAuth2Client;
import net.tourbook.cloud.oauth2.OAuth2RequestAction;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class DropboxPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   final IPreferenceStore _prefStore = CommonActivator.getPrefStore();

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
            _textAccessToken.setEditable(false);
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
            _textFolderPath.setEditable(false);
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

      final OAuth2Client client = new OAuth2Client();
      //TODO FB Ask Wolfgang to populate it at build time.
      client.setId("vye6ci8xzzsuiao"); // client_id
      client.setSecret("ovxyfwr544wrdvg"); // client_secret
      client.setAccessTokenUrl("https://api.dropboxapi.com/oauth2/token"); //$NON-NLS-1$
      client.setAuthorizeUrl("https://www.dropbox.com/oauth2/authorize"); //$NON-NLS-1$
      client.setRedirectUri("https://sourceforge.net/projects/mytourbook"); //$NON-NLS-1$

      final OAuth2RequestAction request = new OAuth2RequestAction(client);
      //Opens the dialog
      request.run();
      final String token = request.getAccessToken();
      final String dialogMessage = StringUtils.isNullOrEmpty(token) ? NLS.bind(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_NotRetrieved,
            request
                  .getResponse())
            : Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieved;

      if (!StringUtils.isNullOrEmpty(token)) {
         _textAccessToken.setText(token);

         // Once we retrieve a valid token, by default, we set the user folder to the root folder of
         // their Dropbox account.
         _textFolderPath.setText("/");//$NON-NLS-1$
      }

      MessageDialog.openInformation(
            Display.getCurrent().getActiveShell(),
            Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieval_Title,
            dialogMessage);

      enableControls();
   }

   protected void onClickChooseFolder() {
      final DropboxBrowser dropboxFolderChooser = new DropboxBrowser(Display.getCurrent().getActiveShell(),
            ChooserType.Folder,
            _textAccessToken.getText());

      if (dropboxFolderChooser.open() == Window.OK) {

         final String selectedFolder = dropboxFolderChooser.getSelectedFolder();
         if (!StringUtils.isNullOrEmpty(selectedFolder)) {
            _textFolderPath.setText(selectedFolder);
         }
      }
   }

   @Override
   protected void performDefaults() {

      _textAccessToken.setText(_prefStore.getDefaultString(ICommonPreferences.DROPBOX_ACCESSTOKEN));
      _textFolderPath.setText(_prefStore.getDefaultString(ICommonPreferences.DROPBOX_FOLDER));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {

         _prefStore.setValue(ICommonPreferences.DROPBOX_ACCESSTOKEN, _textAccessToken.getText());
         _prefStore.setValue(ICommonPreferences.DROPBOX_FOLDER, _textFolderPath.getText());
      }

      return isOK;
   }

   private void restoreState() {
      _textAccessToken.setText(_prefStore.getString(ICommonPreferences.DROPBOX_ACCESSTOKEN));
      _textFolderPath.setText(_prefStore.getString(ICommonPreferences.DROPBOX_FOLDER));

      enableControls();
   }
}

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
package net.tourbook.cloud.suunto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageSuunto extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   //
   private static final String PREF_CLOUDCONNECTIVITY_ACCESSTOKEN_LABEL  = net.tourbook.cloud.Messages.Pref_CloudConnectivity_AccessToken_Label;
   private static final String PREF_CLOUDCONNECTIVITY_AUTHORIZE_BUTTON   = net.tourbook.cloud.Messages.Pref_CloudConnectivity_Authorize_Button;
   private static final String PREF_CLOUDCONNECTIVITY_CLOUDACCOUNT_GROUP = net.tourbook.cloud.Messages.Pref_CloudConnectivity_CloudAccount_Group;
   private static final String PREF_CLOUDCONNECTIVITY_EXPIRESAT_LABEL    = net.tourbook.cloud.Messages.Pref_CloudConnectivity_ExpiresAt_Label;
   private static final String PREF_CLOUDCONNECTIVITY_REFRESHTOKEN_LABEL = net.tourbook.cloud.Messages.Pref_CloudConnectivity_RefreshToken_Label;
   private static final String PREF_CLOUDCONNECTIVITY_WEBPAGE_LABEL      = net.tourbook.cloud.Messages.Pref_CloudConnectivity_WebPage_Label;
   //

   public static final String      ID            = "net.tourbook.cloud.PrefPageSuunto";        //$NON-NLS-1$

   public static final String      ClientId      = "d8f3e53f-6c20-4d17-9a4e-a4930c8667e8";     //$NON-NLS-1$

   public static final int         CALLBACK_PORT = 4919;

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
   private Label                   _labelExpiresAt;
   private Label                   _labelExpiresAt_Value;
   private Label                   _labelRefreshToken;
   private Label                   _labelRefreshToken_Value;

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();

      _prefChangeListener = event -> {

         if (event.getProperty().equals(Preferences.SUUNTO_ACCESSTOKEN)) {

            Display.getDefault().syncExec(() -> {

               if (!event.getOldValue().equals(event.getNewValue())) {

                  _labelAccessToken_Value.setText(_prefStore.getString(Preferences.SUUNTO_ACCESSTOKEN));
                  _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
                        _prefStore.getLong(Preferences.SUUNTO_ACCESSTOKEN_ISSUE_DATETIME),
                        _prefStore.getInt(Preferences.SUUNTO_ACCESSTOKEN_EXPIRES_IN) * 1000));
                  _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.SUUNTO_REFRESHTOKEN));

                  _group.redraw();

                  updateTokensInformationGroup();
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
            linkWebPage.setText(UI.LINK_TAG_START + Messages.Pref_AccountInformation_SuuntoApp_WebPage_Link + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  WEB.openUrl(Messages.Pref_AccountInformation_SuuntoApp_WebPage_Link);
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
            _labelRefreshToken = new Label(_group, SWT.NONE);
            _labelRefreshToken.setText(PREF_CLOUDCONNECTIVITY_REFRESHTOKEN_LABEL);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(_group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(_group, SWT.NONE);
            _labelExpiresAt.setText(PREF_CLOUDCONNECTIVITY_EXPIRESAT_LABEL);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(_group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
      }
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
    * so that the user can allow the MyTourbook Suunto app to have access
    * to their Suunto account.
    */
   private void onClickAuthorize() {

      final SuuntoTokensRetrievalHandler tokensRetrievalHandler = new SuuntoTokensRetrievalHandler();
      _server = new LocalHostServer(CALLBACK_PORT, "Suunto", _prefChangeListener); //$NON-NLS-1$
      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);

      if (!isServerCreated) {
         return;
      }

      Display.getDefault().syncExec(() -> WEB.openUrl(
            "https://cloudapi-oauth.suunto.com/oauth/authorize?response_type=code&client_id=" + ClientId + "&redirect_uri=http://localhost:4919"));//$NON-NLS-1$ //$NON-NLS-2$
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

      _labelAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.SUUNTO_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(UI.EMPTY_STRING);
      _labelRefreshToken_Value.setText(_prefStore.getDefaultString(Preferences.SUUNTO_REFRESHTOKEN));

      updateTokensInformationGroup();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(Preferences.SUUNTO_ACCESSTOKEN, _labelAccessToken_Value.getText());
         _prefStore.setValue(Preferences.SUUNTO_REFRESHTOKEN, _labelRefreshToken_Value.getText());
         if (StringUtils.isNullOrEmpty(_labelExpiresAt_Value.getText())) {
            _prefStore.setValue(Preferences.SUUNTO_ACCESSTOKEN_ISSUE_DATETIME, UI.EMPTY_STRING);
            _prefStore.setValue(Preferences.SUUNTO_ACCESSTOKEN_EXPIRES_IN, UI.EMPTY_STRING);
         }

         if (_server != null) {
            _server.stopCallBackServer();
         }
         final String[] ffdsw = _state.getArray(DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS);
         final List<String> titi = new ArrayList<>(Arrays.asList(ffdsw));
         titi.add("file:///home/frederic/Downloads/S9FilesToImport");
         _state.put(DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS, titi.toArray(new String[titi.size()]));
      }

      return isOK;
   }

   private void restoreState() {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.SUUNTO_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
            _prefStore.getLong(Preferences.SUUNTO_ACCESSTOKEN_ISSUE_DATETIME),
            _prefStore.getInt(Preferences.SUUNTO_ACCESSTOKEN_EXPIRES_IN) * 1000));
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.SUUNTO_REFRESHTOKEN));

      updateTokensInformationGroup();
   }

   private void updateTokensInformationGroup() {

      final boolean isAuthorized = StringUtils.hasContent(_prefStore.getString(Preferences.SUUNTO_ACCESSTOKEN));

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
   }

}

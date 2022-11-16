/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package preferences;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.tourbook.Messages;

import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class PrefPageTagsTests extends UITest {

   @Test
   void openPreferencePage() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("Tagging").select(); //$NON-NLS-1$

      bot.tree(1).getTreeItem("Shoes 2").select(); //$NON-NLS-1$
      bot.button(Messages.Action_Tag_Edit).click();

      bot.button(Messages.App_Action_Save).click();
      Utils.clickApplyAndCloseButton(bot);
   }

   @Test
   void PrefPageTags_CreateAndDeleteTag_ShouldBeDeleted() {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("Tagging").select(); //$NON-NLS-1$

      //assert tag # is 1
      bot.button(Messages.pref_tourtag_btn_new_tag).click();
      bot.textWithLabel(Messages.pref_tourtag_dlg_new_tag_message).setText("New Tag");
      Utils.clickOkButton(bot);

      final var toto = bot.tree(1).getTreeItem("New Tag").select(); //$NON-NLS-1$
      assertNotNull(toto);
      //assert tag # is 2

      toto.contextMenu(Messages.Action_Tag_Delete).click();
      //assert tag # is 1
      Utils.clickApplyAndCloseButton(bot);
   }

}

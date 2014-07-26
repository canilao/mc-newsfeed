package org.maylincraft.newsfeed.listeners;

import java.sql.SQLException;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.maylincraft.newsfeed.NewsFeedPlugin;
import org.maylincraft.newsfeed.database.Database;

public class BlockBreakListener implements Listener {

   @SuppressWarnings("unused")
   private NewsFeedPlugin plugIn;

   public BlockBreakListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Database db = NewsFeedPlugin.getNewsFeedDatabase();

      if (event.getBlock().getType() == Material.DIAMOND_ORE) {
         try {
            db.insertDiamondOreBreak(event);
         } catch (SQLException e) {
            NewsFeedPlugin.logWarning(e.getMessage(), e);
         }
      }
   }
}

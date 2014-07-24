package org.maylincraft.newsfeed.listeners;

import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.maylincraft.newsfeed.NewsFeedPlugin;

public class PlayerDeathListener implements Listener {

   @SuppressWarnings("unused")
   private NewsFeedPlugin plugIn;

   public PlayerDeathListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onPlayerDeath(final PlayerDeathEvent event) {
      try {
         NewsFeedPlugin.getNewsFeedDatabase().insertPlayerDeath(event);
      } catch (SQLException e) {
         NewsFeedPlugin.logWarning("Failed onPlayerDeath()", e);
      }
   }
}

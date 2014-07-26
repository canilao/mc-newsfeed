package org.maylincraft.newsfeed.listeners;

import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.maylincraft.newsfeed.NewsFeedPlugin;
import org.maylincraft.newsfeed.database.Database;

public class LoginListener implements Listener {

   @SuppressWarnings("unused")
   private NewsFeedPlugin plugIn;

   public LoginListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onPlayerLogin(PlayerLoginEvent event) {
      Database db = NewsFeedPlugin.getNewsFeedDatabase();
      try {
         db.insertRecordNewPlayer(event.getPlayer().getName());
         db.insertPlayerLogin(event.getPlayer().getName(),
               Database.getIsoTime());
      } catch (SQLException e) {
         NewsFeedPlugin.logWarning(e.getMessage(), e);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Database db = NewsFeedPlugin.getNewsFeedDatabase();
      try {
         db.insertPlayerQuit(event.getPlayer().getName(), Database.getIsoTime());
      } catch (SQLException e) {
         NewsFeedPlugin.logWarning(e.getMessage(), e);
      }
   }
}

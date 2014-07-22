package org.maylincraft.newsfeed;

import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LoginListener implements Listener {

   private NewsFeedPlugin plugIn;

   public LoginListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onPlayerLogin(PlayerLoginEvent event) {
      Database db = NewsFeedPlugin.getSqliteDatabase();
      try {
         db.insertRecordNewPlayer(event.getPlayer().getName()); 
         db.insertPlayerLogin(event.getPlayer().getName(), Database.getIsoTime());
      } catch (SQLException e) {
         plugIn.getLogger().warning(e.getMessage());
      }
   }
   
   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Database db = NewsFeedPlugin.getSqliteDatabase();
      try {
         db.insertPlayerQuit(event.getPlayer().getName(), Database.getIsoTime());
      } catch (SQLException e) {
         plugIn.getLogger().warning(e.getMessage());
      }
   }
}

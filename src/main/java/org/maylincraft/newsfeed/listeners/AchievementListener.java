package org.maylincraft.newsfeed.listeners;

import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.maylincraft.newsfeed.NewsFeedPlugin;
import org.maylincraft.newsfeed.database.Database;

public class AchievementListener implements Listener {
   
   @SuppressWarnings("unused")
   private NewsFeedPlugin plugIn;

   public AchievementListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onAchievement(PlayerAchievementAwardedEvent event) {
      Database db = NewsFeedPlugin.getNewsFeedDatabase();
      
      try {
         db.insertAchievementEvent(event);
      } catch (SQLException e) {
         NewsFeedPlugin.logWarning(e.getMessage(), e);
      } catch (Exception e) {
         NewsFeedPlugin.logWarning(e.getMessage(), e);
      }
   }
}

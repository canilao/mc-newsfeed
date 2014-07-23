package org.maylincraft.newsfeed.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maylincraft.newsfeed.NewsFeedPlugin;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;

public class McmmoXpGainListener implements Listener {

   static public final int levelUpEventInterval = 10;

   @SuppressWarnings("unused")
   private NewsFeedPlugin plugIn;

   public McmmoXpGainListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }

   @EventHandler
   public void onPlayerLevelUp(final McMMOPlayerLevelUpEvent event) {
      try {
         processLevelUpEvent(event);
      } catch (Exception e) {
         Bukkit.getLogger().warning("Failed processLevelUpEvent()");
      }
   }

   private void processLevelUpEvent(final McMMOPlayerLevelUpEvent event)
         throws Exception {
      // Insert int the database only if the skill level is high enough to post
      // to the newsfeed.
      if (event.getSkillLevel() % levelUpEventInterval == 0) {
         NewsFeedPlugin.getNewsFeedDatabase().insertMcmmoSkillEvent(event);
      }
   }
}

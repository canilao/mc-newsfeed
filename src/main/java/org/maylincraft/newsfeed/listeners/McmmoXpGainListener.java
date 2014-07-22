package org.maylincraft.newsfeed.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maylincraft.newsfeed.NewsFeedPlugin;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;

public class McmmoXpGainListener implements Listener {

   private NewsFeedPlugin plugIn;

   public McmmoXpGainListener(NewsFeedPlugin thePlugin) {
      plugIn = thePlugin;
   }
   
   @EventHandler
   public void onPlayerLevelUp(final McMMOPlayerLevelUpEvent event) {
      plugIn.getLogger().info("TEST: " + event.getSkill() + " - " + event.getSkillLevel());
   }
}

package org.maylincraft.newsfeed;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.util.player.UserManager;

public class McmmoFullStats extends HttpServlet {

   private static final long serialVersionUID = -8878317726882341382L;

   static HashMap<String, PlayerProfile> playerProfileCache = new HashMap<String, PlayerProfile>();
   static long lastCachedPlayerProfiles = 0L;

   @SuppressWarnings("unchecked")
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {

      McMMOPlayer player;
      PlayerProfile playerProfile;
      JSONObject jsonObj;
      JSONArray jsonArray;
      boolean playerOnline = false;

      // Get all of the users.
      List<String> userNames = mcMMO.getDatabaseManager().getStoredUsers();

      // Create the JSONObject to generate a response.
      jsonArray = new JSONArray();

      // Clear out the cache if its time.
      if (System.currentTimeMillis() > lastCachedPlayerProfiles + 60000 * 5) {
         playerProfileCache.clear();
      }

      // For each user, get his/her profile.
      for (String userName : userNames) {
         // Create a JSONObject for this user.
         jsonObj = new JSONObject();

         // There are 2 places to look for the player, offline data or online.
         for (Player p : Bukkit.getOnlinePlayers()) {
            playerOnline = p.getName().equals(userName);
            if (playerOnline)
               break;
         }

         if (playerOnline) {
            // Player is online.
            player = UserManager.getPlayer(userName);
            // In this case the player was online.
            playerProfile = player.getProfile();
         } else {
            // Before we go to the database check the cache.
            if (!playerProfileCache.containsKey(userName)) {
               // Not in cache so go ahead and grab from the database.
               playerProfile = mcMMO.getDatabaseManager().loadPlayerProfile(
                     userName, false);
               // Now cache the PlayerProfile object.
               playerProfileCache.put(userName, playerProfile);
               // Save the cached time.
               lastCachedPlayerProfiles = System.currentTimeMillis();
            } else {
               // It was in the cache, get it from there.
               playerProfile = playerProfileCache.get(userName);
            }
         }

         // Add the data to the JSONObject.
         jsonObj.put("Name", userName);
         jsonObj.put("Mining",
               Integer.toString(playerProfile.getSkillLevel(SkillType.MINING)));
         jsonObj.put("WoodCutting", Integer.toString(playerProfile
               .getSkillLevel(SkillType.WOODCUTTING)));
         jsonObj.put("Repair",
               Integer.toString(playerProfile.getSkillLevel(SkillType.REPAIR)));
         jsonObj
               .put("Unarmed", Integer.toString(playerProfile
                     .getSkillLevel(SkillType.UNARMED)));
         jsonObj.put("Herbalism", Integer.toString(playerProfile
               .getSkillLevel(SkillType.HERBALISM)));
         jsonObj.put("Excavation", Integer.toString(playerProfile
               .getSkillLevel(SkillType.EXCAVATION)));
         jsonObj
               .put("Archery", Integer.toString(playerProfile
                     .getSkillLevel(SkillType.ARCHERY)));
         jsonObj.put("Swords",
               Integer.toString(playerProfile.getSkillLevel(SkillType.SWORDS)));
         jsonObj.put("Axes",
               Integer.toString(playerProfile.getSkillLevel(SkillType.AXES)));
         jsonObj.put("Acrobatics", Integer.toString(playerProfile
               .getSkillLevel(SkillType.ACROBATICS)));
         jsonObj.put("Taming",
               Integer.toString(playerProfile.getSkillLevel(SkillType.TAMING)));
         jsonObj
               .put("Fishing", Integer.toString(playerProfile
                     .getSkillLevel(SkillType.FISHING)));
         jsonObj.put("PowerLevel", CalculatePowerLevel(playerProfile));

         // Add it to the collection.
         jsonArray.add(jsonObj);
      }

      response.setContentType("text/html;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);

      response.getWriter().println(jsonArray.toJSONString());
      response.addHeader("Access-Control-Allow-Origin", "*");
   }

   private int CalculatePowerLevel(PlayerProfile playerProfile) {
      return playerProfile.getSkillLevel(SkillType.MINING)
            + playerProfile.getSkillLevel(SkillType.WOODCUTTING)
            + playerProfile.getSkillLevel(SkillType.REPAIR)
            + playerProfile.getSkillLevel(SkillType.UNARMED)
            + playerProfile.getSkillLevel(SkillType.HERBALISM)
            + playerProfile.getSkillLevel(SkillType.EXCAVATION)
            + playerProfile.getSkillLevel(SkillType.ARCHERY)
            + playerProfile.getSkillLevel(SkillType.SWORDS)
            + playerProfile.getSkillLevel(SkillType.AXES)
            + playerProfile.getSkillLevel(SkillType.ACROBATICS)
            + playerProfile.getSkillLevel(SkillType.TAMING)
            + playerProfile.getSkillLevel(SkillType.FISHING);
   }
}

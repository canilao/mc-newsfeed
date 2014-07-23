package org.maylincraft.newsfeed;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.maylincraft.newsfeed.listeners.McmmoXpGainListener;
import org.maylincraft.newsfeed.data.McmmoFullStats;;

public class NewsFeedPlugin extends JavaPlugin {

   private static Database db;

   public static Database getNewsFeedDatabase() {
      return db;
   }

   @Override
   public void onEnable() {
      // Initialize the database.
      try {
         initializeDatabase();
      } catch (InstantiationException e) {
         logSevere("Failed to initialize database: " + e.getMessage());
      } catch (IllegalAccessException e) {
         logSevere("Failed to initialize database: " + e.getMessage());
      } catch (ClassNotFoundException e) {
         logSevere("Failed to initialize database: " + e.getMessage());
      } catch (SQLException e) {
         logSevere("Failed to initialize database: " + e.getMessage());
      }

      // Register our event listeners.
      registerListeners();

      // Start the web service.
      try {
         startWebServer();
      } catch (Exception e) {
         logSevere("Failed to initialize webserver: " + e.getMessage());
      }
   }

   @Override
   public void onDisable() {
      try {
         db.close();
      } catch (SQLException e) {
         logSevere("Failed to close the database: " + e.getMessage());
      }
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label,
         String[] args) {
      if (cmd.getName().equalsIgnoreCase("basic")) {
         // If the player typed
         // /basic then do the
         // following...
         // do something...
         return true;
      } else if (cmd.getName().equalsIgnoreCase("basic2")) {
         if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
         } else {
            Player player = (Player) sender;
            player.sendMessage("asdfasdfasdf");
            // do something
         }
         return true;
      }
      return false;
   }

   private void logSevere(String msg) {
      this.getLogger().severe(msg);
   }

   private void initializeDatabase() throws InstantiationException,
         IllegalAccessException, ClassNotFoundException, SQLException {
      db = new Database();
      db.initialize();
   }

   private void registerListeners() {
      getServer().getPluginManager().registerEvents(new LoginListener(this),
            this);
      getServer().getPluginManager().registerEvents(
            new McmmoXpGainListener(this), this);
   }

   private void startWebServer() throws Exception {
      Server server = new Server(1975);

      ServletContextHandler context = new ServletContextHandler(
            ServletContextHandler.SESSIONS);
      context.setContextPath("/");
      server.setHandler(context);

      ResourceHandler resource_handler = new ResourceHandler();
      resource_handler.setDirectoriesListed(true);
      resource_handler.setWelcomeFiles(new String[] { "index.html" });
      resource_handler.setResourceBase("plugins/newsfeed/web");

      context.setContextPath("/");
      context.setResourceBase("plugins/newsfeed/web");
      context.setClassLoader(Thread.currentThread().getContextClassLoader());

      ServletContextHandler helloContextHandler = new ServletContextHandler(
            ServletContextHandler.SESSIONS);
      helloContextHandler.setContextPath("/data");
      helloContextHandler.addServlet(new ServletHolder(new McmmoFullStats()),
            "/*");

      ContextHandlerCollection contexts = new ContextHandlerCollection();
      contexts.setHandlers(new Handler[] { resource_handler,
            helloContextHandler });

      server.setHandler(contexts);

      server.start();
   }
}

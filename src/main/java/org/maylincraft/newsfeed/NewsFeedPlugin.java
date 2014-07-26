package org.maylincraft.newsfeed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.maylincraft.newsfeed.data.McmmoFullStats;
import org.maylincraft.newsfeed.database.Database;
import org.maylincraft.newsfeed.listeners.BlockBreakListener;
import org.maylincraft.newsfeed.listeners.LoginListener;
import org.maylincraft.newsfeed.listeners.McmmoXpGainListener;
import org.maylincraft.newsfeed.listeners.PlayerDeathListener;

;

public class NewsFeedPlugin extends JavaPlugin {

   private static Database db;
   private static NewsFeedPlugin thePlugin;

   public static Database getNewsFeedDatabase() {
      return db;
   }

   public static void logSevere(String msg, Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      thePlugin.getLogger().severe(msg + " - " + sw.toString());
   }

   public static void logWarning(String msg, Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      thePlugin.getLogger().severe(msg + " - " + sw.toString());
   }

   @Override
   public void onEnable() {
      thePlugin = this;
      // Create the directory structure.
      createDirectoryStructure();
      // Initialize the database.
      try {
         initializeDatabase();
      } catch (InstantiationException e) {
         logSevere("Failed to initialize database: ", e);
      } catch (IllegalAccessException e) {
         logSevere("Failed to initialize database: ", e);
      } catch (ClassNotFoundException e) {
         logSevere("Failed to initialize database: ", e);
      } catch (SQLException e) {
         logSevere("Failed to initialize database: ", e);
      }

      // Register our event listeners.
      registerListeners();

      // Start the web service.
      try {
         startWebServer();
      } catch (Exception e) {
         logSevere("Failed to initialize webserver: ", e);
      }
   }

   private void createDirectoryStructure() {
      boolean success = true;

      File rsrcDir = new File("plugins/newsfeed/");
      if (!rsrcDir.exists()) {
         success &= rsrcDir.mkdir();
      }
      File dbDir = new File("plugins/newsfeed/db/");
      if (!dbDir.exists()) {
         success &= dbDir.mkdir();
      }
      File webDir = new File("plugins/newsfeed/web/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }
      webDir = new File("plugins/newsfeed/web/css/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }
      webDir = new File("plugins/newsfeed/web/js/");
      if (!webDir.exists()) {
         success &= webDir.mkdir();
      }

      try {
         if (success) {
            // Copy the files to the structure.
            copyFileFromJar("/web/index.html",
                  "plugins/newsfeed/web/index.html");
            copyFileFromJar("/web/css/bootstrap.min.css",
                  "plugins/newsfeed/web/css/bootstrap.min.css");
            copyFileFromJar("/web/css/styles.css",
                  "plugins/newsfeed/web/css/styles.css");
            copyFileFromJar("/web/js/bootstrap.min.js",
                  "plugins/newsfeed/web/js/bootstrap.min.js");
            copyFileFromJar("/web/js/scripts.js",
                  "plugins/newsfeed/web/js/scripts.js");
         } else {
            throw new IOException();
         }
      } catch (IOException e) {
         logSevere("Failed to create directory structure", e);
      }
   }

   private void copyFileFromJar(String resourcePath, String destinationPath)
         throws IOException {
      InputStream stream = NewsFeedPlugin.class
            .getResourceAsStream(resourcePath);
      if (stream == null) {
         getLogger().severe("Failed to create input stream");
      }
      OutputStream resStreamOut = null;
      int readBytes;
      byte[] buffer = new byte[4096];
      try {
         resStreamOut = new FileOutputStream(new File(destinationPath));
         while ((readBytes = stream.read(buffer)) > 0) {
            resStreamOut.write(buffer, 0, readBytes);
         }
      } catch (IOException e) {
         logSevere("Failed to write files", e);
      } finally {
         stream.close();
         if (resStreamOut != null) {
            resStreamOut.close();
         }
      }
   }

   @Override
   public void onDisable() {
      try {
         db.close();
      } catch (SQLException e) {
         logSevere("Failed to close the database: ", e);
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
      getServer().getPluginManager().registerEvents(
            new PlayerDeathListener(this), this);
      getServer().getPluginManager().registerEvents(
            new BlockBreakListener(this), this);
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

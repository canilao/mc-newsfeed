package org.maylincraft.newsfeed.data;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.maylincraft.newsfeed.NewsFeedPlugin;

public class NewsFeed extends HttpServlet {

   private static final long serialVersionUID = 9091108444099548045L;

   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {
      try {
         JSONArray jsonArray = NewsFeedPlugin.getNewsFeedDatabase()
               .getNewsFeed();

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_OK);

         response.getWriter().println(jsonArray.toJSONString());
         response.addHeader("Access-Control-Allow-Origin", "*");

      } catch (SQLException e) {
         NewsFeedPlugin.logWarning(
               "Fail to generate news feed for web request", e);

         response.setContentType("text/html;charset=utf-8");
         response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

         response.addHeader("Access-Control-Allow-Origin", "*");
      }
   }
}

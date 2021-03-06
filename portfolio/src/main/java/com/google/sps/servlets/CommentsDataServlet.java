// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;


import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/comments")
public class CommentsDataServlet extends HttpServlet {


  @Override 
  public void init() {

  }
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String langCode = request.getParameter("langCode");
    String musicId = request.getParameter("musicId");

    String json = convertListMapToJson(getCommentsByMusicId(musicId, langCode));

    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String comment = request.getParameter("comment");
    String musicId = request.getParameter("musicId");
	long timestamp = System.currentTimeMillis();
    

	Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("content", comment);
    commentEntity.setProperty("musicId", musicId);
    commentEntity.setProperty("timestamp", timestamp);

	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

	response.sendRedirect("blog/index.html");
  }

  private List<Map<String, String> > getCommentsByMusicId(String musicId, String langCode) {
    Filter propertyFilter = new FilterPredicate("musicId", FilterOperator.EQUAL, musicId);
    Query q = new Query("Comment").setFilter(propertyFilter).addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery pq = datastore.prepare(q);

    List<Map<String, String> > comments = new ArrayList<>();
    for (Entity result : pq.asIterable()) {
        String content = (String)result.getProperty("content");
        String id = (String)result.getProperty("musicId");
        Long timestamp = (Long)result.getProperty("timestamp");

        Translate translate = TranslateOptions.getDefaultInstance().getService();
        Translation translation =
            translate.translate(content, Translate.TranslateOption.targetLanguage(langCode));
        String translatedContent = translation.getTranslatedText();

        System.out.println(id + " " + timestamp + " " + translatedContent);
        Map<String, String> commentsData = new HashMap<String, String>();
        commentsData.put("content", translatedContent);
        commentsData.put("timestamp", String.valueOf(timestamp));
        comments.add(commentsData);
    }
    return comments;
  }

  private String convertListMapToJson(List<Map<String, String> >data){
      Gson gson = new GsonBuilder().disableHtmlEscaping().create();
      String json = gson.toJson(data);
      return json;
  }
}

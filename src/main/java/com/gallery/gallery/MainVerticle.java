package com.gallery.gallery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.core.buffer.Buffer;
import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private List<String> uploadedImages = new ArrayList<>();

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);

    // Serve static content (CSS, JS, Images)
    router.route("/static/*").handler(StaticHandler.create().setCachingEnabled(false));

    // Enable multipart form data parsing
    router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

    // Display form for uploading images
    router.get("/upload").handler(ctx -> {
      ctx.response().putHeader("content-type", "text/html")
        .end("<form action=\"/upload\" method=\"post\" enctype=\"multipart/form-data\">\n" +
          "    <div>\n" +
          "        <label for=\"file\">Select an image:</label>\n" +
          "        <input type=\"file\" id=\"file\" name=\"file\" />\n" +
          "    </div>\n" +
          "    <div class=\"button\">\n" +
          "        <button type=\"submit\">Upload</button>\n" +
          "    </div>" +
          "</form>");
    });

    // Handle uploaded images
    router.post("/upload").handler(ctx -> {
      ctx.response().putHeader("content-type", "text/html");
      ctx.response().setChunked(true);

      // Handle uploaded files
      ctx.fileUploads().forEach(fileUpload -> {
        String uploadedFileName = "uploads/" + fileUpload.fileName();
        uploadedImages.add(uploadedFileName);

        // Move uploaded file to 'uploads' directory
        vertx.fileSystem().move(fileUpload.uploadedFileName(), uploadedFileName, res -> {
          if (res.succeeded()) {
            ctx.response().write("Filename: " + fileUpload.fileName());
            ctx.response().write("<br/>");
            ctx.response().write("Size: " + fileUpload.size());
            ctx.response().write("<br/>");
            ctx.response().write("Uploaded File Path: " + uploadedFileName);
          } else {
            ctx.response().write("Failed to process the uploaded file.");
          }
          ctx.response().end();
        });
      });
    });

    // Endpoint to get uploaded images
    router.get("/uploadedImages").handler(ctx -> {
      ctx.response().putHeader("content-type", "application/json");
      ctx.response().end(Json.encodePrettily(uploadedImages));
    });

    // Start the server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onSuccess(server -> {
        startPromise.complete();
        System.out.println("HTTP server started on port 8080");
      })
      .onFailure(error -> {
        startPromise.fail(error);
        System.err.println("Failed to start HTTP server: " + error.getMessage());
      });
  }
}

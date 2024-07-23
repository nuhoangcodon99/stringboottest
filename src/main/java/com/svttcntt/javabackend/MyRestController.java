package com.svttcntt.javabackend;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.header.Header;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;

@RestController
@CrossOrigin(originPatterns = "*",allowCredentials = "true")
@RequestMapping("/api")
public class MyRestController {
    MongoClient client;
    MongoDatabase db;
    MongoCollection users;
    String uri = "mongodb://localhost:27017/?readPreference=primary&directConnection=true&ssl=false";
    public MyRestController() {
        client = MongoClients.create(uri);
        db=client.getDatabase("test");
        users =db.getCollection("user");
    }
    private ResponseEntity<String> Failed(){
        return new ResponseEntity<String>("false",HttpStatus.OK);
    }
    private ResponseEntity<String> Success(){
        return new ResponseEntity<String>("true",HttpStatus.OK);
    }
    private Boolean CheckLogin(HttpServletRequest request){
        var c = request.getCookies();
        if(c==null){
            return false;
        }
        for (var cookie: c) {
            if(cookie.getName().equals("token") && cookie.getValue().equals("123")){
                return true;
            }
        }
        return  false;
    }
    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@RequestParam("account") String acc,
                                        @RequestParam("pass") String pass,
                                        HttpServletResponse response) {
        
        BasicDBObject criteria = new BasicDBObject();
        criteria.append("account", acc);
        criteria.append("pass", pass);
        var user = users.find(criteria).first();
        if(user == null){
            return Failed();
        }
        var token = new Cookie("token","123");
        token.setMaxAge(86400);
        response.addCookie(token);
        return Success();
    }
    @PostMapping(value = "/user/list",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayList<Object>> listUser(HttpServletRequest request, HttpServletResponse response) {
        
        var list = new ArrayList<Object>();
        if(!CheckLogin(request)){
            return new ResponseEntity<ArrayList<Object>>(list,HttpStatus.OK);
        }
        users.find().forEach((user)->{
            Document u = (Document) user;
            list.add(u.getString("account")+","+u.getString("displayname")+","+u.getString("donvi")+","+u.getString("ngaysinh"));
        });
        return new ResponseEntity<ArrayList<Object>>(list,HttpStatus.OK);
    }

    @PostMapping(value = "/user/update")
    public ResponseEntity<String> updateUser(@RequestParam("account") String acc,
                                             @RequestParam("displayname") String name,
                                             @RequestParam("ngaysinh") String ngaysinh,
                                             @RequestParam("donvi") String donvi,
                                             HttpServletRequest request, HttpServletResponse response) {
        
        if(!CheckLogin(request)){
            return Failed();
        }
        var user = users.find(eq("account",acc)).first();
        if(user == null){
            return Failed();
        }
        Bson update = Updates.combine(
                Updates.set("displayname",name),
                Updates.set("ngaysinh",ngaysinh),
                Updates.set("donvi",donvi)
        );
        users.findOneAndUpdate(eq("account",acc),update);
        return Success();
    }
    @PostMapping(value = "/user/delete")
    public ResponseEntity<String> deleteUser(@RequestParam("account") String acc,
                                             HttpServletRequest request, HttpServletResponse response) {
        
        if(!CheckLogin(request)){
            return Failed();
        }
        var user = users.find(eq("account",acc)).first();
        if(user == null){
            return new ResponseEntity<String>("false", HttpStatus.OK);
        }
        users.findOneAndDelete(eq("account",acc));
        return Success();
    }
    @PostMapping(value = "/user/add")
    public ResponseEntity<String> addUser(@RequestParam("account") String acc,
                                          @RequestParam("pass") String pass,
                                          @RequestParam("displayname") String name,
                                          @RequestParam("ngaysinh") String ngaysinh,
                                          @RequestParam("donvi") String donvi,
                                          HttpServletRequest request, HttpServletResponse response) {
        
        if(!CheckLogin(request)){
            return Failed();
        }
        var userexist = users.find(eq("account",acc)).first();
        if(userexist != null){
            return new ResponseEntity<String>("exist", HttpStatus.OK);
        }
        Document user = new Document();
        user.append("account", acc);
        user.append("pass", pass);
        user.append("displayname", name);
        user.append("ngaysinh", ngaysinh);
        user.append("donvi", donvi);
        users.insertOne(user);
        return Success();
    }
}
package com.springboot.MyTodoList.controller;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    //@CrossOrigin
    @GetMapping(value = "/users")
    public List<User> getAllUsers(){
        return userService.findAll();
    }

    //@CrossOrigin
    @GetMapping(value = "/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id){
        try{
            User user = userService.findById((long) id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    //@CrossOrigin
    @PostMapping(value = "/adduser")
    public ResponseEntity<User> addUser(@RequestBody User newUser) throws Exception{
        User dbUser = userService.save(newUser);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("location",""+dbUser.getId());
        responseHeaders.set("Access-Control-Expose-Headers","location");
        //URI location = URI.create(""+td.getID())

        return ResponseEntity.ok()
                .headers(responseHeaders).build();
    }
    //@CrossOrigin
    @PutMapping(value = "updateUser/{id}")
    public ResponseEntity<User> updateUser(@RequestBody User user, @PathVariable int id){
        try{
            user.setId((long) id);
            User dbUser = userService.save(user);
            return new ResponseEntity<>(dbUser,HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
    //@CrossOrigin
    @DeleteMapping(value = "deleteUser/{id}")
    public ResponseEntity<Boolean> deleteUser(@PathVariable("id") int id){
        try{
            userService.delete((long) id);
            return new ResponseEntity<>(true, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(false,HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping(value = "/unitTestAdd")
    public User test(){
        return new User();
    }


}

package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.LoginRequestDto;
import com.yutkubeygo.watchit.dto.LoginResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request)
    {
            UserResponseDto user = userService.createUser(request);

            //Eger varolan bir maille kayit olmak dednirse conflict uyarisi gonderir
            if(user == null)
                return ResponseEntity.status(HttpStatus.CONFLICT).build();

            return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping
    public List<UserResponseDto> getUsers()
    {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id)
    {
        UserResponseDto user = userService.getUserById(id);
        if(user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();


        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id,@Valid @RequestBody UserRequestDto newUser)
    {
        UserResponseDto updatedUser = userService.updateUser(id,newUser);

        if(updatedUser==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();//404

        return ResponseEntity.ok(updatedUser);//200 OK+Güncel veri
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id)
    {
        //Eğer kullanıcı silinemediyse böyle bir kullanıcı yoktur/bulunamamıştır
        if(!userService.deleteUserById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request)
    {
        LoginResponseDto logined = userService.login(request);

        if(logined == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(logined);
    }
}

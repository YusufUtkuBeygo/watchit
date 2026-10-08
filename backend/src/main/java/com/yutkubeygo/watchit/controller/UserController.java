package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.LoginRequestDto;
import com.yutkubeygo.watchit.dto.LoginResponseDto;
import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(@AuthenticationPrincipal Long userId)
    {
        UserResponseDto user = userService.getUserById(userId);
        if(user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id)
    {
        UserResponseDto user = userService.getUserById(id);
        if(user == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();


        return ResponseEntity.ok(user);
    }



    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateUser(@Valid @RequestBody UserRequestDto newUser, @AuthenticationPrincipal Long userId)
    {
        UserResponseDto updatedUser = userService.updateUser(userId,newUser);

        if(updatedUser==null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();//404

        return ResponseEntity.ok(updatedUser);//200 OK+Güncel veri
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUserById( @AuthenticationPrincipal Long userId)
    {
        //Eğer kullanıcı silinemediyse böyle bir kullanıcı yoktur/bulunamamıştır
        if(!userService.deleteUserById(userId))
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

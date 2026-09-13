package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.UserRequestDto;
import com.yutkubeygo.watchit.dto.UserResponseDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.service.UserService;
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
    public UserResponseDto createUser(@RequestBody UserRequestDto request)
    {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponseDto> getUsers()
    {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable Long id)
    {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserResponseDto updateUser(@PathVariable Long id,@RequestBody User newUser)
    {
        return userService.updateUser(id,newUser);
    }

    @DeleteMapping("/{id}")
    public void deleteUserById(@PathVariable Long id)
    {
        userService.deleteUserById(id);
    }
}

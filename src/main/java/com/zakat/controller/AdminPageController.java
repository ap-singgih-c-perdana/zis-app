package com.zakat.controller;

import com.zakat.entity.User;
import com.zakat.enums.UserRole;
import com.zakat.service.UserService;
import com.zakat.service.dto.UserCreateRequest;
import com.zakat.service.dto.UserUpdateRequest;
import com.zakat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class AdminPageController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping({"/user/list", "/admin/user"})
    public String listUser() {
        return "user-list";
    }

    @GetMapping("/user/list2")
    public String listUser2(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "user-list2";
    }

    @GetMapping("/user/add")
    public String showFormUser(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", new UserRole[]{UserRole.OPERATOR, UserRole.VIEWER});
        return "user-add";
    }

    @PostMapping("/user/add")
    public String addUser(RedirectAttributes redirectAttributes, User user) {
        if (user.getRole() == null || user.getRole() == UserRole.ADMIN) {
            user.setRole(UserRole.OPERATOR);
        }

        UserCreateRequest req = new UserCreateRequest(
                user.getUsername(), user.getEmail(), user.getPassword(), user.getRole()
        );
        userService.create(req);
        redirectAttributes.addFlashAttribute("successMessage", "User ditambahkan");
        return "redirect:/settings/users";
    }

    @GetMapping("/user/edit/{id}")
    public String showEditUser(@PathVariable UUID id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        model.addAttribute("roles", new UserRole[]{UserRole.OPERATOR, UserRole.VIEWER});
        return "user-add"; // reuse form template for edit
    }

    @PostMapping("/user/edit/{id}")
    public String editUser(@PathVariable UUID id, User userForm, RedirectAttributes redirectAttributes) {
        // build update request
        UserUpdateRequest req = new UserUpdateRequest(
                userForm.getUsername(), userForm.getEmail(), userForm.getPassword(), userForm.getRole(), userForm.isActive()
        );
        userService.update(id, req);
        redirectAttributes.addFlashAttribute("successMessage", "User diperbarui");
        return "redirect:/settings/users";
    }

}

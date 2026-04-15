package com.shacky.materialmanagement.controller;

import com.shacky.materialmanagement.auth.CurrentCustomerService;
import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final CurrentCustomerService currentCustomerService;

    public CommentController(CommentService commentService, CurrentCustomerService currentCustomerService) {
        this.commentService = commentService;
        this.currentCustomerService = currentCustomerService;
    }

    @PostMapping("/post")
    public String postComment(@RequestParam("content") String content,
                              RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomerService.getCurrentCustomer().orElse(null);
        try {
            commentService.postComment(customer, content);
            redirectAttributes.addFlashAttribute("successMessage", "Your comment has been posted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/my-orders";
    }

    @GetMapping("/comments")
    public String showAllComments(Model model) {
        model.addAttribute("comments", commentService.getAllComments());
        return "services";
    }

    @PostMapping("/{id}/like")
    @ResponseBody
    public String likeComment(@PathVariable("id") Long commentId) {
        return String.valueOf(commentService.likeComment(commentId));
    }
}

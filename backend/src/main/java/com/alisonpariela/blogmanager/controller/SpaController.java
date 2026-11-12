package com.alisonpariela.blogmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    /** The combined image serves the Vite build; client-side login routing survives refresh. */
    @GetMapping("/login")
    public String login() { return "forward:/index.html"; }
}

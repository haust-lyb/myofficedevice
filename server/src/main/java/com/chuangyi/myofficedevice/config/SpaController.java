package com.chuangyi.myofficedevice.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/flow"})
    public String index() {
        return "forward:/index.html";
    }
}

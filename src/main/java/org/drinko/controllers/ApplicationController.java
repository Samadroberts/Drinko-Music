package org.drinko.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class ApplicationController {
    @RequestMapping("/")
    public ResponseEntity defaultMapping() {
        return ResponseEntity.ok("BOT_UP");
    }
}

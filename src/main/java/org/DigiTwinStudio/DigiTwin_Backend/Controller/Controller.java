package org.DigiTwinStudio.DigiTwin_Backend.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {


    @GetMapping("/")
    public String Hello() {
        return "Hello World!";
    }

}

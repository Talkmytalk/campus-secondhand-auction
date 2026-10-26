package edu.campus.auction.web;

import edu.campus.auction.service.AuthService;
import edu.campus.auction.web.ApiModels.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody Credentials input) { return auth.register(input); }

    @PostMapping("/login")
    public LoginView login(@Valid @RequestBody Credentials input) { return auth.login(input); }

    @GetMapping("/me")
    public UserView me(@RequestHeader(value = "Authorization", required = false) String header) {
        return auth.me(header);
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String header) {
        auth.logout(header);
    }
}

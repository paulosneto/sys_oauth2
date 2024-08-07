package com.systouch.controller;

import com.systouch.domain.Role;
import com.systouch.domain.User;
import com.systouch.dto.LoginDTORequest;
import com.systouch.dto.LoginDTOResponse;
import com.systouch.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class TokenController {


    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public TokenController(JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder){
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    // Faz o login com os dados informados
    @PostMapping("/login")
    public ResponseEntity<LoginDTOResponse> login(@RequestBody LoginDTORequest request){
        // verifica no banco se existe algum usuario com o nome de usuario informado.
        Optional<User> user = this.userRepository.findByUserName(request.username());

        // Faz a configuração entre a senha informada pelo usuário e a
        // qual está cadastrada no bando de dados
        if(user.isEmpty() || !user.get().isLoginCorrect(request, bCryptPasswordEncoder)){
            throw new BadCredentialsException("Usuário ou senha invalido");
        }

        // Define o tempo de duração do token
        var now = Instant.now();
        var expiresIn = 300L; // Exatamente 5 minutos, mas poderia ser o tempo que quiser desde quando este seja em segundos

        // escopo: retorno da regra definida para o usuario logado
        var scopes = user.get().getRoles()
                              .stream()
                              .map(Role::getName)
                              .collect(Collectors.joining(""));


        // Criação de regras do JWT, passando o
        // issuer(tag)
        // subject(id do usuario)
        // issuedAt(exato momento)
        // expiresAt(tempo de expiração do token)
        // claim(regra do usuário(BASIC ou ADMIN))
        // build(fecha as configurações)
        var claims = JwtClaimsSet.builder()
                .issuer("backend")
                .subject(user.get().getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .build();

        // Recupera o token criptografado e envia para a requisição de login como resposta;
        var jetValues = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        // Retorna o token ja montado com o tempo de expiração definido(5 minutos)
        return ResponseEntity.ok(new LoginDTOResponse(jetValues, expiresIn));




    }

}

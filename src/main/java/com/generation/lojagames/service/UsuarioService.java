package com.generation.lojagames.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.generation.lojagames.model.Usuario;
import com.generation.lojagames.model.UsuarioLogin;
import com.generation.lojagames.repository.UsuarioRepository;
import com.generation.lojagames.security.JwtService;

@Service
public class UsuarioService {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

	public Optional<Usuario> cadastrarUsuario(Usuario usuario) {

		if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent())
			return Optional.empty();
		
		if(!maiorQue18(usuario))
			return Optional.empty();

		usuario.setSenha(criptografarSenha(usuario.getSenha()));

		return Optional.of(usuarioRepository.save(usuario));
	
	}

	public Optional<Usuario> atualizarUsuario(Usuario usuario) {
		
		if(usuarioRepository.findById(usuario.getId()).isPresent()) {

			Optional<Usuario> buscaUsuario = usuarioRepository.findByUsuario(usuario.getUsuario());
			
			if(!maiorQue18(usuario))
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível cadastrar idade menor que 18 anos!", null);

			if ( (buscaUsuario.isPresent()) && ( buscaUsuario.get().getId() != usuario.getId()))
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null);

			usuario.setSenha(criptografarSenha(usuario.getSenha()));

			return Optional.ofNullable(usuarioRepository.save(usuario));
			
		}

		return Optional.empty();
	
	}	

	public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {
        
        // Gera o Objeto de autenticação
		var credenciais = new UsernamePasswordAuthenticationToken(usuarioLogin.get().getUsuario(), usuarioLogin.get().getSenha());
		
        // Autentica o Usuario
		Authentication authentication = authenticationManager.authenticate(credenciais);
        
        // Se a autenticação foi efetuada com sucesso
		if (authentication.isAuthenticated()) {

            // Busca os dados do usuário
			Optional<Usuario> usuario = usuarioRepository.findByUsuario(usuarioLogin.get().getUsuario());

            // Se o usuário foi encontrado
			if (usuario.isPresent()) {

                // Preenche o Objeto usuarioLogin com os dados encontrados 
			   usuarioLogin.get().setId(usuario.get().getId());
                usuarioLogin.get().setNome(usuario.get().getNome());
                usuarioLogin.get().setDataNascimento(usuario.get().getDataNascimento());
                usuarioLogin.get().setToken(gerarToken(usuarioLogin.get().getUsuario()));
                usuarioLogin.get().setSenha("");
				
                 // Retorna o Objeto preenchido
			   return usuarioLogin;
			
			}

        } 
            
		return Optional.empty();

    }

	private String criptografarSenha(String senha) {

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		
		return encoder.encode(senha);

	}
	
	private boolean maiorQue18(Usuario usuario) {
		
		LocalDate dataAtual = LocalDateTime.now().toLocalDate();
		
		Period idade = Period.between(usuario.getDataNascimento().toLocalDate(), dataAtual);
		
		if(idade.getYears()<18)
			return false;
		
		return true;
		
	}

	private String gerarToken(String usuario) {
		return "Bearer " + jwtService.generateToken(usuario);
	}

}

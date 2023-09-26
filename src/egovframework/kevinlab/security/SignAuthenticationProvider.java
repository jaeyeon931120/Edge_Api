package egovframework.kevinlab.security;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class SignAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(SignAuthenticationProvider.class);

    @Autowired
    SignUserService signUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaltSource saltSource;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        SignUser user;
        Collection<? extends GrantedAuthority> authorities;

        try {
            user = signUserService.loadUserByUsername(username);

            String hashedPassword = passwordEncoder.encodePassword(password, saltSource.getSalt(user));
            logger.info("username : " + username + " / password : " + password + " / hash password : " + hashedPassword);
            logger.info("username : " + user.getUsername() + " / password : " + user.getPassword()+ " / sSiteId : " + user.getsSiteId());

            if (!password.equals(user.getPassword())) throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");

            authorities = user.getAuthorities();

        } catch(UsernameNotFoundException e) {
            logger.info(e.toString());
            throw new UsernameNotFoundException(e.getMessage());
        } catch(BadCredentialsException e) {
            logger.info(e.toString());
            throw new BadCredentialsException(e.getMessage());
        } catch(Exception e) {
            logger.info(e.toString());
            throw new RuntimeException(e.getMessage());
        }

        return new UsernamePasswordAuthenticationToken(user, password, authorities);
    }

    @Override
    public boolean supports(Class<?> arg0) {
        return true;
    }
}
package egovframework.kevinlab.security;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import egovframework.kevinlab.dto.UserDto;


@Service
public class SignUserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(SignUserService.class);

	
    @Override
    public SignUser loadUserByUsername(final String username) throws UsernameNotFoundException {

        logger.info("username : " + username);

        UserDto dto = new UserDto();

        // 만약 데이터가 없을 경우 익셉션
        if (dto == null) throw new UsernameNotFoundException("접속자 정보를 찾을 수 없습니다.");

        String authType = dto.getsAuth();

        SignUser user = new SignUser();
        user.setUsername(dto.getsUserId());
        user.setPassword(dto.getsPwd());
        user.setsSiteId(dto.getsSiteId());
        user.setUserautn(authType);

        SignRole role = new SignRole();
        role.setName("ROLE_"+authType);

        List<SignRole> roles = new ArrayList<SignRole>();
        roles.add(role);
        user.setAuthorities(roles);

        return user;
    }
}

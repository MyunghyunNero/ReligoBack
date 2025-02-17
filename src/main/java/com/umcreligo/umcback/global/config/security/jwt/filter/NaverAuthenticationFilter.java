package com.umcreligo.umcback.global.config.security.jwt.filter;

import com.umcreligo.umcback.domain.user.domain.User;
import com.umcreligo.umcback.domain.user.repository.UserRepository;
import com.umcreligo.umcback.global.config.security.jwt.KakaoOAuthService;
import com.umcreligo.umcback.global.config.security.jwt.KakaoProfile;
import com.umcreligo.umcback.global.config.security.jwt.NaverOAuthService;
import com.umcreligo.umcback.global.config.security.jwt.NaverProfile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

import static com.umcreligo.umcback.global.config.security.jwt.JwtService.TOKEN_HEADER_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
public class NaverAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final NaverOAuthService naverOAuthService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;

    private final EntityManagerFactory enf;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        EntityManager em = enf.createEntityManager();
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_HEADER_PREFIX)) {
            throw new AuthenticationCredentialsNotFoundException("AccessToken이 존재하지 않습니다.");
        }
        String accessToken = authorizationHeader.substring(TOKEN_HEADER_PREFIX.length());
        NaverProfile naverProfile = naverOAuthService.getKakaoProfileWithAccessToken(accessToken);
        String OauthId = (String) naverProfile.getResponse().get("id");
        if(StringUtils.isBlank(OauthId)) {
            throw new UsernameNotFoundException("가입된 회원이 존재하지 않습니다.");
        }
        //kakaoId랑 겹칠 가능성이 있기에 뒤에 naver를 붙여줬습니다.
        OauthId +="naver";
        Optional<User> option = userRepository.findByAuthId(OauthId);
        if(!option.isPresent()){
            //디비 저장하기 위한 transaction 만들기
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            String password = "naver";
            String encodedPassword = passwordEncoder.encode(password);
            String email =((String)naverProfile.getResponse().get("email")==null)?"":(String)naverProfile.getResponse().get("email");
            String gender =((String)naverProfile.getResponse().get("gender")==null)?"":(String)naverProfile.getResponse().get("gender");
            User user = new User();
            user.setEmail(email);
            user.setAuthId(OauthId);
            user.setPassword(encodedPassword);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setSocialType(User.SocialType.NAVER);
            user.setGender(gender);
            userRepository.save(user);
            transaction.commit();

        }
        Authentication auth = new UsernamePasswordAuthenticationToken(OauthId,"naver" );
        return authenticationManager.authenticate(auth);
    }
}

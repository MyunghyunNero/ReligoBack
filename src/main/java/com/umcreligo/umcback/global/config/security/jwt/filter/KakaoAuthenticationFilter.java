package com.umcreligo.umcback.global.config.security.jwt.filter;

import com.umcreligo.umcback.domain.user.domain.User;
import com.umcreligo.umcback.domain.user.repository.UserRepository;
import com.umcreligo.umcback.global.config.security.jwt.KakaoOAuthService;
import com.umcreligo.umcback.global.config.security.jwt.KakaoProfile;
import lombok.RequiredArgsConstructor;
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
public class KakaoAuthenticationFilter extends UsernamePasswordAuthenticationFilter {


    private final KakaoOAuthService kakaoOAuthService;
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
        KakaoProfile kakaoProfile = kakaoOAuthService.getKakaoProfileWithAccessToken(accessToken);
        Long kakaoId = kakaoProfile.getId();
        if(kakaoId==null) {
            throw new UsernameNotFoundException("요청 값이 없습니다.");
        }
        String OauthId = Long.toString(kakaoId);
        //네이버Id 랑 겹칠 수도 있기에 뒤에 kakao를 붙였습니다.
        OauthId += "kakao";
        Optional<User> option = userRepository.findByAuthId(OauthId);
        if(!option.isPresent()){
            //디비 저장하기 위한 transaction 만들기
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            String password = "kakao";
            String encodedPassword = passwordEncoder.encode(password);
            String email = ((String) kakaoProfile.getKakao_account().get("email")==null) ? "":(String) kakaoProfile.getKakao_account().get("email");
            String gender = ((String) kakaoProfile.getKakao_account().get("gender")==null)? "":(String) kakaoProfile.getKakao_account().get("gender");
            User user = new User();
            user.setEmail(email);
            user.setAuthId(OauthId);
            user.setPassword(encodedPassword);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setSocialType(User.SocialType.KAKAO);
            user.setGender(gender);
            userRepository.save(user);
            transaction.commit();

        }
        Authentication auth = new UsernamePasswordAuthenticationToken(OauthId,"kakao" );
        return authenticationManager.authenticate(auth);
    }
}

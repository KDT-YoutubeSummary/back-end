package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            // 이미 가입된 사용자인 경우
            user = userOptional.get();
            user.setUserName(name); // 구글 프로필 이름으로 업데이트 (선택사항)
        } else {
            // 처음으로 소셜 로그인하는 사용자인 경우
            user = new User();
            user.setEmail(email);
            user.setUserName(name);

            // ✨ 수정된 부분: User 엔티티에 provider 필드가 없으므로 해당 라인을 제거했습니다.
            // user.setProvider("google");

            // 소셜 로그인 사용자는 직접 비밀번호로 로그인하지 않으므로, 임의의 값을 암호화하여 저장합니다.
            user.setPasswordHash(passwordEncoder.encode("SocialLoginUserPassword"));
        }
        userRepository.save(user);

        // 인증 정보를 담은 UserPrincipal 객체를 반환합니다.
        return new UserPrincipal(user, attributes);
    }
}

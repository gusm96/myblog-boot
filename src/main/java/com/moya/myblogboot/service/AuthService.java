package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.*;
import com.moya.myblogboot.exception.*;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String REFRESH_TOKEN_KEY = "refreshToken:";
    private static final String REFRESH_TOKEN_HASH_KEY = "tokenValue";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    private final RedisTemplate<String, String> redisTemplate;

    // 회원 가입
    @Transactional

    public String memberJoin(MemberJoinReqDto memberJoinReqDto) {
        // 아이디 중복 체크
        validateUsername(memberJoinReqDto.getUsername());
        // 비밀번호 암호화
        memberJoinReqDto.passwordEncode(passwordEncoder.encode(memberJoinReqDto.getPassword()));
        // Member Entity 생성
        Member newMember = memberJoinReqDto.toEntity();
        // Member Persist
        try {
            Long result = memberRepository.save(newMember);
            if (result > 0) {
                return "회원가입을 성공했습니다.";
            } else {
                throw new PersistenceException("회원가입을 실패했습니다.");
            }
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.");
        }
    }

    // 회원 아이디 유효성 검사.
    private void validateUsername(String username) {
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new DuplicateKeyException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 로그인
    public TokenResDto memberLogin(LoginReqDto loginReqDto) {
        // username으로 회원 찾기
        Member findMember = memberRepository.findByUsername(loginReqDto.getUsername()).orElseThrow(()
                -> new UsernameNotFoundException("존재하지 않는 아이디 입니다."));
        // password 비교
        if (!passwordEncoder.matches(loginReqDto.getPassword(), findMember.getPassword()))
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        // Token 생성
        Token newToken = createToken(findMember);
        // Redis RefreshToken 저장.
        Long refreshTokenKey = saveRefreshTokenToRedis(newToken.getRefresh_token(), findMember.getId());

        return TokenResDto.builder()
                .access_token(newToken.getAccess_token())
                .refresh_token_key(refreshTokenKey)
                .build();
    }

    public Member retrieveMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(()
                -> new EntityNotFoundException("회원이 존재하지 않습니다."));
    }

    public String reissuingAccessToken(Long refreshTokenKey) {
        // Refresh Token 찾는다.
        String findRefreshToken = retrieveRefreshTokenByKey(refreshTokenKey);
        // Refresh Token 검증.
        try {
            JwtUtil.validateToken(findRefreshToken, secret);
        } catch (ExpiredTokenException e) {
            // RefreshToken 만료시 Data 삭제.
            deleteRefreshToken(refreshTokenKey);
            throw new ExpiredRefreshTokenException("토큰이 만료되었습니다.");
        }
        // Access Token 재발급
        return JwtUtil.reissuingToken(getTokenInfo(findRefreshToken), secret, accessTokenExpiration);
    }

    private String retrieveRefreshTokenByKey(Long refreshTokenKey) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        String key = REFRESH_TOKEN_KEY + refreshTokenKey;
        Object refreshToken = hashOperations.get(key, REFRESH_TOKEN_HASH_KEY);
        if (refreshToken == null) {
            return null;
        }
        return refreshToken.toString();
    }

    private Token createToken(Member member) {
        return JwtUtil.createToken(member, secret, accessTokenExpiration, refreshTokenExpiration);
    }

    @Transactional
    public boolean logout(Long refreshTokenKey) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        String key = REFRESH_TOKEN_KEY + refreshTokenKey;
        if (hashOperations.get(key, REFRESH_TOKEN_HASH_KEY) == null) {
            return true;
        }
        try {
            hashOperations.delete(key, REFRESH_TOKEN_HASH_KEY);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("로그아웃 중 에러가 발생했습니다");
        }
    }

    public TokenInfo getTokenInfo(String token) {
        return JwtUtil.getTokenInfo(token, secret);

    }

    @Transactional
    public void deleteRefreshToken(Long refreshTokenKey) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        String key = REFRESH_TOKEN_KEY + refreshTokenKey;
        if (hashOperations.get(key, REFRESH_TOKEN_HASH_KEY) != null)
            hashOperations.delete(key, REFRESH_TOKEN_HASH_KEY);
    }
    @Transactional
    public Long saveRefreshTokenToRedis(String refreshToken, Long memberId) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        String key = REFRESH_TOKEN_KEY + memberId;
        // 이미 토큰이 저장되어 있다면, 삭제하고 새로 저장.
        if (hashOperations.get(key, REFRESH_TOKEN_HASH_KEY) == null) {
            hashOperations.put(key, REFRESH_TOKEN_HASH_KEY, refreshToken);
        } else {
            hashOperations.delete(key, REFRESH_TOKEN_HASH_KEY);
            hashOperations.put(key, REFRESH_TOKEN_HASH_KEY, refreshToken);
        }
        return memberId;
    }
}

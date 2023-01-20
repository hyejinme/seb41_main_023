package com.newyear.mainproject.refresh;

import com.newyear.mainproject.exception.BusinessLogicException;
import com.newyear.mainproject.exception.ExceptionCode;
import com.newyear.mainproject.security.jwt.JwtTokenizer;
import com.newyear.mainproject.security.logout.RedisUtil;
import com.newyear.mainproject.security.utils.CustomAuthorityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/token")
@Validated
public class TokenController {

    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils authorityUtils;
    private final RedisUtil redisUtils;

    @PostMapping("/reissue")
    public ResponseEntity reissueToken(@RequestHeader("Refresh") @NotBlank String refreshToken,
                                       HttpServletResponse response) {

        String encodeBase64SecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        //토큰 유효성 검증
        Jws<Claims> claims = jwtTokenizer.getClaims(refreshToken, encodeBase64SecretKey);

        Map<String, Object> map = new HashMap<>();
        String email = claims.getBody().getSubject();

        //redis에 refresh token 이 없으면 예외 처리
        if (redisUtils.get(email) == null) {
            throw new BusinessLogicException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }

        List<String> roles = authorityUtils.createRoles(email);
        map.put("username", email);
        map.put("roles", roles);

        //토큰 재발급
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMinutes());
        String newAccessToken = jwtTokenizer.generateAccessToken(map, email, expiration, encodeBase64SecretKey);
        response.setHeader("Authorization", "Bearer " + newAccessToken);

        return new ResponseEntity(HttpStatus.OK);

    }
}

package com.recallrelay.security;

import com.recallrelay.employee.Employee;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-seconds:28800}") long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public Token issue(Employee employee) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("recallrelay")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(employee.getId()))
                .claim("employeeId", employee.getId())
                .claim("employeeLoginId", employee.getEmployeeLoginId())
                .claim("organizationId", employee.getOrganizationId())
                .claim("role", employee.getRole().name());

        if (employee.getDealershipId() != null) {
            claims.claim("dealershipId", employee.getDealershipId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String tokenValue = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims.build())
        ).getTokenValue();

        return new Token(tokenValue, expirationSeconds);
    }

    public record Token(String value, long expiresIn) {
    }
}

# Authentication Documentation

This document describes the stateless JWT authentication implementation in this Spring Boot backend starter template.

## Overview

The authentication system uses **stateless JWT (JSON Web Tokens)** with Spring Security. There are no server-side sessions, refresh tokens, or database-backed authentication state.

## Authentication Flow

### 1. User Registration

```
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "securePassword123"
}
```

**Server-side process:**
1. Validate request DTO (`@Valid` + Jakarta Bean Validation)
2. Check if email already exists (`UserRepository.existsByEmail`)
3. Hash password with BCrypt (`PasswordEncoder.encode`)
4. Create User entity with `Role.USER`
5. Save to database
6. Generate JWT token for the new user
7. Return `AuthResponse` with token and user info

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "role": "USER",
      "enabled": true,
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  },
  "error": null,
  "requestId": "uuid",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2. User Login

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "securePassword123"
}
```

**Server-side process:**
1. Validate request DTO
2. `AuthenticationManager.authenticate()` with `UsernamePasswordAuthenticationToken`
3. `DaoAuthenticationProvider` loads user via `CustomUserDetailsService`
4. BCrypt password verification (`PasswordEncoder.matches`)
5. On success: generate JWT token
6. Return `AuthResponse` with token and user info

**Response:** Same structure as registration.

### 3. Authenticated Requests

```
GET /api/projects
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Server-side process:**
1. `JwtAuthenticationFilter` extracts JWT from `Authorization` header
2. Validates token signature, expiration, issuer
3. Extracts email from token subject
4. Loads `UserDetails` via `CustomUserDetailsService`
5. Creates `Authentication` and sets in `SecurityContextHolder`
6. Controller executes with authenticated principal

### 4. Logout

**Stateless JWT has no server-side logout.** Client simply discards the token.

For immediate invalidation, implement token blacklist (not included by default).

## Implementation Details

### JWT Token Structure

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload (Claims):**
```json
{
  "sub": "john.doe@example.com",
  "iss": "backend-starter",
  "iat": 1705312200,
  "exp": 1705398600
}
```

- `sub`: User email (username)
- `iss`: Configured issuer (`app.jwt.issuer`)
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp (configured via `app.jwt.expiration`)

**Signature:** HMAC-SHA256 with secret key

### Configuration

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}          # Base64-encoded, min 256 bits
    expiration: 86400000           # 24 hours in milliseconds
    issuer: backend-starter
```

### Password Hashing

- Algorithm: BCrypt (via Spring Security's `BCryptPasswordEncoder`)
- Default strength: 10 rounds
- Automatic salt generation
- Verified with `passwordEncoder.matches(rawPassword, encodedPassword)`

### UserDetails Implementation

`User` entity implements `UserDetails`:

```java
@Entity
public class User implements UserDetails {
    // ... fields ...

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;  // Email used as username
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
```

### Custom UserDetailsService

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userService.findEntityByEmail(email);
    }
}
```

## Spring Security Configuration

### SecurityFilterChain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(customAccessDeniedHandler)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/actuator/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### Key Components

1. **JwtAuthenticationFilter** - Extracts and validates JWT, sets SecurityContext
2. **JwtAuthenticationEntryPoint** - Handles 401 responses (unauthenticated)
3. **CustomAccessDeniedHandler** - Handles 403 responses (unauthorized)
4. **DaoAuthenticationProvider** - Uses UserDetailsService + PasswordEncoder

## Authorization

### Role-Based Access Control

Two roles defined in `User.Role` enum:
- `USER` - Default role for registered users
- `ADMIN` - Administrative access

### Method-Level Security

```java
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig { ... }

// Controller usage:
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { ... }

@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) { ... }
```

### Accessing Current User

In controllers/services:

```java
// From SecurityContext
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
User user = (User) auth.getPrincipal();  // User entity (implements UserDetails)
String email = user.getEmail();
Long userId = user.getId();
Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
```

## Protected Endpoints

### Public Endpoints (No Authentication)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /actuator/health`
- `GET /actuator/info`

### Authenticated Endpoints (Any Valid JWT)
- `GET /api/users/me`
- `GET /api/users/{id}` (self or ADMIN)
- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{id}`
- `PUT /api/projects/{id}`

### Admin-Only Endpoints
- `GET /api/users` (list all users)
- `DELETE /api/projects/{id}`
- `GET /actuator/**` (except health/info)

## Token Management

### Token Generation

```java
// In JwtTokenProvider
public String generateToken(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtExpirationMs);

    return Jwts.builder()
        .subject(userDetails.getUsername())
        .issuer(issuer)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
}
```

### Token Validation

```java
public boolean validateToken(String token) {
    try {
        parseClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException ex) {
        return false;
    }
}
```

### Token Parsing

```java
private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .requireIssuer(issuer)
        .build()
        .parseSignedClaims(token)
        .getPayload();
}

public String getEmailFromToken(String token) {
    return parseClaims(token).getSubject();
}
```

## Security Considerations

### What IS Implemented
- ✅ Stateless JWT authentication
- ✅ BCrypt password hashing
- ✅ Token signature verification (HS256)
- ✅ Token expiration validation
- ✅ Issuer validation
- ✅ Role-based authorization
- ✅ Method-level security (`@PreAuthorize`)
- ✅ Secure defaults (CSRF disabled for stateless, CORS configured)

### What IS NOT Implemented (by design)

| Feature | Reason |
|---------|--------|
| Refresh Tokens | Adds complexity, token rotation, storage |
| Token Blacklist | Requires storage (Redis/DB), defeats statelessness |
| Access/Refresh Hybrid | Two token types to manage |
| CSRF Tokens | Not needed for stateless JWT (no cookies) |
| Session Management | Stateless by design |
| Database Auth Sessions | Contradicts stateless architecture |
| OAuth2/OIDC | Out of scope for starter template |

### Adding Refresh Tokens (if needed)

If your application requires refresh tokens:

1. Add `RefreshToken` entity with user reference, token hash, expiry
2. Create `RefreshTokenRepository` and `RefreshTokenService`
3. Modify `AuthService.login()` to issue both access + refresh tokens
4. Add `POST /api/auth/refresh` endpoint
5. Implement token rotation (invalidate old, issue new)
6. Add cleanup job for expired refresh tokens

## Testing Authentication

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock UserService userService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthMapper authMapper;

    @Test
    void register_ShouldCreateUserAndReturnToken() { ... }
    @Test
    void login_WithValidCredentials_ShouldReturnToken() { ... }
}
```

### Controller Tests

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;

    @Test
    @WithAnonymousUser
    void register_WithValidRequest_ShouldReturnAuthResponse() { ... }

    @Test
    @WithMockUser
    void login_WithValidCredentials_ShouldReturnAuthResponse() { ... }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthIntegrationTest extends BaseIntegrationTest {
    @Autowired TestRestTemplate restTemplate;

    @Test
    void fullAuthFlow_RegisterLoginAccessProtected() { ... }
}
```

## Configuration for Different Environments

### Development

```yaml
# application-dev.yml
app:
  jwt:
    secret: dev-secret-change-in-production-use-strong-secret
  security:
    cors:
      allowed-origins: http://localhost:3000,http://localhost:5173
```

### Production

```yaml
# application-prod.yml
app:
  jwt:
    secret: ${JWT_SECRET}  # Must be set via environment variable
  security:
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

**Production Requirements:**
- JWT secret: 256+ bits, base64-encoded, rotated periodically
- HTTPS only (configure reverse proxy/TLS termination)
- Short token expiry (15-30 min) with refresh tokens if needed
- Secure CORS origins (no wildcards)
- Rate limiting on auth endpoints

## Common Issues and Solutions

### Token Expired
- Client receives 401 with "UNAUTHORIZED"
- Client should redirect to login
- Consider implementing silent refresh if using refresh tokens

### Invalid Signature
- Check JWT secret matches between environments
- Ensure secret is base64-encoded
- Verify no whitespace/newlines in secret

### CORS Errors
- Verify `allowed-origins` includes frontend origin
- Check `allow-credentials: true` for cookie-based auth (not used here)
- Ensure preflight OPTIONS requests work

### Authentication Not Working
- Verify `JwtAuthenticationFilter` is registered before `UsernamePasswordAuthenticationFilter`
- Check `SecurityContextHolder` is populated in filter chain
- Ensure `@EnableMethodSecurity` is on configuration class
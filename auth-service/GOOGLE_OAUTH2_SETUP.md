# Google OAuth2 Login Setup Guide

## Overview
This auth-service now supports Google OAuth2 login using Spring Security OAuth2 Client. Users can sign in with their Google account, and the system automatically creates a new user if they don't exist.

---

## 📋 Table of Contents
1. [Google Cloud Console Setup](#google-cloud-console-setup)
2. [Environment Configuration](#environment-configuration)
3. [OAuth2 Flow](#oauth2-flow)
4. [Frontend Integration](#frontend-integration)
5. [Testing the Integration](#testing-the-integration)
6. [Security Considerations](#security-considerations)
7. [Troubleshooting](#troubleshooting)

---

## 🚀 Google Cloud Console Setup

### Step 1: Create a Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Select a project"** → **"New Project"**
3. Enter project name: `AutoNova-Auth` (or your preferred name)
4. Click **"Create"**

### Step 2: Enable Google+ API (optional but recommended)
1. In the left sidebar, go to **"APIs & Services"** → **"Library"**
2. Search for **"Google+ API"**
3. Click **"Enable"**

### Step 3: Create OAuth2 Credentials
1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"Create Credentials"** → **"OAuth client ID"**
3. If prompted, configure the **OAuth consent screen** first:
   - User Type: **External** (for testing)
   - App name: `AutoNova`
   - User support email: Your email
   - Developer contact email: Your email
   - Click **"Save and Continue"** through the steps

4. After consent screen setup, create OAuth client ID:
   - Application type: **Web application**
   - Name: `AutoNova Auth Service`
   - **Authorized JavaScript origins**:
     ```
     http://localhost:8081
     http://localhost:3000
     ```
   - **Authorized redirect URIs**:
     ```
     http://localhost:8081/login/oauth2/code/google
     ```
   - Click **"Create"**

5. **Copy the credentials** shown:
   - Client ID: `XXXXXXX.apps.googleusercontent.com`
   - Client secret: `XXXXXXXXXXXXXXXX`

---

## 🔧 Environment Configuration

### Option 1: Environment Variables (Recommended for Production)

**Windows PowerShell:**
```powershell
$env:GOOGLE_CLIENT_ID="YOUR_CLIENT_ID_HERE"
$env:GOOGLE_CLIENT_SECRET="YOUR_CLIENT_SECRET_HERE"
```

**Windows Command Prompt:**
```cmd
set GOOGLE_CLIENT_ID=YOUR_CLIENT_ID_HERE
set GOOGLE_CLIENT_SECRET=YOUR_CLIENT_SECRET_HERE
```

**Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID="YOUR_CLIENT_ID_HERE"
export GOOGLE_CLIENT_SECRET="YOUR_CLIENT_SECRET_HERE"
```

### Option 2: application.properties (Development Only)

⚠️ **Warning:** Never commit real credentials to version control!

Edit `src/main/resources/application.properties`:
```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID_HERE
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET_HERE
```

### Option 3: External Configuration File

Create `application-local.properties` (add to .gitignore):
```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_ACTUAL_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_ACTUAL_SECRET
```

Run with: `mvn spring-boot:run -Dspring.profiles.active=local`

---

## 🔄 OAuth2 Flow

### How It Works

```
1. User clicks "Login with Google" button on frontend
   ↓
2. Frontend redirects to: http://localhost:8081/oauth2/authorization/google
   ↓
3. Spring Security redirects to Google login page
   ↓
4. User logs in with Google credentials
   ↓
5. Google redirects back to: http://localhost:8081/login/oauth2/code/google?code=...
   ↓
6. OAuth2LoginSuccessHandler processes the authentication
   ↓
7. OAuth2Service finds or creates user in database
   ↓
8. System generates JWT access token (1h) + refresh token (7d)
   ↓
9. User redirected to: http://localhost:3000/oauth2/callback?token=JWT&refreshToken=REFRESH
   ↓
10. Frontend extracts tokens and stores them (localStorage/sessionStorage)
```

### User Creation Logic

When a new user logs in via Google:
- **Email:** From Google profile (verified)
- **Name:** From Google profile
- **First Name:** From Google profile
- **Last Name:** From Google profile
- **Profile Picture:** Google profile picture URL
- **Password:** Set to `"OAUTH2_USER_NO_PASSWORD"` (cannot use regular login)
- **Role:** Assigned `CUSTOMER` by default
- **Email Verified:** Automatically `true` (verified by Google)
- **Enabled:** Automatically `true`

---

## 💻 Frontend Integration

### React Example

#### 1. Create OAuth2 Login Button Component

```jsx
// components/GoogleLoginButton.jsx
import React from 'react';

const GoogleLoginButton = () => {
  const handleGoogleLogin = () => {
    // Redirect to backend OAuth2 endpoint
    window.location.href = 'http://localhost:8081/oauth2/authorization/google';
  };

  return (
    <button 
      onClick={handleGoogleLogin}
      className="google-login-btn"
    >
      <img src="/google-icon.png" alt="Google" />
      Sign in with Google
    </button>
  );
};

export default GoogleLoginButton;
```

#### 2. Create OAuth2 Callback Handler

```jsx
// pages/OAuth2Callback.jsx
import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

const OAuth2Callback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    // Extract tokens from URL query parameters
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');
    const error = searchParams.get('error');

    if (error) {
      console.error('OAuth2 login failed:', error);
      alert('Login failed. Please try again.');
      navigate('/login');
      return;
    }

    if (token && refreshToken) {
      // Store tokens (same as regular login)
      localStorage.setItem('accessToken', token);
      localStorage.setItem('refreshToken', refreshToken);

      // Redirect to dashboard or home
      navigate('/dashboard');
    } else {
      console.error('Tokens not found in callback');
      navigate('/login');
    }
  }, [searchParams, navigate]);

  return (
    <div style={{ textAlign: 'center', marginTop: '50px' }}>
      <h2>Completing login...</h2>
      <p>Please wait while we sign you in.</p>
    </div>
  );
};

export default OAuth2Callback;
```

#### 3. Add Route for Callback Page

```jsx
// App.jsx or routes configuration
import OAuth2Callback from './pages/OAuth2Callback';

// In your router setup:
<Route path="/oauth2/callback" element={<OAuth2Callback />} />
```

#### 4. Use the Google Login Button

```jsx
// pages/Login.jsx
import GoogleLoginButton from '../components/GoogleLoginButton';

const Login = () => {
  return (
    <div>
      <h2>Login</h2>
      
      {/* Regular email/password login form */}
      <form>
        <input type="email" placeholder="Email" />
        <input type="password" placeholder="Password" />
        <button type="submit">Login</button>
      </form>

      <div className="divider">OR</div>

      {/* Google OAuth2 login */}
      <GoogleLoginButton />
    </div>
  );
};
```

### Angular Example

```typescript
// google-login.component.ts
export class GoogleLoginComponent {
  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8081/oauth2/authorization/google';
  }
}

// oauth2-callback.component.ts
export class OAuth2CallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const refreshToken = params['refreshToken'];
      
      if (token && refreshToken) {
        this.authService.storeTokens(token, refreshToken);
        this.router.navigate(['/dashboard']);
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}
```

---

## 🧪 Testing the Integration

### 1. Start the Backend
```bash
cd auth-service
.\mvnw.cmd spring-boot:run
```

Verify it's running: http://localhost:8081/actuator/health

### 2. Test OAuth2 Endpoint Directly

Open browser and navigate to:
```
http://localhost:8081/oauth2/authorization/google
```

You should be redirected to Google's login page.

### 3. Complete Login Flow

1. Enter Google credentials
2. Grant permissions to the app
3. You'll be redirected to: `http://localhost:3000/oauth2/callback?token=...&refreshToken=...`
4. Copy the tokens from the URL

### 4. Verify User Created

Check your database:
```sql
SELECT * FROM users WHERE email = 'your-google-email@gmail.com';
```

You should see:
- New user record
- `password` field: `"OAUTH2_USER_NO_PASSWORD"`
- `email_verified`: `true`
- `enabled`: `true`
- `role`: `ROLE_CUSTOMER`

### 5. Test JWT Token

Use the access token from the callback URL:
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8081/api/auth/user-info
```

Expected response:
```json
{
  "id": 123,
  "email": "your-google-email@gmail.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "ROLE_CUSTOMER"
}
```

### 6. Test Refresh Token

```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'
```

Expected response:
```json
{
  "accessToken": "NEW_ACCESS_TOKEN",
  "refreshToken": "SAME_REFRESH_TOKEN",
  "userInfo": { ... }
}
```

---

## 🔒 Security Considerations

### Password Handling
- OAuth2 users have password: `"OAUTH2_USER_NO_PASSWORD"`
- They **cannot** use regular email/password login
- They **cannot** use the password reset feature
- Frontend should hide password-related features for OAuth2 users

### Email Verification
- Google OAuth2 users are **automatically email-verified**
- No need to send verification emails
- `emailVerified` flag is set to `true` on creation

### Role Assignment
- New OAuth2 users get **ROLE_CUSTOMER** by default
- Admin users must be created manually (not via OAuth2)
- Consider adding OAuth2 provider tracking to User entity

### Token Security
- Access tokens: **1 hour** expiry
- Refresh tokens: **7 days** expiry
- Same token pattern as regular login
- Logout revokes refresh tokens

### Profile Picture
- Google provides profile picture URL
- Store as `profilePictureUrl` in User entity (if you add this field)
- URL may become invalid if user changes Google settings

### Recommended Enhancements

1. **Add OAuth Provider Field to User Entity**
```java
@Entity
public class User {
    // ... existing fields
    
    @Column(name = "oauth_provider")
    private String oauthProvider; // "GOOGLE", "LOCAL", etc.
    
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;
}
```

2. **Prevent Password Reset for OAuth2 Users**
```java
// In PasswordResetService
if ("OAUTH2_USER_NO_PASSWORD".equals(user.getPassword())) {
    throw new RuntimeException("OAuth2 users cannot reset password");
}
```

3. **Add Multiple OAuth2 Providers**
- Facebook: `spring.security.oauth2.client.registration.facebook`
- GitHub: `spring.security.oauth2.client.registration.github`
- Microsoft: `spring.security.oauth2.client.registration.azure`

---

## 🐛 Troubleshooting

### Issue: "redirect_uri_mismatch" Error

**Cause:** Redirect URI in code doesn't match Google Cloud Console configuration.

**Solution:**
1. Check Google Cloud Console → Credentials → Your OAuth Client
2. Ensure **Authorized redirect URIs** includes:
   ```
   http://localhost:8081/login/oauth2/code/google
   ```
3. Save changes and wait 5 minutes for propagation

---

### Issue: Frontend Not Receiving Tokens

**Cause:** CORS or redirect URL mismatch.

**Solution:**
1. Verify `OAuth2LoginSuccessHandler` redirect URL:
   ```java
   String targetUrl = "http://localhost:3000/oauth2/callback?token=" + ...
   ```
2. Ensure frontend has route `/oauth2/callback`
3. Check browser console for errors

---

### Issue: "Invalid client" Error

**Cause:** Client ID or secret is incorrect or not set.

**Solution:**
1. Verify environment variables are set:
   ```powershell
   echo $env:GOOGLE_CLIENT_ID
   echo $env:GOOGLE_CLIENT_SECRET
   ```
2. Restart the Spring Boot application
3. Check `application.properties` for placeholder values

---

### Issue: User Not Created in Database

**Cause:** Database connection issue or unique constraint violation.

**Solution:**
1. Check database logs
2. Verify `users` table exists
3. Check if email already exists:
   ```sql
   SELECT * FROM users WHERE email = 'your-email@gmail.com';
   ```
4. Check Spring Boot logs for exceptions

---

### Issue: Tokens Not Working After Login

**Cause:** JWT secret mismatch or token expired.

**Solution:**
1. Verify JWT secret is set in `application.properties`
2. Check token expiry settings:
   ```properties
   jwt.expiration=3600000  # 1 hour
   jwt.refresh.expiration=604800000  # 7 days
   ```
3. Test token with `/api/auth/user-info` endpoint

---

## 📚 Related Documentation

- [ENHANCED_SECURITY_IMPLEMENTATION.md](./ENHANCED_SECURITY_IMPLEMENTATION.md) - JWT + Refresh Token system
- [ENHANCED_SECURITY_QUICKREF.md](./ENHANCED_SECURITY_QUICKREF.md) - Quick reference for security features
- [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) - All API endpoints
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)

---

## 🎯 Quick Start Checklist

- [ ] Created Google Cloud project
- [ ] Enabled Google+ API (optional)
- [ ] Created OAuth2 credentials
- [ ] Added redirect URI: `http://localhost:8081/login/oauth2/code/google`
- [ ] Copied client ID and secret
- [ ] Set environment variables (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`)
- [ ] Restarted Spring Boot application
- [ ] Created frontend `/oauth2/callback` page
- [ ] Added "Login with Google" button
- [ ] Tested full login flow
- [ ] Verified user created in database
- [ ] Tested JWT token works

---

## 🚀 Next Steps

1. **Production Deployment:**
   - Update redirect URI to production domain
   - Use secure environment variable management (AWS Secrets Manager, Azure Key Vault)
   - Enable HTTPS for all OAuth2 endpoints

2. **Enhanced Features:**
   - Add profile picture display in frontend
   - Support multiple OAuth2 providers (Facebook, GitHub)
   - Add OAuth2 provider badge in user profile
   - Implement account linking (merge OAuth2 and local accounts)

3. **Monitoring:**
   - Log OAuth2 login attempts
   - Track OAuth2 vs. regular login usage
   - Monitor token refresh patterns

---

**Last Updated:** 2024-11-03  
**Version:** 1.0  
**Author:** AutoNova Development Team

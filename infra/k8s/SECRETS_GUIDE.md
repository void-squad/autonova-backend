# Quick Reference: Required Secrets Configuration

This document lists all the secrets you need to configure before deploying the system.

## 🔐 Critical Secrets (Must Configure)

### 1. Database Passwords
These are the passwords for service-specific database users in your PostgreSQL (Neon) instance:

```bash
USER_MANAGEMENT_DB_PASSWORD=your-secure-password
CUSTOMER_SERVICE_DB_PASSWORD=your-secure-password
PROJECTS_DB_PASSWORD=your-secure-password
PROGRESS_MONITORING_DB_PASSWORD=your-secure-password
NOTIFICATION_DB_PASSWORD=your-secure-password
PBS_DB_PASSWORD=your-secure-password
TIME_LOGGING_DB_PASSWORD=your-secure-password
VECTOR_DB_PASSWORD=your-secure-password
```

**How to get:** These are set when you create the database users in PostgreSQL.

### 2. PostgreSQL Main Connection
```bash
POSTGRES_HOST=ep-mute-thunder-adhoybp1-pooler.c-2.us-east-1.aws.neon.tech
POSTGRES_PORT=5432
POSTGRES_USER=neondb_owner
POSTGRES_PASSWORD=your-main-postgres-password
PGDATABASE=autonova_db
```

**How to get:** From your Neon database dashboard (https://console.neon.tech/)

### 3. Email Configuration (Gmail)
```bash
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-gmail-app-password
```

**How to get Gmail App Password:**
1. Go to Google Account settings: https://myaccount.google.com/
2. Security → 2-Step Verification (must be enabled)
3. App passwords → Generate new app password
4. Select "Mail" and "Other (Custom name)"
5. Copy the 16-character password

### 4. Google OAuth2
```bash
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
```

**How to get:**
1. Go to Google Cloud Console: https://console.cloud.google.com/
2. APIs & Services → Credentials
3. Create OAuth 2.0 Client ID (Web application)
4. Add authorized redirect URIs:
   - `http://localhost:8081/login/oauth2/code/google`
   - Your production URLs
5. Copy Client ID and Client Secret

### 5. Stripe Payment (if using payments)
```bash
STRIPE_API_KEY=sk_test_... (or sk_live_...)
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PUBLISHABLE_KEY=pk_test_... (or pk_live_...)
STRIPE_WEBHOOK_ENDPOINT_SECRET=whsec_...
```

**How to get:**
1. Go to Stripe Dashboard: https://dashboard.stripe.com/
2. Developers → API keys
3. Copy Secret key (STRIPE_API_KEY)
4. Copy Publishable key (STRIPE_PUBLISHABLE_KEY)
5. Webhooks → Add endpoint → Copy signing secret

### 6. Google Gemini AI (for Chatbot)
```bash
GEMINI_API_KEY=your-gemini-api-key
SPRING_AI_VERTEXAI_GEMINI_PROJECT_ID=YOUR_PROJECT_ID
```

**How to get:**
1. Go to Google AI Studio: https://aistudio.google.com/
2. Get API key
3. For Vertex AI: Use your Google Cloud Project ID

## 🔧 Optional/Default Secrets (Can Keep As-Is)

### JWT Secret
```bash
JWT_SECRET=YXV0b25vdmEtc2VjcmV0LWtleS1mb3Itand0LWF1dGhlbnRpY2F0aW9uLXNlcnZpY2UtMjAyNQ==
```
**Note:** This is a base64-encoded string. You can keep the default or generate a new one:
```bash
echo -n "your-secret-text" | base64
```

### Frontend URL
```bash
FRONTEND_URL=http://localhost:5173
```
**Update to your actual frontend URL in production**

### Database SSL Mode
```bash
DB_SSLMODE=require
```
**Keep as `require` for Neon cloud database**

## 📋 Configuration Checklist

Before deployment, ensure you have:

- [ ] Created all databases in PostgreSQL:
  - [ ] user_management_db
  - [ ] customer_service
  - [ ] projects_db
  - [ ] progress (for progress monitoring)
  - [ ] notifications_db
  - [ ] payments_billing_db
  - [ ] time_logging_db
  - [ ] vector_db
  
- [ ] Created service users with appropriate permissions for each database

- [ ] Obtained Gmail App Password (not regular password)

- [ ] Created Google OAuth2 credentials with correct redirect URIs

- [ ] Set up Stripe account (if using payments)

- [ ] Obtained Gemini AI API key (if using chatbot)

- [ ] Updated all passwords in `.env` file

- [ ] Verified all service URLs are correct

## 🚨 Security Best Practices

1. **Never commit `.env` files** to version control
2. **Use strong passwords** for all database users (minimum 16 characters)
3. **Rotate secrets regularly** in production
4. **Use different secrets** for dev/staging/production
5. **Limit database user permissions** to only what's needed
6. **Enable 2FA** on all external services (Google, Stripe, etc.)
7. **Use secret management tools** in production (HashiCorp Vault, AWS Secrets Manager, etc.)

## 🔍 Testing Secrets

After configuring, test each connection:

### Test PostgreSQL Connection
```bash
psql "postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@${POSTGRES_HOST}:${POSTGRES_PORT}/${PGDATABASE}?sslmode=require"
```

### Test Gmail SMTP
```bash
curl --url 'smtps://smtp.gmail.com:465' --ssl-reqd \
  --mail-from 'your-email@gmail.com' \
  --mail-rcpt 'test@example.com' \
  --user 'your-email@gmail.com:your-app-password' \
  -T -
```

### Test Stripe API
```bash
curl https://api.stripe.com/v1/charges \
  -u ${STRIPE_API_KEY}: \
  -d amount=999 \
  -d currency=usd \
  -d source=tok_visa
```

## 📝 Environment File Location

**Kubernetes:**
```
infra/k8s/secrets/.env
```

**Docker Compose:**
```
infra/.env
```

## 🆘 Troubleshooting

### "Authentication failed" errors
- Verify passwords are correct (no extra spaces)
- Check user exists in database
- Verify SSL mode setting matches database requirements

### "Invalid API key" errors
- Ensure API keys are copied completely
- Check for expiration dates
- Verify correct API key type (test vs live)

### Email sending fails
- Must use Gmail App Password (not regular password)
- 2FA must be enabled on Google account
- Check for "Less secure app access" is NOT enabled (use App Passwords instead)

### OAuth redirect errors
- Verify redirect URI matches exactly in Google Console
- Check protocol (http vs https)
- Ensure port matches

## 📞 Support Resources

- **Neon PostgreSQL:** https://neon.tech/docs
- **Gmail SMTP:** https://support.google.com/accounts/answer/185833
- **Google OAuth2:** https://developers.google.com/identity/protocols/oauth2
- **Stripe API:** https://stripe.com/docs/api
- **Gemini AI:** https://ai.google.dev/docs

## ⚡ Quick Start

1. Copy the example file:
   ```bash
   cp infra/k8s/secrets/.env.example infra/k8s/secrets/.env
   ```

2. Open in editor:
   ```bash
   vim infra/k8s/secrets/.env
   ```

3. Fill in at minimum:
   - All `*_DB_PASSWORD` values
   - `POSTGRES_PASSWORD`
   - `EMAIL_USERNAME` and `EMAIL_PASSWORD`
   - `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`

4. Create Kubernetes secret:
   ```bash
   cd infra/k8s/secrets
   ./create-secrets-from-env.sh autonova-env autonova
   ```

5. Verify:
   ```bash
   kubectl get secret autonova-env -n autonova
   ```

You're now ready to deploy! 🚀

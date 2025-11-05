# Quick Reference: User Update Permissions

## ✅ What Changed

### 1. WHO Can Update User Details?
- ✅ **ADMIN** - Can update anyone
- ✅ **EMPLOYEE** - Can update only themselves
- ✅ **CUSTOMER** - Can update only themselves
- ❌ **USER** (guest) - CANNOT update anyone

### 2. Role Changes
- ❌ **Regular PUT** `/api/users/{id}` - Role field is **IGNORED**
- ✅ **New PATCH** `/api/users/{id}/role` - **ADMIN ONLY** can change roles

---

## 📋 API Endpoints

### Update User Profile (No Role Change)
```http
PUT /api/users/{id}
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "userName": "New Name",
  "email": "newemail@example.com",
  "contactOne": "+94123456789",
  "password": "newpassword",  // Optional
  "address": "New Address",   // Optional
  "contactTwo": "+94987654321" // Optional
}
```

**Access:**
- ADMIN → Can update any user
- EMPLOYEE → Can update only themselves (id must match their userId)
- CUSTOMER → Can update only themselves (id must match their userId)
- USER → ❌ FORBIDDEN

**Note:** If you include `"role": "ADMIN"` in the request body, **it will be ignored**.

---

### Change User Role (ADMIN Only)
```http
PATCH /api/users/{id}/role
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json

{
  "role": "EMPLOYEE"
}
```

**Access:** ADMIN ONLY

**Allowed Values:** `CUSTOMER`, `EMPLOYEE`, `ADMIN`

---

## 🧪 Test Commands

### Test 1: Customer Updates Own Profile ✅
```bash
curl -X PUT http://localhost:8081/api/users/4 \
  -H "Authorization: Bearer <CUSTOMER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Updated Name",
    "contactOne": "+94777777777"
  }'
```
**Expected:** 200 OK - Profile updated

---

### Test 2: Guest (USER) Tries to Update ❌
```bash
curl -X PUT http://localhost:8081/api/users/5 \
  -H "Authorization: Bearer <USER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Hacker"
  }'
```
**Expected:** 403 Forbidden

---

### Test 3: Customer Tries to Change Role via PUT ⚠️
```bash
curl -X PUT http://localhost:8081/api/users/4 \
  -H "Authorization: Bearer <CUSTOMER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Customer",
    "role": "ADMIN"
  }'
```
**Expected:** 200 OK - Name updated, but **role stays CUSTOMER** (role field ignored)

---

### Test 4: Admin Changes Role via PATCH ✅
```bash
curl -X PATCH http://localhost:8081/api/users/4/role \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "EMPLOYEE"
  }'
```
**Expected:** 200 OK - Role changed to EMPLOYEE

---

### Test 5: Employee Tries to Change Role ❌
```bash
curl -X PATCH http://localhost:8081/api/users/4/role \
  -H "Authorization: Bearer <EMPLOYEE_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "ADMIN"
  }'
```
**Expected:** 403 Forbidden - Only ADMIN can access this endpoint

---

## ⚠️ Before Testing

**Fix the database constraint first!**

Connect to your Neon PostgreSQL database and run:

```sql
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check 
    CHECK (role IN ('USER', 'CUSTOMER', 'EMPLOYEE', 'ADMIN'));
```

See `FIX_DATABASE_CONSTRAINT.md` for detailed instructions.

---

## 📝 Modified Files

1. `UserSecurityService.java` - Updated `canModifyUser()`
2. `UserService.java` - Removed role update, added `updateUserRole()`
3. `UserController.java` - Added `PATCH /{id}/role` endpoint

---

## 🎯 Summary

| Action | USER | CUSTOMER | EMPLOYEE | ADMIN |
|--------|------|----------|----------|-------|
| Update own profile | ❌ | ✅ | ✅ | ✅ |
| Update other's profile | ❌ | ❌ | ❌ | ✅ |
| Change own role | ❌ | ❌ | ❌ | ❌* |
| Change other's role | ❌ | ❌ | ❌ | ✅ |

*Even ADMIN must use the dedicated PATCH endpoint to change roles

---

**All changes compiled successfully!** ✅

Ready to test after fixing the database constraint.

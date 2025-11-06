# Frontend Integration Guide

This guide describes how the React frontend should communicate with the Autonova backend services through the API Gateway.

## 1. Gateway Overview

- **Gateway URL:** `http://localhost:8080`
- **Available routes:**
  - `POST /api/users` → Auth Service (user signup)
  - `POST /api/auth/login` → Auth Service (issue JWT)
  - `GET /api/customers/me` → Customer Service (fetch profile)
  - `PUT /api/customers/me` → Update profile
  - `DELETE /api/customers/me` → Delete profile
  - `POST /api/customers/me/vehicles` → Create vehicle
  - `GET /api/customers/me/vehicles` → List vehicles
  - `GET /api/customers/me/vehicles/{vehicleId}` → Vehicle details
  - `PUT /api/customers/me/vehicles/{vehicleId}` → Update vehicle
  - `DELETE /api/customers/me/vehicles/{vehicleId}` → Delete vehicle

> **CORS** is enabled for `http://localhost:5173`. Adjust `gateway-service/src/main/resources/application.yml` when deploying elsewhere.

## 2. React Environment Setup

Create `.env.local` in the React project:

```ini
REACT_APP_API_BASE_URL=http://localhost:8080
```

Then create a thin API client (using `fetch` or `axios`). Example using `fetch`:

```ts
// src/api/client.ts
const API_BASE_URL =
  process.env.REACT_APP_API_BASE_URL ?? 'http://localhost:8080';

export async function api<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('authToken');
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    throw new Error(errorBody.error ?? response.statusText);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
```

## 3. Auth Flows

```ts
// login
const login = (email: string, password: string) =>
  api<{
    token: string;
    type: string;
    user: { id: number; userName: string; email: string; role: string };
  }>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }).then((res) => {
    localStorage.setItem('authToken', res.token);
    return res.user;
  });

// logout
const logout = () => {
  localStorage.removeItem('authToken');
};
```

## 4. Customer Profile

```ts
const getProfile = () => api<Customer>('/api/customers/me');

const updateProfile = (payload: CustomerUpdate) =>
  api<Customer>('/api/customers/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

const deleteProfile = () =>
  api<void>('/api/customers/me', {
    method: 'DELETE',
  });
```

## 5. Vehicles

```ts
const createVehicle = (payload: VehicleInput) =>
  api<Vehicle>('/api/customers/me/vehicles', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

const listVehicles = () => api<Vehicle[]>('/api/customers/me/vehicles');

const updateVehicle = (vehicleId: number, payload: VehicleInput) =>
  api<Vehicle>(`/api/customers/me/vehicles/${vehicleId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

const deleteVehicle = (vehicleId: number) =>
  api<void>(`/api/customers/me/vehicles/${vehicleId}`, {
    method: 'DELETE',
  });
```

Define suitable TypeScript interfaces:

```ts
interface Customer {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  vehicles: Vehicle[];
}

interface CustomerUpdate {
  firstName: string;
  lastName: string;
  phoneNumber: string;
}

interface Vehicle {
  id: number;
  make: string;
  model: string;
  year: number;
  vin: string;
  licensePlate: string;
}

interface VehicleInput extends Omit<Vehicle, 'id'> {}
```

## 6. Error Handling & Refresh

- If any request returns `401` or `403`, redirect users to the login page.
- Token refresh is not yet implemented; store issue time and prompt for re-login when expired.
- Use `try/catch` around API calls and map backend error messages to UI toasts.

## 7. Additional Notes

- Keep the JWT secret in backend configuration only; the frontend stores only the issued token.
- When deploying to production, place the gateway behind HTTPS and update `allowedOrigins`.
- For SSR/Next.js, replace `localStorage` with server-side token handling.

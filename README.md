# Android Networking & Authentication Showcase

This repository is a focused technical showcase demonstrating a clean, modern approach to networking and authentication in a real Android project.

## Purpose

The goal of this showcase is to provide a clear reference implementation for handling API interactions, token management, and authentication flows.

This showcase intentionally limits its scope to Networking and Authentication to make the core logic easy to inspect and understand. It builds upon a minimal Jetpack Compose UI to demonstrate the data flow.

## What it Demonstrates

- **Modern Android Networking**: Kotlin Coroutines, Retrofit, and OkHttp.
- **Authentication Flow**: Managing access and refresh tokens.
- **Automatic Token Refresh**: Transparently handling `HTTP 401 Unauthorized` errors, refreshing the token, and automatically retrying the original request.
- **Error Handling**: Meaningful normalization of network, HTTP, and timeout errors into sealed classes for the UI layer.
- **Testable Architecture**: A clean separation of responsibilities ensuring that complex behaviors like 401 retries are easily unit-testable.

## Architecture & Data Flow

The networking layer is structured to keep UI and ViewModels agnostic of underlying HTTP mechanisms.

`Application` → `ViewModel` → `Repository` → `ApiClient` → `ApiService (Retrofit)` → `OkHttpClient`

Key components:
- `TokenManager`: Responsible for storing and providing access/refresh tokens.
- `AuthInterceptor`: Automatically attaches the access token (`Bearer <token>`) to outgoing API requests.
- `TokenAuthenticator`: An OkHttp `Authenticator` that safely intercepts 401 errors, synchronizes refresh requests (to prevent concurrent refreshes), and retries the original request upon success.

## Mock Backend

To keep this showcase safe and public, an in-process OkHttp Interceptor (`MockBackendInterceptor`) simulates network latency, responses, and 401 scenarios. This allows the demonstration to exercise the entire Retrofit/OkHttp stack.

## Token Storage

This showcase uses `SharedPreferences` for token persistence to keep the implementation focused on networking and authentication. Secure token storage is demonstrated separately in the data and security showcase.

## Dependency Injection

The project uses a small `AppContainer` for manual dependency injection. This keeps the networking example explicit and avoids introducing a DI framework that is not necessary for this focused showcase.

## Testing

The project includes unit tests emphasizing the actual behavior of the networking and authentication layer:
- Successful API requests and token attachment.
- `401 Unauthorized` → Refresh → Retry flow.
- Token refresh failures.
- Safeguards against infinite retry loops.
- Error normalization (e.g., Timeout, NoInternet, ServerError).

Tests utilize `MockWebServer` to validate the full OkHttp stack.

## How to Run

1. Open the project in Android Studio.
2. Build the project.
3. Run on an emulator or physical device.
4. Run unit tests via `./gradlew test` or inside Android Studio.

## Code Quality and Security

- **No Secrets**: Contains absolutely no real credentials, API keys, or private endpoints.
- **Focused**: The implementation remains focused on the primary networking goals.
- **Maintainable**: Uses clear naming conventions and logical structure over extensive comments.

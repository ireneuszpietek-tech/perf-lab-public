## Authentication (Current State)

The current authentication setup is intentionally limited to a minimal development-only configuration.

Under the `dev` profile:
- HS256 is used with a static symmetric key
- Local `JwtEncoder` and `JwtDecoder` beans are provided
- The setup is intended solely to unblock application-level work and request flow testing

This configuration is **not considered production-grade** and does not represent a finalized security model.

Authentication, token format, verification strategy, and lifecycle policies are explicitly 
deferred and treated as future research variables.

## Deferred Research Topics

- Stateless token-based authentication model
- Symmetric vs asymmetric token signing cost under load
- Local JWT verification vs remote introspection
- Token lifetime strategies for long-running and asynchronous processes
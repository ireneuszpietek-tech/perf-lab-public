package ip.logistics.platform.http

class Errors {
    // app exceptions — used in domain / service / ctrl
    class NotFound(msg: String) : RuntimeException(msg)
    class Conflict(msg: String) : RuntimeException(msg)

    // optional exceptions
    class BadRequest(msg: String) : RuntimeException(msg)
    class Unauthorized(msg: String) : RuntimeException(msg)
}
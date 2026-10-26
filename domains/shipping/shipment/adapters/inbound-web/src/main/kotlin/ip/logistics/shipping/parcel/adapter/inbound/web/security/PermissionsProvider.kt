package ip.logistics.shipping.parcel.adapter.inbound.web.security

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

@Component
class PermissionsProvider {

    //here we can get data from cache/DB/FF in runtime
    //
    //return required scope
    //
    //GET: /api/parcels/** required [read:parcel]
    //POST /api/parcels required [create:parcel]
    fun requiredScope(method: HttpMethod, path: String): String? {
        return when (method) {
            HttpMethod.GET if path.startsWith("/api/parcels/") -> "read:parcel"
            HttpMethod.POST if path == "/api/parcels" -> "create:parcel"
            else -> null
        }
    }
}
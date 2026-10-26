package ip.logistics.shipping.parcel.adapter.inbound.web.controller

import ip.logistics.parcel.domain.model.TrackingCode
import ip.logistics.shipping.parcel.adapter.inbound.web.dto.CreateShipmentRequestDto
import ip.logistics.shipping.parcel.adapter.inbound.web.dto.CreateShipmentResponseDto
import ip.logistics.shipping.parcel.application.command.CreateShipmentCommand
import ip.logistics.shipping.parcel.application.command.CreateShipmentResult
import ip.logistics.shipping.parcel.application.port.inbound.CreateShipment
import ip.logistics.shipping.parcel.application.port.inbound.GetShipmentView
import ip.logistics.shipping.parcel.application.query.ShipmentView
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("api/shipping/v1/parcels")
class ParcelController(
    private val createShipment: CreateShipment,
    private val getShipmentView: GetShipmentView
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun create(@Valid @RequestBody req: CreateShipmentRequestDto): ResponseEntity<CreateShipmentResponseDto> {
        val res: CreateShipmentResult = createShipment.handle(
            CreateShipmentCommand(
                req.fromLocker,
                req.toLocker,
                req.weightGrams,
                req.size,
                req.clientOperationId)
        )
        val body = CreateShipmentResponseDto(res.trackingCode.value, res.status.name)
        val uri = URI.create("/api/shipping/v1/parcels/${res.trackingCode.value}")
        return ResponseEntity.created(uri).body(body)
    }

    @GetMapping("/{trackingCode}")
    fun get(@PathVariable trackingCode: String): ResponseEntity<ShipmentView> {
        log.info("Get parcel by public id: [{}]", trackingCode)
        val shipmentView = getShipmentView.handle(TrackingCode(trackingCode));

        return if (shipmentView == null) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(shipmentView)
        }
    }

}
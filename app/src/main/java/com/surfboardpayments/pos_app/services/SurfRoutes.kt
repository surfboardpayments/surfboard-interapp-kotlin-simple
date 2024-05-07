package com.surfboardpayments.pos_app.services

import com.surfboardpayments.pos_app.models.CodeGenerated
import com.surfboardpayments.pos_app.models.codeGenerated
import com.surfboardpayments.pos_app.models.noDataResponse
import com.surfboardpayments.pos_app.models.orderCreated
import com.surfboardpayments.pos_app.models.paymentInitiated

class SurfRouteValue(
    val requestType: RequestType,
    val route: String,
    val response: (source: String) -> Any
)

enum class SurfRoute {
    CreateOrder,
 InitiatePayment, GenerateRegistrationCode, CancelOrder
}

class RouteMapClass {
    val routeMap: Map<SurfRoute, SurfRouteValue> = mapOf(
        SurfRoute.CreateOrder to SurfRouteValue(RequestType.POST, "orders", ::orderCreated),

        SurfRoute.InitiatePayment to SurfRouteValue(
            RequestType.POST,
            "payments",
            ::paymentInitiated
        ),
        SurfRoute.GenerateRegistrationCode to SurfRouteValue(
            RequestType.GET,
            "merchants/${Constants.merchantId}/stores/${Constants.storeId}/terminals/interapp",
            ::codeGenerated
        ),
        SurfRoute.CancelOrder to SurfRouteValue(
            RequestType.DELETE,
            "orders/${Constants.orderId}",
            ::noDataResponse
        )
    )

}
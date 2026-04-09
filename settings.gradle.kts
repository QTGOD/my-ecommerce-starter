rootProject.name = "my-ecommerce-starter"

include(
    "libs:common",
    "services:gateway-service",
    "services:auth-service",
    "services:product-service",
    "services:cart-service",
    "services:order-service",
    "services:inventory-listener",
    "services:payment-service"
)

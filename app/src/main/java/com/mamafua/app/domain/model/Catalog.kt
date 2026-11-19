@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class CatalogData(
    val categories: List<String> = emptyList(),
    val services: List<CatalogServiceItem> = emptyList(),
    val addons: List<CatalogAddonItem> = emptyList(),
    @JsonNames("detergent_toggle", "detergentToggle")
    val detergentToggle: DetergentToggleConfig = DetergentToggleConfig(),
    val estates: List<EldoretEstate> = emptyList()
)

@Serializable
data class CatalogServiceItem(
    val code: String,
    @JsonNames("name", "title", "label")
    val name: String = code,
    val category: String = "Standard Laundry (Fua & Pasi)",
    @JsonNames("base_price", "basePrice", "price")
    val basePrice: Double = 0.0,
    @JsonNames("unit_label", "unitLabel", "unit")
    val unitLabel: String = "per item",
    val description: String? = null,
    val checklist: List<String> = emptyList()
)

@Serializable
data class CatalogAddonItem(
    val code: String,
    @JsonNames("name", "title", "label")
    val name: String = code,
    @JsonNames("price", "base_price", "basePrice", "amount")
    val price: Double = 0.0,
    @JsonNames("unit_label", "unitLabel", "unit")
    val unitLabel: String = "add-on"
)

@Serializable
data class DetergentToggleConfig(
    @JsonNames("client_provides_label", "clientProvidesLabel")
    val clientProvidesLabel: String = "I will provide detergents & soap",
    @JsonNames("worker_brings_label", "workerBringsLabel")
    val workerBringsLabel: String = "Mama Fua brings detergents (+KSh 250)",
    @JsonNames("worker_brings_fee", "workerBringsFee", "fee")
    val workerBringsFee: Double = 250.0
)

@Serializable
data class EldoretEstate(
    val name: String,
    @JsonNames("latitude", "lat")
    val latitude: Double = 0.5143,
    @JsonNames("longitude", "lng", "lon")
    val longitude: Double = 35.2698
)

@Serializable
data class SelectedQuantityItem(
    val code: String,
    val quantity: Int
)

@Serializable
data class EstimateLineItem(
    val code: String = "",
    val name: String = "",
    val quantity: Int = 1,
    @JsonNames("unit_price", "unitPrice", "price")
    val unitPrice: Double = 0.0,
    @JsonNames("line_total", "lineTotal", "total", "subtotal")
    val lineTotal: Double = 0.0
)

@Serializable
data class PriceEstimate(
    val currency: String = "KES",
    @JsonNames("line_items", "lineItems")
    val lineItems: List<EstimateLineItem> = emptyList(),
    @JsonNames("addon_lines", "addonLines")
    val addonLines: List<EstimateLineItem> = emptyList(),
    @JsonNames("worker_brings_detergents", "workerBringsDetergents")
    val workerBringsDetergents: Boolean = false,
    @JsonNames("detergent_fee", "detergentFee")
    val detergentFee: Double = 0.0,
    @JsonNames("services_subtotal", "servicesSubtotal")
    val servicesSubtotal: Double = 0.0,
    @JsonNames("addons_subtotal", "addonsSubtotal")
    val addonsSubtotal: Double = 0.0,
    @JsonNames("total_estimated_price", "totalEstimatedPrice", "total")
    val totalEstimatedPrice: Double = 0.0,
    @JsonNames("commitment_deposit_50_percent", "commitmentDeposit50Percent", "deposit")
    val commitmentDeposit50Percent: Double = 0.0
)

object EldoretCatalogDefaults {
    val defaultEstates = listOf(
        EldoretEstate("Kapsoya", 0.5220, 35.2920),
        EldoretEstate("Stage/Kimumu", 0.5472, 35.2861),
        EldoretEstate("Pioneer", 0.4932, 35.2654),
        EldoretEstate("Elgon View", 0.4988, 35.2842),
        EldoretEstate("Annex", 0.4795, 35.2950),
        EldoretEstate("Langas", 0.4830, 35.2520),
        EldoretEstate("Town Center", 0.5143, 35.2698),
        EldoretEstate("West Indies", 0.5190, 35.2580),
        EldoretEstate("Action", 0.5315, 35.2760),
        EldoretEstate("Maili Nne", 0.5560, 35.2420)
    )

    val defaultCategories = listOf(
        "Standard Laundry (Fua & Pasi)",
        "General House Cleaning",
        "Deep & Specialized Cleaning",
        "Move-In / Move-Out Cleaning"
    )

    fun getCategoryImage(category: String?): String {
        val cat = category.orEmpty().lowercase()
        return when {
            cat.contains("laundry") || cat.contains("fua") || cat.contains("pasi") ->
                "https://images.unsplash.com/photo-1582735689369-4fe89db7114c?auto=format&fit=crop&w=800&q=80"
            cat.contains("deep") || cat.contains("specialized") ->
                "https://images.unsplash.com/photo-1528740561666-dc2479dc08ab?auto=format&fit=crop&w=800&q=80"
            cat.contains("move") || cat.contains("vacant") ->
                "https://images.unsplash.com/photo-1588854337221-4cf9fa96059c?auto=format&fit=crop&w=800&q=80"
            else ->
                "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=800&q=80"
        }
    }

    val defaultServices = listOf(
        CatalogServiceItem(
            code = "laundry_basket_small",
            name = "Small Laundry Basket",
            category = "Standard Laundry (Fua & Pasi)",
            basePrice = 500.0,
            unitLabel = "per basket",
            checklist = listOf("Sort colors & whites", "Wash & rinse", "Hang to dry", "Fold neatly")
        ),
        CatalogServiceItem(
            code = "laundry_basket_medium",
            name = "Medium Laundry Basket",
            category = "Standard Laundry (Fua & Pasi)",
            basePrice = 800.0,
            unitLabel = "per basket",
            checklist = listOf("Sort fabrics", "Hand/machine wash", "Hang & fold", "Light ironing")
        ),
        CatalogServiceItem(
            code = "laundry_basket_large",
            name = "Large Family Basket + Ironing",
            category = "Standard Laundry (Fua & Pasi)",
            basePrice = 1200.0,
            unitLabel = "per basket",
            checklist = listOf("Full family load wash", "Stain treatment", "Full ironing (Pasi) & folding")
        ),
        CatalogServiceItem(
            code = "laundry_duvet_heavy",
            name = "Heavy Duvet / Blanket",
            category = "Standard Laundry (Fua & Pasi)",
            basePrice = 450.0,
            unitLabel = "per duvet",
            checklist = listOf("Deep soak", "Gentle scrub & rinse", "Sun dry thoroughly")
        ),
        CatalogServiceItem(
            code = "house_clean_studio",
            name = "Studio / Bedsitter Cleaning",
            category = "General House Cleaning",
            basePrice = 600.0,
            unitLabel = "per unit",
            checklist = listOf("Sweep & mop floor", "Wash dishes & sink", "Bathroom & toilet scrub", "Dust surfaces")
        ),
        CatalogServiceItem(
            code = "house_clean_1br",
            name = "1 Bedroom Apartment Cleaning",
            category = "General House Cleaning",
            basePrice = 900.0,
            unitLabel = "per apartment",
            checklist = listOf("Living room & bedroom mop", "Kitchen counters & dishes", "Bathroom sanitize", "Make bed & organize")
        ),
        CatalogServiceItem(
            code = "house_clean_2br",
            name = "2 Bedroom Apartment Cleaning",
            category = "General House Cleaning",
            basePrice = 1300.0,
            unitLabel = "per apartment",
            checklist = listOf("All rooms swept & mopped", "Kitchen deep wipe", "Bathrooms scrubbed", "Trash removal")
        ),
        CatalogServiceItem(
            code = "house_clean_3br",
            name = "3+ Bedroom House Cleaning",
            category = "General House Cleaning",
            basePrice = 1800.0,
            unitLabel = "per house",
            checklist = listOf("All bedrooms & living spaces", "Kitchen & dining", "All bathrooms", "Veranda/balcony")
        ),
        CatalogServiceItem(
            code = "deep_sofa_seat",
            name = "Sofa Set Deep Scrub",
            category = "Deep & Specialized Cleaning",
            basePrice = 350.0,
            unitLabel = "per seat",
            checklist = listOf("Vacuum dust", "Foam shampoo scrub", "Stain extraction", "Deodorize")
        ),
        CatalogServiceItem(
            code = "deep_mattress",
            name = "Mattress Deep Sanitization",
            category = "Deep & Specialized Cleaning",
            basePrice = 900.0,
            unitLabel = "per mattress",
            checklist = listOf("Stain removal", "Anti-bacterial scrub", "Odor neutralization")
        ),
        CatalogServiceItem(
            code = "move_vacant_per_room",
            name = "Move-In / Move-Out Full Scrub",
            category = "Move-In / Move-Out Cleaning",
            basePrice = 650.0,
            unitLabel = "per room",
            checklist = listOf("Cabinets & wardrobes inside/out", "Walls & skirting wipe", "Tiles & grout scrub", "Windows & frames")
        )
    )

    val defaultAddons = listOf(
        CatalogAddonItem("addon_fridge", "Inside Fridge Cleaning", 300.0, "per fridge"),
        CatalogAddonItem("addon_oven", "Oven / Cooker Degreasing", 300.0, "per cooker"),
        CatalogAddonItem("addon_windows", "Interior Window Washing", 250.0, "per set"),
        CatalogAddonItem("addon_sneakers", "Sneakers / Shoes Scrub", 100.0, "per pair"),
        CatalogAddonItem("addon_compound", "Veranda / Compound Sweep", 250.0, "per area")
    )

    val defaultWorkerBadges = listOf(
        "Fast Ironer",
        "Sofa Specialist",
        "Deep Clean Pro",
        "Delicate Fabrics",
        "Move-Out Expert",
        "Spotless Kitchen"
    )

    fun defaultCatalog() = CatalogData(
        categories = defaultCategories,
        services = defaultServices,
        addons = defaultAddons,
        detergentToggle = DetergentToggleConfig(),
        estates = defaultEstates
    )

    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    fun findNearestEstate(
        lat: Double,
        lng: Double,
        estates: List<EldoretEstate> = defaultEstates
    ): EldoretEstate {
        val pool = estates.ifEmpty { defaultEstates }
        return pool.minByOrNull { est ->
            haversineDistanceKm(lat, lng, est.latitude, est.longitude)
        } ?: defaultEstates.first()
    }

    fun resolveCoordinates(
        latitude: Double?,
        longitude: Double?,
        estateName: String?,
        locationText: String? = null
    ): Pair<Double, Double> {
        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            return latitude to longitude
        }
        val byEstate = defaultEstates.find {
            !estateName.isNullOrBlank() && it.name.equals(estateName.trim(), ignoreCase = true)
        } ?: defaultEstates.find { est ->
            !locationText.isNullOrBlank() && locationText.contains(est.name, ignoreCase = true)
        }
        return if (byEstate != null) {
            byEstate.latitude to byEstate.longitude
        } else {
            0.5143 to 35.2698 // Eldoret Town Center
        }
    }

    fun computeLocalEstimate(
        catalog: CatalogData,
        items: List<SelectedQuantityItem>,
        addons: List<SelectedQuantityItem>,
        workerBringsDetergents: Boolean
    ): PriceEstimate {
        val lineItems = items.filter { it.quantity > 0 }.mapNotNull { sel ->
            val svc = catalog.services.find { it.code == sel.code } ?: return@mapNotNull null
            EstimateLineItem(
                code = svc.code,
                name = svc.name,
                quantity = sel.quantity,
                unitPrice = svc.basePrice,
                lineTotal = svc.basePrice * sel.quantity
            )
        }
        val addonLines = addons.filter { it.quantity > 0 }.mapNotNull { sel ->
            val addon = catalog.addons.find { it.code == sel.code } ?: return@mapNotNull null
            EstimateLineItem(
                code = addon.code,
                name = addon.name,
                quantity = sel.quantity,
                unitPrice = addon.price,
                lineTotal = addon.price * sel.quantity
            )
        }
        val servicesSub = lineItems.sumOf { it.lineTotal }
        val addonsSub = addonLines.sumOf { it.lineTotal }
        val detergentFee = if (workerBringsDetergents && (servicesSub > 0 || addonsSub > 0)) {
            catalog.detergentToggle.workerBringsFee
        } else 0.0
        val total = servicesSub + addonsSub + detergentFee
        return PriceEstimate(
            currency = "KES",
            lineItems = lineItems,
            addonLines = addonLines,
            workerBringsDetergents = workerBringsDetergents,
            detergentFee = detergentFee,
            servicesSubtotal = servicesSub,
            addonsSubtotal = addonsSub,
            totalEstimatedPrice = total,
            commitmentDeposit50Percent = kotlin.math.ceil(total * 0.5)
        )
    }
}

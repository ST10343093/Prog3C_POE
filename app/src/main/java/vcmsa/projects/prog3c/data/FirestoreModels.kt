package vcmsa.projects.prog3c.data

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Firestore data models for cloud storage
 * These replace the Room entities for online database storage
 */

/**
 * Category model for Firestore
 */
data class FirestoreCategory(
    val id: String = "",           // Firestore document ID
    val name: String = "",         // Category name (e.g., "Groceries", "Rent")
    val color: Int = 0,           // Color as integer
    val userId: String = ""       // User ID to separate data per user
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", 0, "")

    // Convert to map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "color" to color,
            "userId" to userId
        )
    }
}

/**
 * Expense model for Firestore
 */
data class FirestoreExpense(
    val id: String = "",               // Firestore document ID
    val amount: Double = 0.0,          // Expense amount
    val description: String = "",       // Expense description
    val date: Timestamp = Timestamp.now(), // Date as Firestore Timestamp
    val categoryId: String = "",       // Reference to category
    val photoPath: String? = null,     // Path to receipt photo
    val userId: String = ""            // User ID to separate data per user
) {
    // No-argument constructor required by Firestore
    constructor() : this("", 0.0, "", Timestamp.now(), "", null, "")

    // Convert to map for Firestore
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "amount" to amount,
            "description" to description,
            "date" to date,
            "categoryId" to categoryId,
            "userId" to userId
        )
        photoPath?.let { map["photoPath"] = it }
        return map
    }

    // Convert Date to Firestore format
    constructor(
        id: String,
        amount: Double,
        description: String,
        date: Date,
        categoryId: String,
        photoPath: String?,
        userId: String
    ) : this(id, amount, description, Timestamp(date), categoryId, photoPath, userId)

    // Get Date from Timestamp
    fun getDate(): Date {
        return date.toDate()
    }
}

/**
 * Budget model for Firestore
 */
data class FirestoreBudget(
    val id: String = "",                    // Firestore document ID
    val minimumAmount: Double = 0.0,        // Minimum budget target
    val maximumAmount: Double = 0.0,        // Maximum budget limit
    val categoryId: String = "",            // Reference to category
    val startDate: Timestamp = Timestamp.now(), // Budget start date
    val endDate: Timestamp = Timestamp.now(),   // Budget end date
    val userId: String = ""                 // User ID to separate data per user
) {
    // No-argument constructor required by Firestore
    constructor() : this("", 0.0, 0.0, "", Timestamp.now(), Timestamp.now(), "")

    // Convert to map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "minimumAmount" to minimumAmount,
            "maximumAmount" to maximumAmount,
            "categoryId" to categoryId,
            "startDate" to startDate,
            "endDate" to endDate,
            "userId" to userId
        )
    }

    // Convert Date to Firestore format
    constructor(
        id: String,
        minimumAmount: Double,
        maximumAmount: Double,
        categoryId: String,
        startDate: Date,
        endDate: Date,
        userId: String
    ) : this(id, minimumAmount, maximumAmount, categoryId, Timestamp(startDate), Timestamp(endDate), userId)

    // Get Dates from Timestamps
    fun getStartDate(): Date = startDate.toDate()
    fun getEndDate(): Date = endDate.toDate()
}

/**
 * Extension functions for converting between Room and Firestore models
 * These help maintain compatibility with existing code
 */

// Convert Room Category to Firestore Category
fun Category.toFirestore(userId: String = ""): FirestoreCategory {
    return FirestoreCategory(
        id = this.id.toString(), // Convert Long to String
        name = this.name,
        color = this.color,
        userId = userId
    )
}

// Convert Firestore Category to Room Category (for backwards compatibility)
fun FirestoreCategory.toRoom(): Category {
    return Category(
        id = this.id.toLongOrNull() ?: 0L, // Convert String to Long
        name = this.name,
        color = this.color
    )
}

// Convert Room Expense to Firestore Expense
fun Expense.toFirestore(userId: String = ""): FirestoreExpense {
    return FirestoreExpense(
        id = this.id.toString(), // Convert Long to String
        amount = this.amount,
        description = this.description,
        date = this.date,
        categoryId = this.categoryId.toString(), // Convert Long to String
        photoPath = this.photoPath,
        userId = userId
    )
}

// Convert Firestore Expense to Room Expense (for backwards compatibility)
fun FirestoreExpense.toRoom(): Expense {
    return Expense(
        id = this.id.toLongOrNull() ?: 0L, // Convert String to Long
        amount = this.amount,
        description = this.description,
        date = this.getDate(),
        categoryId = this.categoryId.toLongOrNull() ?: 0L, // Convert String to Long
        photoPath = this.photoPath
    )
}

// Convert Room Budget to Firestore Budget
fun Budget.toFirestore(userId: String = ""): FirestoreBudget {
    return FirestoreBudget(
        id = this.id.toString(), // Convert Long to String
        minimumAmount = this.minimumAmount,
        maximumAmount = this.maximumAmount,
        categoryId = this.categoryId.toString(), // Convert Long to String
        startDate = this.startDate,
        endDate = this.endDate,
        userId = userId
    )
}

// Convert Firestore Budget to Room Budget (for backwards compatibility)
fun FirestoreBudget.toRoom(): Budget {
    return Budget(
        id = this.id.toLongOrNull() ?: 0L, // Convert String to Long
        minimumAmount = this.minimumAmount,
        maximumAmount = this.maximumAmount,
        categoryId = this.categoryId.toLongOrNull() ?: 0L, // Convert String to Long
        startDate = this.getStartDate(),
        endDate = this.getEndDate()
    )
}
package vcmsa.projects.prog3c.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import vcmsa.projects.prog3c.*

/**
 * Smart Navigation Helper for the SmartSaver App
 * Provides centralized navigation logic with proper transitions and state management
 * Optimized for Activity-based navigation
 */
object NavigationHelper {

    /**
     * Navigate from current activity to target activity with smart transitions
     * @param currentActivity The current activity
     * @param targetActivityClass The target activity class
     * @param extras Optional bundle for passing data
     * @param clearStack Whether to clear the activity stack
     */
    fun navigateToActivity(
        currentActivity: Activity,
        targetActivityClass: Class<*>,
        extras: Bundle? = null,
        clearStack: Boolean = false
    ) {
        val intent = Intent(currentActivity, targetActivityClass).apply {
            if (clearStack) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            extras?.let { putExtras(it) }
        }
        currentActivity.startActivity(intent)

        // Apply smart transitions based on navigation type
        when {
            clearStack -> currentActivity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            isForwardNavigation(currentActivity, targetActivityClass) ->
                currentActivity.overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
            else -> currentActivity.overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        }

        if (clearStack) {
            currentActivity.finish()
        }
    }

    /**
     * Navigate to Add/Edit Expense with proper arguments
     */
    fun navigateToAddExpense(
        currentActivity: Activity,
        expenseId: String? = null,
        isEdit: Boolean = false
    ) {
        val intent = Intent(currentActivity, AddExpenseActivity::class.java).apply {
            expenseId?.let { putExtra("EXPENSE_ID", it) }
            putExtra("IS_EDIT", isEdit)
        }
        currentActivity.startActivity(intent)
        currentActivity.overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )
    }

    /**
     * Navigate to Expense Detail with expense ID
     */
    fun navigateToExpenseDetail(currentActivity: Activity, expenseId: String) {
        val intent = Intent(currentActivity, ExpenseDetailActivity::class.java).apply {
            putExtra("EXPENSE_ID", expenseId)
        }
        currentActivity.startActivity(intent)
        currentActivity.overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )
    }

    /**
     * Handle authentication flow navigation
     */
    fun navigateToAuthenticatedFlow(currentActivity: Activity) {
        navigateToActivity(
            currentActivity,
            MainActivity::class.java,
            clearStack = true
        )
    }

    /**
     * Handle logout navigation
     */
    fun navigateToWelcome(currentActivity: Activity) {
        navigateToActivity(
            currentActivity,
            WelcomeActivity::class.java,
            clearStack = true
        )
    }

    /**
     * Smart back navigation with proper transitions
     */
    fun navigateBack(currentActivity: Activity) {
        currentActivity.finish()
        currentActivity.overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )
    }

    /**
     * Navigate to categories with smart context awareness
     */
    fun navigateToCategories(currentActivity: Activity, fromExpenseFlow: Boolean = false) {
        val intent = Intent(currentActivity, CategoriesActivity::class.java).apply {
            putExtra("FROM_EXPENSE_FLOW", fromExpenseFlow)
        }
        currentActivity.startActivity(intent)
        currentActivity.overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )
    }

    /**
     * Navigate to budgets with context
     */
    fun navigateToBudgets(currentActivity: Activity, action: BudgetAction = BudgetAction.VIEW) {
        val targetActivity = when (action) {
            BudgetAction.CREATE -> BudgetActivity::class.java
            BudgetAction.VIEW -> BudgetListActivity::class.java
        }
        navigateToActivity(currentActivity, targetActivity)
    }

    /**
     * Navigate to analytics with smart context
     */
    fun navigateToAnalytics(currentActivity: Activity, analyticsType: AnalyticsType = AnalyticsType.MONTHLY) {
        val targetActivity = when (analyticsType) {
            AnalyticsType.MONTHLY -> CategoriesMonthlySpendingActivity::class.java
            AnalyticsType.BREAKDOWN -> SpendingBreakdownActivity::class.java
        }
        navigateToActivity(currentActivity, targetActivity)
    }

    /**
     * Navigate to smart features
     */
    fun navigateToSmartFeature(currentActivity: Activity, feature: SmartFeature) {
        val targetActivity = when (feature) {
            SmartFeature.IN_MY_POCKET -> InMyPocketActivity::class.java
        }
        navigateToActivity(currentActivity, targetActivity)
    }

    /**
     * Determine if navigation is forward or backward based on app hierarchy
     */
    private fun isForwardNavigation(currentActivity: Activity, targetClass: Class<*>): Boolean {
        val hierarchy = listOf(
            WelcomeActivity::class.java,
            LoginActivity::class.java,
            RegisterActivity::class.java,
            MainActivity::class.java,
            AddExpenseActivity::class.java,
            ExpensesActivity::class.java,
            ExpenseDetailActivity::class.java,
            CategoriesActivity::class.java,
            BudgetActivity::class.java,
            BudgetListActivity::class.java,
            CategoriesMonthlySpendingActivity::class.java,
            SpendingBreakdownActivity::class.java,
            InMyPocketActivity::class.java
        )

        val currentIndex = hierarchy.indexOf(currentActivity::class.java)
        val targetIndex = hierarchy.indexOf(targetClass)

        return currentIndex < targetIndex
    }

    /**
     * Get smart title for activities based on context
     */
    fun getSmartTitle(activity: Activity, context: Bundle? = null): String {
        return when (activity) {
            is AddExpenseActivity -> {
                val isEdit = context?.getBoolean("IS_EDIT", false) ?: false
                if (isEdit) "Edit Expense" else "Add Expense"
            }
            is ExpenseDetailActivity -> "Expense Details"
            is ExpensesActivity -> "My Expenses"
            is CategoriesActivity -> {
                val fromExpenseFlow = context?.getBoolean("FROM_EXPENSE_FLOW", false) ?: false
                if (fromExpenseFlow) "Select Category" else "Manage Categories"
            }
            is BudgetActivity -> "Create Budget"
            is BudgetListActivity -> "Budget Tracking"
            is CategoriesMonthlySpendingActivity -> "Spending Analytics"
            is SpendingBreakdownActivity -> "Spending Breakdown"
            is InMyPocketActivity -> "In My Pocket"
            is MainActivity -> "SmartSaver Dashboard"
            else -> "SmartSaver"
        }
    }

    // Enums for better type safety
    enum class BudgetAction {
        CREATE, VIEW
    }

    enum class AnalyticsType {
        MONTHLY, BREAKDOWN
    }

    enum class SmartFeature {
        IN_MY_POCKET
    }
}
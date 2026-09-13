package com.calculator.pcalculator

import kotlin.math.pow

object FinancialCalculator {

    // 3. EMI Calculator
    fun calculateEMI(principal: Double, rate: Double, months: Int): Double {
        val r = rate / 12 / 100
        return (principal * r * (1 + r).pow(months)) / ((1 + r).pow(months) - 1)
    }

    // 4. FDR Calculator
    data class FdrResult(val grossInterest: Double, val tax: Double, val netReturn: Double)
    
    fun calculateFDR(deposit: Double, years: Int, rate: Double, taxRate: Double): FdrResult {
        val grossInterest = (deposit * rate * years) / 100
        val taxAmount = (grossInterest * taxRate) / 100
        val net = deposit + grossInterest - taxAmount
        return FdrResult(grossInterest, taxAmount, net)
    }

    // 5. DPS Calculator (Recurring Deposit approx)
    fun calculateDPS(monthlyDeposit: Double, months: Int, rate: Double): Double {
        // Formula: M = P * n + P * n(n+1)/2 * r/12/100
        val totalDeposit = monthlyDeposit * months
        val interest = (monthlyDeposit * months * (months + 1) / 2) * (rate / 1200)
        return totalDeposit + interest
    }

    // 6. DBR Calculator
    fun calculateDBR(monthlyIncome: Double, existingEMI: Double, proposedEMI: Double): Pair<Double, String> {
        val totalLiability = existingEMI + proposedEMI
        val dbr = (totalLiability / monthlyIncome) * 100
        
        val status = when {
            dbr < 40 -> "Healthy"
            dbr < 60 -> "Caution"
            else -> "Warning" // > 60%
        }
        return Pair(dbr, status)
    }

    // 7. Employee Loan (ELSL) Logic
    fun calculateELSL(grossSalary: Double, takeHome: Double, spouseIncome: Double, tenureYears: Int): Double {
        // Example Logic based on standard banking norms
        // 1. Calculate Net Monthly Income (NMI)
        val nmi = takeHome + spouseIncome
        
        // 2. Max Allowable EMI (usually 40-50% of NMI)
        val maxEmi = nmi * 0.50 
        
        // 3. Calculate Loan Amount based on Max EMI and Tenure (Reverse EMI)
        // Using approximate reverse calculation or specific bank factor
        val rate = 9.0 // Assumption, make this an input
        val months = tenureYears * 12
        val r = rate / 12 / 100
        
        // P = (E * ((1+r)^n - 1)) / (r * (1+r)^n)
        val loanEligibility = (maxEmi * ((1 + r).pow(months) - 1)) / (r * (1 + r).pow(months))
        
        return loanEligibility
    }
}

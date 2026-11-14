package com.example.gstbillingapp.utils

import org.junit.Assert.*
import org.junit.Test

class ScanBillTest {
    @Test fun timesLine() {
        val i = BillTextParser.parseLine("Parle-G 5 x 10")!!
        assertEquals("Parle-G", i.name); assertEquals(5, i.quantity); assertEquals(10.0, i.price, 0.0)
    }
    @Test fun weightLineKeepsWeightInName() {
        val i = BillTextParser.parseLine("Chawal 2 kg 60")!!
        assertEquals("Chawal 2 kg", i.name); assertEquals(1, i.quantity); assertEquals(60.0, i.price, 0.0)
    }
    @Test fun lastNumberIsPrice() {
        val i = BillTextParser.parseLine("Soap Rs. 45")!!
        assertEquals("Soap", i.name); assertEquals(45.0, i.price, 0.0)
    }
    @Test fun skipsNoiseLines() {
        assertNull(BillTextParser.parseLine("12345"))
        assertNull(BillTextParser.parseLine("  "))
        assertEquals(2, BillTextParser.parse("Milk 30\n----\n1234\nSugar 2 x 45").size)
    }
    @Test fun suggestsKnownItem() {
        assertEquals(5.0, GstRates.suggest("Green Tea 250g")!!.rate, 0.0)
        assertEquals(0.0, GstRates.suggest("Amul Milk")!!.rate, 0.0)
    }
    @Test fun wordBoundary() {
        assertNull(GstRates.suggest("Steak")) // "tea" inside a word must not match
    }
    @Test fun ambiguousOrUnknownGivesNoSuggestion() {
        assertNull(GstRates.suggest("Parle-G Biscuit"))
        assertNull(GstRates.suggest("Random widget"))
        assertNull(GstRates.suggest(""))
    }
}

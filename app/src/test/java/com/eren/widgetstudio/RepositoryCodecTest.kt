package com.eren.widgetstudio

import com.eren.widgetstudio.data.DesignRepository
import com.eren.widgetstudio.data.TodoItem
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.withOpt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryCodecTest {
    private val sample = WidgetDesign(name = "Deneme", type = WidgetType.HABIT, todos = listOf(TodoItem("x", true)))
        .withOpt("week", false)

    @Test fun backupRoundTrip() {
        val text = DesignRepository.encodeBackup(listOf(sample))
        val back = DesignRepository.decodeBackup(text)!!
        assertEquals(listOf(sample), back)
    }

    @Test fun oneBrokenEntryDoesNotLoseOthers() {
        val good = DesignRepository.json.encodeToString(WidgetDesign.serializer(), sample)
        val raw = """[$good, {"type": 123, "bgColor": "oops"}]"""
        val decoded = DesignRepository.decodeLenient(raw)
        assertEquals(1, decoded.designs.size)
        assertEquals(1, decoded.skipped)
    }

    @Test fun oldDataWithoutNewFieldsStillLoads() {
        val old = """[{"id":"1","name":"Eski","type":"CLOCK","bgColor":4280295982,"todos":[{"text":"a"}]}]"""
        val decoded = DesignRepository.decodeLenient(old)
        assertEquals(1, decoded.designs.size)
        assertEquals("Eski", decoded.designs[0].name)
    }

    @Test fun unknownTypeFallsBackInsteadOfDropping() {
        val raw = """[{"id":"1","type":"FUTURE_WIDGET"}]"""
        assertEquals(1, DesignRepository.decodeLenient(raw).designs.size)
    }

    @Test fun garbageIsReportedAsCorruptAndRejectedOnImport() {
        assertTrue(DesignRepository.decodeLenient("not json").corrupt)
        assertNull(DesignRepository.decodeBackup("not json"))
        assertNull(DesignRepository.decodeBackup("42"))
        assertNotNull(DesignRepository.decodeBackup("[]"))
    }
}

package com.ward.desktop

import com.v2ray.ang.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourcesTest {
    @Test
    fun stringsAndArraysResolve() {
        assertEquals("Ward", AndroidResources.string(R.string.app_name))
        assertTrue(AndroidResources.array(R.array.securitys).contains("auto"))
    }

    @Test
    fun androidEscapesAreApplied() {
        assertEquals("it's \"x\"\nnext", AndroidResources.unescape("it\\'s \\\"x\\\"\\nnext"))
        assertEquals("a b", AndroidResources.unescape("a   \n  b"))
        assertEquals("keep   spaces", AndroidResources.unescape("\"keep   spaces\""))
    }
}

class UtilsTest {
    @Test
    fun urlAndDomainChecksTerminate() {
        assertTrue(com.v2ray.ang.util.Utils.isValidUrl("example.com"))
        assertTrue(com.v2ray.ang.util.Utils.isValidUrl("https://example.com/sub"))
        assertTrue(com.v2ray.ang.util.Utils.isDomainName("de.example.com"))
        kotlin.test.assertFalse(com.v2ray.ang.util.Utils.isValidUrl("not a url"))
    }
}

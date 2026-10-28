package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SceneItem
import com.example.data.model.SwitchRelay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Switchboard", appName)
    }

    @Test
    fun `switch relay model default test`() {
        val relay = SwitchRelay(
            id = 1,
            relayKey = "relay1",
            name = "Main Light",
            iconKey = "bulb",
            isOn = true
        )
        assertEquals(1, relay.id)
        assertTrue(relay.isOn)
        assertFalse(relay.isPending)
    }

    @Test
    fun `scene item summary test`() {
        val scene = SceneItem(
            id = "good_night",
            name = "Good Night",
            relay1 = false,
            relay2 = false,
            relay3 = false,
            relay4 = false
        )
        assertEquals("S1: OFF, S2: OFF, S3: OFF, S4: OFF", scene.summary())
    }
}

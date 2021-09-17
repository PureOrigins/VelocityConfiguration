package it.pureorigins.velocityconfiguration

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

fun Audience.sendMessage(component: Component?) {
    if (component != null) {
        sendMessage(component)
    }
}

fun Audience.sendActionBar(component: Component?) {
    if (component != null) {
        sendActionBar(component)
    }
}

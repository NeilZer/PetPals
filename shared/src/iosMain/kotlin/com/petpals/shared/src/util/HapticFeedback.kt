
package com.petpals.shared.src.util

actual object HapticFeedback {
    fun impact(intensity: HapticIntensity) {
        val style = when (intensity) {
            HapticIntensity.LIGHT -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
            HapticIntensity.MEDIUM -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
            HapticIntensity.HEAVY -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
        }
        val generator = UIImpactFeedbackGenerator(style)
        generator.impactOccurred()
    }

    fun notification(type: NotificationType) {
        val generator = UINotificationFeedbackGenerator()
        val feedbackType = when (type) {
            NotificationType.SUCCESS -> platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
            NotificationType.WARNING -> platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeWarning
            NotificationType.ERROR -> platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeError
        }
        generator.notificationOccurred(feedbackType)
    }

    actual fun selection() {
        val generator = UISelectionFeedbackGenerator()
        generator.selectionChanged()
    }
}

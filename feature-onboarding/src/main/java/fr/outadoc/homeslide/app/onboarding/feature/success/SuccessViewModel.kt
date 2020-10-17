/*
 * Copyright 2020 Baptiste Candellier
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fr.outadoc.homeslide.app.onboarding.feature.success

import android.app.ActivityManager
import fr.outadoc.homeslide.app.onboarding.navigation.NavigationEvent
import fr.outadoc.homeslide.common.preferences.GlobalPreferenceRepository
import io.uniflow.androidx.flow.AndroidDataFlow
import io.uniflow.core.flow.data.UIEvent

class SuccessViewModel(
    private val prefs: GlobalPreferenceRepository,
    private val activityManager: ActivityManager
) : AndroidDataFlow() {

    object ShowConfettiEvent : UIEvent()

    private val shouldShowConfetti: Boolean
        get() = !activityManager.isLowRamDevice

    fun onOpen() = action {
        if (shouldShowConfetti) {
            sendEvent { ShowConfettiEvent }
        }
    }

    fun onContinueClicked() = action {
        prefs.isOnboardingDone = true
        sendEvent { NavigationEvent.Next }
    }
}

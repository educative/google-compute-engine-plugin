/*
 * Copyright 2026 Educative
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jenkins.plugins.computeengine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

/**
 * Clouds whose VM GCP recently rejected (zone exhaustion, quota or any other create error).
 * {@link NoDelayProvisionerStrategy} tries them after every other cloud until the cooldown passes.
 * Kept in memory only: a controller restart treats every cloud as healthy again.
 */
public final class CloudFailureTracker {
    private static final Logger LOGGER = Logger.getLogger(CloudFailureTracker.class.getName());

    static final long COOLDOWN_MS = TimeUnit.MINUTES.toMillis(
            Long.getLong(CloudFailureTracker.class.getName() + ".cooldownMinutes", 5));

    private static final Map<String, Long> failedAt = new ConcurrentHashMap<>();

    // Replaced in tests.
    static LongSupplier clock = System::currentTimeMillis;

    private CloudFailureTracker() {}

    /** Records that GCP rejected a VM of the cloud with this Jenkins cloud id (e.g. "gce-pr"). */
    public static void markFailed(String cloudId, String reason) {
        failedAt.put(cloudId, clock.getAsLong());
        LOGGER.warning(String.format(
                "Cloud %s could not create a VM (%s); trying other clouds first for %d min",
                cloudId, reason, TimeUnit.MILLISECONDS.toMinutes(COOLDOWN_MS)));
    }

    /** Whether GCP rejected a VM of this cloud within the cooldown. */
    public static boolean isCoolingDown(String cloudId) {
        Long t = failedAt.get(cloudId);
        return t != null && clock.getAsLong() - t < COOLDOWN_MS;
    }

    static void reset() {
        failedAt.clear();
    }
}

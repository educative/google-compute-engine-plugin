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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CloudFailureTrackerTest {
    private final AtomicLong now = new AtomicLong(1_000_000L);

    @Before
    public void setUp() {
        CloudFailureTracker.reset();
        CloudFailureTracker.clock = now::get;
    }

    @After
    public void tearDown() {
        CloudFailureTracker.reset();
        CloudFailureTracker.clock = System::currentTimeMillis;
    }

    @Test
    public void cloudIsHealthyUntilItFails() {
        assertFalse(CloudFailureTracker.isCoolingDown("gce-pr"));
    }

    @Test
    public void failedCloudCoolsDownAndOnlyThatCloud() {
        CloudFailureTracker.markFailed("gce-pr", "ZONE_RESOURCE_POOL_EXHAUSTED: test");
        assertTrue(CloudFailureTracker.isCoolingDown("gce-pr"));
        assertFalse(CloudFailureTracker.isCoolingDown("gce-pr-1"));
    }

    @Test
    public void failedCloudRecoversAfterCooldown() {
        CloudFailureTracker.markFailed("gce-pr", "QUOTA_EXCEEDED: test");
        now.addAndGet(CloudFailureTracker.COOLDOWN_MS - 1);
        assertTrue(CloudFailureTracker.isCoolingDown("gce-pr"));
        now.addAndGet(1);
        assertFalse(CloudFailureTracker.isCoolingDown("gce-pr"));
    }
}

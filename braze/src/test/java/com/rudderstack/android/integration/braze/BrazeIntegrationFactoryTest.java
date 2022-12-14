package com.rudderstack.android.integration.braze;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


import com.rudderstack.android.sdk.core.RudderTraits;

import org.hamcrest.MatcherAssert;
import org.junit.Test;


public class BrazeIntegrationFactoryTest {
    @Test
    public void testCompareAddress() {
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(null, null), not(false));

        RudderTraits.Address currAddress = new RudderTraits.Address("city", null, null, null, null);
        RudderTraits.Address prevAddress = new RudderTraits.Address("city", null, null, null, null);
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(currAddress, null), not(true));
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(null, prevAddress), not(false));
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(currAddress, prevAddress), not(false));

    }
}
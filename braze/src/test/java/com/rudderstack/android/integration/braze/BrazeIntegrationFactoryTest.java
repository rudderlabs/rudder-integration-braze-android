package com.rudderstack.android.integration.braze;

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

        RudderTraits.Address address = new RudderTraits.Address();
        RudderTraits.Address address2 = new RudderTraits.Address()
                .putCity("City 2");
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address2, address), not(true));
        // if current city is null, consider unchanged
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address, address2), not(false));

        address = new RudderTraits.Address();
        address2 = new RudderTraits.Address()
                .putCountry("Country 2");
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address2, address), not(true));
        //if current country is null, consider unchanged
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address, address2), not(false));

        //country same, city different
        address = new RudderTraits.Address()
                .putCountry("country")
                .putCity("city1");
        address2 = new RudderTraits.Address()
                .putCountry("country")
                .putCity("city2");
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address2, address), not(true));

        //country different, city same
        address = new RudderTraits.Address()
                .putCountry("country1")
                .putCity("city");
        address2 = new RudderTraits.Address()
                .putCountry("country2")
                .putCity("city");
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address2, address), not(true));

       //same city same country
        address = new RudderTraits.Address()
                .putCountry("country")
                .putCity("city");
        address2 = new RudderTraits.Address()
                .putCountry("country")
                .putCity("city");
        MatcherAssert.assertThat(BrazeIntegrationFactory.compareAddress(address2, address), not(false));

    }
}
package com.applovin.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.android.gms.ads.AdRequest;

/**
 * Created by thomasso on 1/25/18.
 */

class AppLovinUtils
{
    private static final String DEFAULT_ZONE = "";

    /**
     * Keys for retrieving values from the server parameters.
     */
    private static class ServerParameterKeys
    {
        private static final String SDK_KEY   = "sdkKey";
        private static final String PLACEMENT = "placement";
    }

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server parameters, or Android Manifest.
     */
    static AppLovinSdk retrieveSdk(Bundle serverParameters, Context context)
    {
        final String sdkKey = serverParameters.getString( ServerParameterKeys.SDK_KEY );
        final AppLovinSdk sdk;

        if ( !TextUtils.isEmpty( sdkKey ) )
        {
            sdk = AppLovinSdk.getInstance( sdkKey, new AppLovinSdkSettings(), context );
        }
        else
        {
            sdk = AppLovinSdk.getInstance( context );
        }


        sdk.setPluginVersion( BuildConfig.VERSION_NAME );

        return sdk;
    }

    /**
     * Retrieves the placement from an appropriate connector object. Will use empty string if none exists.
     */
    static String retrievePlacement(Bundle serverParameters)
    {
        return serverParameters.getString( ServerParameterKeys.PLACEMENT );
    }

    /**
     * Retrieves the zone identifier from an appropriate connector object. Will use empty string if none exists.
     */
    static String retrieveZoneId(Bundle networkExtras)
    {
        if ( networkExtras != null && networkExtras.containsKey( AppLovinExtras.Keys.ZONE_ID ) )
        {
            return networkExtras.getString( AppLovinExtras.Keys.ZONE_ID );
        }
        else
        {
            return DEFAULT_ZONE;
        }
    }

    /**
     * Convert the given AppLovin SDK error code into the appropriate AdMob error code.
     */
    static int toAdMobErrorCode(int applovinErrorCode)
    {
        //
        // TODO: Be more exhaustive
        //

        if ( applovinErrorCode == AppLovinErrorCodes.NO_FILL )
        {
            return AdRequest.ERROR_CODE_NO_FILL;
        }
        else if ( applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT )
        {
            return AdRequest.ERROR_CODE_NETWORK_ERROR;
        }
        else
        {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}

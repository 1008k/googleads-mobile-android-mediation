/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.sdk;

import android.content.Context;
import android.net.Uri;

import java.util.Random;

/**
 * An example AdLoader that pretends to load native ads. It has methods that will be used by the
 * {@code SampleCustomEvent} and {@code SampleAdapter} to request native ads.
 */
public class SampleNativeAdLoader {
    private Context mContext;
    private String mAdUnit;
    private SampleNativeAdListener mListener;

    /**
     * Create a new {@link SampleInterstitial}.
     *
     * @param context An Android {@link Context}.
     */
    public SampleNativeAdLoader(Context context) {
        this.mContext = context;
    }

    /**
     * Sets the sample ad unit.
     *
     * @param sampleAdUnit The sample ad unit.
     */
    public void setAdUnit(String sampleAdUnit) {
        this.mAdUnit = sampleAdUnit;
    }

    /**
     * Sets a {@link SampleAdListener} to listen for ad events.
     *
     * @param listener The native ad listener.
     */
    public void setNativeAdListener(SampleNativeAdListener listener) {
        this.mListener = listener;
    }

    /**
     * Fetch an ad. Instead of doing an actual ad fetch, we will randomly decide to succeed, or
     * fail with different error codes.
     *
     * @param request The ad request with targeting information.
     */
    public void fetchAd(SampleNativeAdRequest request) {
        // Check for conditions that constitute a bad request.
        if ((mListener == null) || (mAdUnit == null)
                || (!request.areContentAdsRequested() && !request.areAppInstallAdsRequested())) {
            mListener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
            return;
        }

        Random random = new Random();
        int nextInt = random.nextInt(100);
        if (mListener != null) {
            if (nextInt < 80) {
                // Act as if the request was successful and create a sample native ad
                // of the request type filled with dummy data.
                if (request.areAppInstallAdsRequested()
                        && (!request.areContentAdsRequested()
                        || random.nextBoolean())) {
                    mListener.onNativeAppInstallAdFetched(createFakeAppInstallAd(request));
                } else {
                    mListener.onNativeContentAdFetched(createFakeContentAd(request));
                }
            } else if (nextInt < 85) {
                mListener.onAdFetchFailed(SampleErrorCode.UNKNOWN);
            } else if (nextInt < 90) {
                mListener.onAdFetchFailed(SampleErrorCode.BAD_REQUEST);
            } else if (nextInt < 95) {
                mListener.onAdFetchFailed(SampleErrorCode.NETWORK_ERROR);
            } else if (nextInt < 100) {
                mListener.onAdFetchFailed(SampleErrorCode.NO_INVENTORY);
            }
        }
    }

    private SampleNativeAppInstallAd createFakeAppInstallAd(SampleNativeAdRequest request) {
        SampleNativeAppInstallAd fakeAd = new SampleNativeAppInstallAd();

        fakeAd.setHeadline("Sample App!");
        fakeAd.setBody("This app doesn't actually exist.");
        fakeAd.setCallToAction("Take Action!");
        fakeAd.setDegreeOfAwesomeness("Quite Awesome");
        fakeAd.setPrice(1.99);
        fakeAd.setStarRating(4.5);
        fakeAd.setStoreName("Sample Store");
        fakeAd.setImageUri(Uri.parse("http://www.example.com/"));
        fakeAd.setAppIconUri(Uri.parse("http://www.example.com/"));

        // There are other options offered in the SampleNativeAdRequest,
        // but for simplicity's sake, this is the only one we'll put to use.
        if (request.getShouldDownloadImages()) {
            fakeAd.setAppIcon(mContext.getResources()
                    .getDrawable(R.drawable.sample_app_icon));
            fakeAd.setImage(mContext.getResources()
                    .getDrawable(R.drawable.sample_app_image));
        }

        return fakeAd;
    }

    private SampleNativeContentAd createFakeContentAd(SampleNativeAdRequest request) {
        SampleNativeContentAd fakeAd = new SampleNativeContentAd();

        fakeAd.setHeadline("Sample Content!");
        fakeAd.setBody("This is a sample ad, so there's no real content. In the event of a real "
                + "ad, though, some persuasive text would appear here.");
        fakeAd.setCallToAction("Take Action!");
        fakeAd.setDegreeOfAwesomeness("Fairly Awesome");

        // There are other options offered in the SampleNativeAdRequest,
        // but for simplicity's sake, this is the only one we'll put to use.
        if (request.getShouldDownloadImages()) {
            fakeAd.setLogo(mContext.getResources()
                    .getDrawable(R.drawable.sample_content_logo));
            fakeAd.setImage(mContext.getResources()
                    .getDrawable(R.drawable.sample_content_ad_image));
        }

        return fakeAd;
    }
}

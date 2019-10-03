package com.google.ads.mediation.fyber;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.VideoContentListenerAdapter;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * Class for rendering a Fyber Marketplace rewarded video
 */
public class FyberRewardedVideoRenderer implements MediationRewardedAd {
    private final static String TAG = FyberRewardedVideoRenderer.class.getSimpleName();;

    /** AdMob's Interstitial ad configuration object */
    MediationRewardedAdConfiguration mAdConfiguration;
    /** AdMob's callback object */
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;

    // TODO: Can we somehow separate AdMob from DFP?
    private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

    /**
     * AbMob's rewarded ad callback. as returned from {@link MediationAdLoadCallback#onSuccess}
     */
    private MediationRewardedAdCallback mRewardedAdCallback;

    /**
     * The Spot object for the banner
     */
    private InneractiveAdSpot mRewardedSpot;
    private InneractiveFullscreenUnitController mUnitController;
    private boolean mReceivedRewardItem = false;

    /**
     * The event listener of the Ad
     */
    private InneractiveFullscreenAdEventsListener mAdListener;

    /**
     * Constructor
     * @param adConfiguration AdMob interstitial ad configuration
     * @param adLoadCallback AdMob load callback
     */
    FyberRewardedVideoRenderer(MediationRewardedAdConfiguration adConfiguration,
                                      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
        mAdConfiguration = adConfiguration;
        mAdLoadCallback = adLoadCallback;
    }

    void render() {
        // Check that we got a valid spot id from the server
        String spotId = mAdConfiguration.getServerParameters().getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mAdLoadCallback.onFailure("Cannot render interstitial ad. Please define a valid spot id on the AdMob console");
            return;
        }

        mRewardedSpot = InneractiveAdSpotManager.get().createSpot();
        mRewardedSpot.setMediationName(MEDIATOR_NAME);

        mUnitController = new InneractiveFullscreenUnitController();
        mRewardedSpot.addUnitController(mUnitController);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);

        // TODO: Parse network extras
        initRequestListener();

        mRewardedSpot.requestAd(request);
    }

    private void initRequestListener() {
        mRewardedSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                if (adSpot != mRewardedSpot) {
                    Log.d(TAG, "Wrong Interstitial Spot: Received - " + adSpot + ", Actual - " + mRewardedSpot);
                    return;
                }

                // Report load success to AdMob, and cache the returned callback for a later use
                mRewardedAdCallback = mAdLoadCallback.onSuccess(FyberRewardedVideoRenderer.this);
                mAdListener = createFyberAdListener(mUnitController, mRewardedAdCallback);
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                                     InneractiveErrorCode errorCode) {
                mAdLoadCallback.onFailure("Error code: " + errorCode.toString());
            }
        });
    }

    /**
     * Creates a listener for Fyber's fullscreen placement events
     * @param controller the full screen controller
     * @param callback Google's rewarded ad callback
     * @return the created events listener
     */
    private InneractiveFullscreenAdEventsListenerAdapter createFyberAdListener(InneractiveFullscreenUnitController controller, final MediationRewardedAdCallback callback) {
        InneractiveFullscreenAdEventsListenerAdapter adListener = new InneractiveFullscreenAdEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
                callback.onAdOpened();
                callback.onVideoStart();
                callback.reportAdImpression();
            }

            @Override
            public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
                callback.reportAdClicked();
            }

            @Override
            public void onAdDismissed(InneractiveAdSpot inneractiveAdSpot) {
                callback.onAdClosed();
                userEarnedReward(callback);
            }
        };

        // Listen to video completion event
        InneractiveFullscreenVideoContentController videoContentController =
                new InneractiveFullscreenVideoContentController();

        videoContentController.setEventsListener(new VideoContentListenerAdapter() {
            /**
             * Called by inneractive when an Intersititial video ad was played to the end
             * <br>Can be used for incentive flow
             * <br>Note: This event does not indicate that the interstitial was closed
             */
            @Override
            public void onCompleted() {
                callback.onVideoComplete();
                userEarnedReward(callback);
            }
        });

        controller.addContentController(videoContentController);
        controller.setEventsListener(mAdListener);

        return adListener;
    }

    @Override
    public void showAd(Context context) {
        // TODO: What about show errors. Is there an interface for that?
        if (mRewardedSpot != null && mUnitController != null && mRewardedSpot.isReady()) {
            mUnitController.show(context);
        } else if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdFailedToShow("showAd called, but Fyber's rewarded spot is not ready");
        }
    }

    /**
     * Small helper method, in order to report user earned reward for both video and mraid video ads
     * @param callback
     */
    private void userEarnedReward(MediationRewardedAdCallback callback) {
        if(!mReceivedRewardItem) {
            mReceivedRewardItem = true;

            callback.onUserEarnedReward(RewardItem.DEFAULT_REWARD);
        }
    }
}

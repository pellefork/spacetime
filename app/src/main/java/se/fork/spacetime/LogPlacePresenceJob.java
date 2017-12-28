package se.fork.spacetime;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.utils.LocalStorage;

/**
 * Created by per.fork on 2017-12-28.
 */

public class LogPlacePresenceJob extends Job {

    static final String TAG = "detect_place_presence";  // TODO Externalize this into a Constants class
    static final int FREQ_IN_MINUTES = 15;               // TODO Parameterize this from Settings
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private Location lastKnownLocation;
    private boolean isAPIClientPrepared;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private CountDownLatch countDownLatch;

    public LogPlacePresenceJob() {
        super();
        isAPIClientPrepared = false;
    }

    private void prepareAPIClient() {

        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(this.locationRequest);
        this.locationSettingsRequest = builder.build();

        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult); // why? this. is. retarded. Android.
                lastKnownLocation = locationResult.getLastLocation();

                Log.i(TAG, "Location Callback results: " + lastKnownLocation);
            }
        };

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());


        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRunJob:Bad permission setting"); // TODO Copy this to correct activity in order to make sure it's OK once we get here
        } else {
            Log.d(TAG, "onRunJob: OK permissions, issuing location request");
            Looper.prepare();
            mFusedLocationClient.requestLocationUpdates(this.locationRequest, this.locationCallback, Looper.myLooper());
            // mFusedLocationClient.requestLocationUpdates(this.locationRequest, this.locationCallback, null);
        }
        isAPIClientPrepared = true;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        Log.d(TAG, "onRunJob: Entering method");

        if(!isAPIClientPrepared) {
            prepareAPIClient();
            return Result.SUCCESS;
        } else {
            LatLng pos = findLastKnownPosition();
            if(pos != null) {
                List<String> keys = LocalStorage.getInstance().getMyPlaceLists(getContext()).getKeys();
                for(String key: keys) {
                    LoggablePlaceList placeList = LocalStorage.getInstance().getLoggablePlaceList(getContext(), key);
                    for(LoggablePlace place: placeList.getLoggablePlaces()) {
                        logPresence(place, pos);
                    }
                }
                return Result.SUCCESS;
            } else {
                return Result.RESCHEDULE;
            }
        }

    }

    private LatLng findLastKnownPosition() {
        if(lastKnownLocation != null) {
            return new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        } else {
            return null;
        }
    }

    private void notifyUser(String message) {
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0,
                new Intent(getContext(), StartActivity.class), 0);

        Notification notification = new NotificationCompat.Builder(getContext(), "default")
                .setContentTitle("Spacetime has checked your presence")
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setColor(Color.RED)
                .setLocalOnly(true)
                .build();

        NotificationManagerCompat.from(getContext())
                .notify(new Random().nextInt(), notification);
    }

    private void logPresence(LoggablePlace place, LatLng pos) {
        if(place.isInPlace(pos)) {
            Log.d(TAG, "Presence positive in " + place.getName() + ", " + place.getAddress());
            notifyUser("Presence positive in " + place.getName() + ", " + place.getAddress());
        } else {
            Log.d(TAG, "Presence negative in " + place.getName() + ", " + place.getAddress());
            notifyUser("Presence negative in " + place.getName() + ", " + place.getAddress());
        }
    }

    public static void schedulePeriodic() {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(FREQ_IN_MINUTES), TimeUnit.MINUTES.toMillis(5))
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void runJobImmediately() {
        new JobRequest.Builder(TAG)
                .startNow()
                .build()
                .schedule();
    }
}

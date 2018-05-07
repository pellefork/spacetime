package se.fork.spacetime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.fork.spacetime.database.PlaceLogEntry;
import se.fork.spacetime.database.SpacetimeDatabase;
import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.Presence;
import se.fork.spacetime.utils.GeofenceErrorMessages;
import se.fork.spacetime.utils.LocalStorage;

/**
 * Created by per.fork on 2018-04-26.
 */

public class GeofenceService extends JobIntentService {

    private static final int JOB_ID = 573;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";

    private static final String PACKAGE_NAME = "se.fork.spacetime.geofenceservice";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    static final String EXTRA_PRESENCE = PACKAGE_NAME + ".presence";

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        Log.d("GeofenceService", "enqueueWork: Enter method");
        enqueueWork(context, GeofenceService.class, JOB_ID, intent);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleWork(Intent intent) {
        Log.d(this.getClass().getSimpleName(), "onHandleWork: Enter method");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            saveTransitionDetails(geofenceTransition, triggeringGeofences);

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition,
                    triggeringGeofences);

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails);
            Log.i(TAG, "onHandleWork: Geofencing transition: " + geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */

    private void saveTransitionDetails(int geofenceTransition,
                                       List<Geofence> triggeringGeofences) {
        for (Geofence geofence: triggeringGeofences) {
            handleTransition(geofence, geofenceTransition);
        }

    }

    private void handleTransition(Geofence geofence, int transition) {
        Log.i(TAG, "onNewTransition: " + transition + " on " + geofence);

        Presence presence = doHandleTransition(geofence, transition);

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);
        // intent.putExtra(EXTRA_LOCATION, location);
        intent.putExtra(EXTRA_PRESENCE, presence);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

    }

    private Presence doHandleTransition(Geofence geofence, int transition) {
        Presence presence = null;
        if(geofence != null) {
            List<String> presentPlaces = new ArrayList<>();
            List<String> keys = LocalStorage.getInstance().getMyPlaceLists(this).getKeys();
            for(String key: keys) {
                LoggablePlaceList placeList = LocalStorage.getInstance().getLoggablePlaceList(this, key);
                Log.d(this.getClass().getSimpleName(), "doHandleTransition: geofence id = " + geofence.getRequestId() );


                for(LoggablePlace place: placeList.getLoggablePlaces().values()) {
                    if(place.isEnabled() && (place.getId().equals(geofence.getRequestId()))) {
                        Log.d(this.getClass().getSimpleName(), "doHandleTransition: Match on place = " + place );
                        boolean inPlace = transition == Geofence.GEOFENCE_TRANSITION_ENTER;
                        if (place.isInside() != inPlace) {  // Only log changed insideness
                            addLogEntry(placeList, place, inPlace);
                            place.setInside(inPlace);
                            LocalStorage.getInstance().saveLoggablePlaceList(placeList, this);
                        }
                        if (inPlace) {
                            presentPlaces.add(place.getId());
                        }
                    } else {
                        Log.d(this.getClass().getSimpleName(), "doHandleTransition: Miss on place = " + place );
                    }
                }
            }
            presence = new Presence(presentPlaces);
        }
        return presence;
    }

    // TODO Change timestamp to actual timestamp from queued transition rather than now. Important for Android O and above.

    private void addLogEntry(LoggablePlaceList list, LoggablePlace place, boolean inPlace) {
        PlaceLogEntry entry = new PlaceLogEntry(place.getId(), place.getName(), list.getName(), inPlace, new Date().getTime());
        SpacetimeDatabase db = SpacetimeDatabase.getSpacetimeDatabase(this);
        db.placeLogEntryDao().insertAll(entry);
        Log.d(this.getClass().getSimpleName(), "addLogEntry: Wrote to db: " + entry);
    }


    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(LocalStorage.getInstance().getPlaceFromId(geofence.getRequestId(), getApplicationContext()).getName());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the StartActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), StartActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }
}

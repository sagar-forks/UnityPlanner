package com.nbdeg.unityplanner.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nbdeg.unityplanner.R;
import com.nbdeg.unityplanner.data.Assignments;
import com.nbdeg.unityplanner.data.Classes;

import java.util.ArrayList;

public class Database {

    private static final String TAG = "Database";
    private ArrayList<Assignments> assignmentList = new ArrayList<>();
    private ArrayList<Classes> classList = new ArrayList<>();

    public static FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    public static DatabaseReference doneAssignmentsDb = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("assignments").child("done");
    public static DatabaseReference classDb = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("classes");
    public static DatabaseReference dueAssignmentsDb = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("assignments").child("due");

    // Gets all assignments
    public ArrayList<Assignments> getAssignments() {
        assignmentList.clear();
        doneAssignmentsDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                    Assignments assignment = userSnapshot.getValue(Assignments.class);
                    assignmentList.add(assignment);
                    Log.i(TAG, "Assignment loaded: " + assignment.getName());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading assignments: " + databaseError.getMessage());
            }
        });
        return assignmentList;
    }

    // Gets all classes
    public ArrayList<Classes> getClasses() {
        classList.clear();
        classDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    Classes mClass = userSnapshot.getValue(Classes.class);
                    classList.add(mClass);
                    Log.i(TAG, "Class loaded: " + mClass.getName());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading classes: " + databaseError.getMessage());
            }
        });
        return classList;
    }


    // Adding objects to database
    public void addAssignment(Assignments assignment, Context context) {
        Log.i(TAG, "Creating assignment: " + assignment.getName());

        // Notification
        // scheduleNotification(getNotification(assignment, context), assignment.getDueDate(), context, assignment);

        // Database
        String key = dueAssignmentsDb.push().getKey();
        assignment.setID(key);
        dueAssignmentsDb.child(key).setValue(assignment);
    }

    public void addClass(Classes mClass) {
        Log.i(TAG, "Creating class: " + mClass.getName());
        String key = classDb.push().getKey();
        mClass.setID(key);
        classDb.child(key).setValue(mClass);
    }

    public void finishAssignment(Assignments finishedAssignment, boolean isExisting, Context context) {
        if (isExisting) {
            // cancelNotification(context, finishedAssignment);
            dueAssignmentsDb.child(finishedAssignment.getID()).removeValue();
            doneAssignmentsDb.child(finishedAssignment.getID()).setValue(finishedAssignment);
        } else {
            Log.i(TAG, "Creating finished assignment: " + finishedAssignment.getName());
            doneAssignmentsDb.push().setValue(finishedAssignment);
        }
    }

    // Editing existing objects in database
    public void editClass(final String oldID, final Classes newClass) {
        classDb.child(oldID).setValue(newClass);
    }

    public void editAssignment(final Assignments newAssignment, Context context) {
        if (newAssignment.getPercent() == 100) {
            finishAssignment(newAssignment, true, context);
        } else {
            // editNotification(context, newAssignment);
            dueAssignmentsDb.child(newAssignment.getID()).setValue(newAssignment);
        }
    }

    private void cancelNotification(Context context, Assignments assignments) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(assignments.getNotificationIntent());
    }

    private void editNotification(Context context, Assignments assignments) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(assignments.getNotificationIntent());
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, assignments.getDueDate(), assignments.getNotificationIntent());
    }

    private void scheduleNotification(Notification notification, long notifyTime, Context context, Assignments assignment) {
        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, assignment.getID());
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        assignment.setNotificationIntent(pendingIntent);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, notifyTime, pendingIntent);
    }

    private Notification getNotification(Assignments assignment, Context context) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle("Assignment Due Tomorrow");
        builder.setContentText(assignment.getName());
        builder.setSmallIcon(R.drawable.ic_assignments_due);
        return builder.build();
    }
}

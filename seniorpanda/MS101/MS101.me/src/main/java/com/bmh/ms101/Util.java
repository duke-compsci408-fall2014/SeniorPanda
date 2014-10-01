package com.bmh.ms101;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.ex.UserIsOfflineException;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import org.droidparts.net.http.HTTPException;
import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;

/**
 * Utility class with common methods used all over the place
 */
class Util {
    // Methods to color views by use of filters
    public static void makeGreen(View view, Context ctx) {
        applyFilter(R.color.green_filter, view, ctx);
    }

    public static void makeRed(View view, Context ctx) {
        applyFilter(R.color.red_filter, view, ctx);
    }

    public static void makeBlue(View view, Context ctx) {
        applyFilter(R.color.blue_filter, view, ctx);
    }

    public static void makeYellow(View view, Context ctx) {
        applyFilter(R.color.yellow_filter, view, ctx);
    }

    private static void applyFilter(int colorId, View view, Context ctx) {
        view.getBackground().setColorFilter(ctx.getResources().getColor(colorId),
                PorterDuff.Mode.MULTIPLY);
    }

    // Toasts
    public static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context ctx, int resId) {
        toast(ctx, ctx.getString(resId));
    }

    // Google Analytics methods
    public static void trackUiEvent(String action, Context ctx) {
        trackEvent("ui_action", action, ctx);
    }

    public static void trackBgEvent(String action, Context ctx) {
        trackEvent("bg_action", action, ctx);
    }

    private static void trackEvent(String category, String action, Context ctx) {
        EasyTracker easyTracker = EasyTracker.getInstance(ctx);
        easyTracker.send(MapBuilder.createEvent(category, // Event category (required)
                action, // Event action (required)
                null, // Event label
                null) // Event value
                .build());
    }

    /**
     * Creates a simple dialog that shows a message and has one button intended to only mDismiss it
     * @param ctx Context to create dialog with
     * @param title Resource ID for the title text
     * @param message Resource ID for the message text
     * @param neutralButtonText Resource ID for the button text
     */
    public static void buildInfoDialog(Context ctx, int title, int message, int neutralButtonText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(title).setMessage(message)
                .setNeutralButton(neutralButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
        builder.show().setCanceledOnTouchOutside(true);
    }

    /**
     * Handles when a send job fails.
     * @param activity The activity to use as context
     * @param e The reported exception
     */
    public static void handleSendJobFailure(Activity activity, Exception e) {
        if (e instanceof UserIsOfflineException) {
            e.printStackTrace();
            toast(activity, R.string.toast_error_offline);
        } else if (e instanceof HTTPException) {
            e.printStackTrace();
            toast(activity, R.string.toast_new_backend_error);
        } else if (e instanceof JSONException) {
            e.printStackTrace();
            toast(activity, R.string.toast_json_error);
        }
    }

    /**
     * Handles when a get job fails.
     * @param activity The activity to use as context
     * @param e The reported exception
     */
    public static void handleGetJobFailure(Activity activity, Exception e) {
        if (e instanceof UserIsOfflineException) {
            e.printStackTrace();
            toast(activity, R.string.toast_error_offline);
        } else if (e instanceof HTTPException) {
            e.printStackTrace();
            toast(activity, R.string.toast_new_backend_error);
        } else if (e instanceof JSONException) {
            e.printStackTrace();
            toast(activity, R.string.toast_json_error);
        }
    }

    /**
     * Handles the login response when it throws an Exception.
     * This method is NOT SAFE to call from any thread but the UI thread!
     * @param activity The activity to use as context
     * @param user The User to use (from the activity)
     * @param e The Exception we got in response to our login request
     */
    public static void handleDFLoginError(Activity activity, User user, Exception e) {
        if (e instanceof JSONException) {
            EventBus.getDefault().post(new DFLoginResponseEvent("JSON Exception"));
        } else if (e instanceof HTTPException) {
            HTTPException httpException = (HTTPException) e;
            // See if we failed because of us or the server
            String httpErrorMessage = "";
            try {
                httpErrorMessage = new JSONObject(httpException.getMessage()).getJSONArray("error").getJSONObject(0).getString("message");
            } catch (JSONException jsonException) {
                jsonException.printStackTrace(); // We really don't care
            }
            if (httpException.getResponseCode() == 400) {
                // We passed the server invalid credentials, let's try to get correct ones
                showDFLoginDialog(activity, user, httpErrorMessage);
            } else {
                // The server is having problems, just stop trying
                httpErrorMessage = httpException.getMessage();
                toast(activity, httpErrorMessage);
                EventBus.getDefault().post(new DFLoginResponseEvent("Server Problems"));
            }
        }
    }

    /**
     * Shows the DreamFactory login dialog so that the user can try and log in.
     * @param activity The activity to use as context
     * @param user The User to use (from the activity)
     * @param errorMessage An error string to display, if any
     */
    public static void showDFLoginDialog(Activity activity, User user, String errorMessage) {
        // Get our views that will be used in the dialog
        LinearLayout dialogView = (LinearLayout) ((LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_df_login, null);
        final TextView tvError = (TextView) dialogView.findViewById(R.id.df_login_error_msg);
        tvError.setText(errorMessage);
        final EditText etEmail = (EditText) dialogView.findViewById(R.id.email_addr);
        etEmail.setText(user.getDFEmail());
        final EditText etPassword = (EditText) dialogView.findViewById(R.id.password);
        etPassword.setText(user.getDFPass());
        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.df_login_prompt)
                .setView(dialogView)
                .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Record the email and password that the user provided
                        User innerUser = new User(MS101.getInstance());
                        innerUser.recordDFEmail(etEmail.getText().toString());
                        innerUser.recordDFPass(etPassword.getText().toString());
                        EventBus.getDefault().post(new DFLoginResponseEvent("Retry"));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel trying to login
                        EventBus.getDefault().post(new DFLoginResponseEvent("Cancelled"));
                    }
                })
                .show();
    }
}

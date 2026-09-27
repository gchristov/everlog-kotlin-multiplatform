package com.everlog.managers.apprate;

import com.everlog.config.AppConfig;
import com.everlog.config.HomeNotification;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AppLaunchManager {

    public static final AppLaunchManager manager = new AppLaunchManager();

    public void launchApp() {
        AppLaunchState.state.setLastLaunchDate(new Date());
    }

    /**
     * Records that the user is using the app, other than launching it. Moves the last launch date on so
     * it also serves as "last active" for the app usage reminder, without counting as a launch.
     */
    public void recordActivity(Date now) {
        AppLaunchState.state.setLastLaunchDate(now);
    }

    // Convenience

    public long lastActiveDate() {
        return AppLaunchState.state.getLastLaunchDate();
    }

    public void clearAppUserData() {
        AppLaunchState.state.clearState();
    }

    // Triggers

    public boolean shouldShowHomeNotification(HomeNotification notification) {
        if (notification != null && notification.canShow()) {
            String lastDismissedId = AppLaunchState.state.homeNotificationLastDismissedId();
            return !notification.dismissalId().equals(lastDismissedId);
        }
        return false;
    }

    public void homeNotificationDismissed(HomeNotification notification) {
        if (notification != null) {
            AppLaunchState.state.setHomeNotificationLastDismissedId(notification);
        }
    }

    public boolean shouldPromptAppUpdate(int availableVersionCode, Date now) {
        if (AppLaunchState.state.appUpdateDeclinedVersionCode() != availableVersionCode) {
            // Never declined, or a newer version than the one declined is available
            return true;
        }
        return calendarDaysSinceDate(AppLaunchState.state.appUpdateDeclinedDate(), now) >= AppConfig.configuration.getAppUpdateRepromptDelayDays();
    }

    public void appUpdateDeclined(int versionCode, Date now) {
        AppLaunchState.state.setAppUpdateDeclined(versionCode, now);
    }

    // Utils

    private long calendarDaysSinceDate(long date, Date now) {
        if (date <= 0) {
            return -1L;
        } else {
            Date today = stripTimeComponents(now);
            Date then = stripTimeComponents(new Date(date));
            return timeDifference(today, then, TimeUnit.DAYS);
        }
    }

    private long timeDifference(Date now, Date then, TimeUnit timeUnit) {
        if (now == null || then == null) {
            return -1L;
        } else {
            long diff = now.getTime() - then.getTime();
            return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
        }
    }

    private Date stripTimeComponents(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}

package com.everlog.managers.apprate;

import com.everlog.config.AppConfig;
import com.everlog.config.HomeNotification;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AppLaunchManager {

    public static final AppLaunchManager manager = new AppLaunchManager();

    public void launchApp() {
        long firstLaunchDate = AppLaunchState.state.getFirstLaunchDate();
        Date now = new Date();
        if (firstLaunchDate < 0) {
            // App has not been launched yet
            AppLaunchState.state.setFirstLaunchDate(now);
        } else {
            // App has been launched before, calculate how many calendar days have passed, ignoring time
            long daysSinceLastLaunch = calendarDaysSinceDate(AppLaunchState.state.getLastLaunchDate());
            AppLaunchState.state.setDaysSinceLastLaunch(daysSinceLastLaunch);
        }
        AppLaunchState.state.setLastLaunchDate(now);

        // Update app launch count
        int launchCount = AppLaunchState.state.getLaunchCount();
        AppLaunchState.state.setLaunchCount(launchCount + 1);

        // Update consecutive launch count
        long daysSinceLastLaunch = daysSinceLastLaunch();
        if (daysSinceLastLaunch == 1) {
            int consecutiveLaunchCount = AppLaunchState.state.getConsecutiveDaysLaunchCount();
            AppLaunchState.state.setConsecutiveDaysLaunchCount(consecutiveLaunchCount + 1);
        } else if (daysSinceLastLaunch > 1) {
            AppLaunchState.state.setConsecutiveDaysLaunchCount(1);
        }

        // Check rate triggers
        if (numberOfAppLaunches() % AppConfig.configuration.getRateTriggerModLaunchNumber() == 0 // App launched every X times
                || numberOfConsecutiveAppLaunchDays() % AppConfig.configuration.getRateTriggerModConsecutiveLaunchDays() == 0) // App launched every X consecutive days
        {
            rateActionTrigger();
        }
    }

    // Convenience

    public void clearAppUserData() {
        AppLaunchState.state.clearState();
    }

    public boolean appLaunchedOnce() {
        return numberOfAppLaunches() > 0;
    }

    public long daysSinceFirstLaunch() {
        return calendarDaysSinceDate(AppLaunchState.state.getFirstLaunchDate());
    }

    public long daysSinceLastLaunch() {
        return AppLaunchState.state.getDaysSinceLastLaunch();
    }

    public int numberOfAppLaunches() {
        return AppLaunchState.state.getLaunchCount();
    }

    public int numberOfConsecutiveAppLaunchDays() {
        return AppLaunchState.state.getConsecutiveDaysLaunchCount();
    }

    private long daysSinceLastRatePrompt() {
        return calendarDaysSinceDate(AppLaunchState.state.rateLastPromptDate());
    }

    // Triggers

    public boolean shouldShowHomeNotification(HomeNotification notification) {
        if (notification != null && notification.canShow()) {
            int lastHash = AppLaunchState.state.homeNotificationLastHash();
            return lastHash != notification.hashCode();
        }
        return false;
    }

    public void homeNotificationDismissed(HomeNotification notification) {
        if (notification != null) {
            AppLaunchState.state.setHomeNotificationLastHash(notification);
        }
    }

    public boolean shouldShowAppRating() {
        if (AppLaunchState.state.rateScheduledToShow()) {
            // Ensure dialog is not shown less than X days since the last time it was visible, unless it was never shown
            return AppLaunchState.state.rateLastPromptDate() < 0
                    || daysSinceLastRatePrompt() >= AppConfig.configuration.getRateTriggerLastShownDelayDays();
        }
        return false;
    }

    public void rateDialogDismissed() {
        AppLaunchState.state.setRateLastPromptDate(new Date());
        AppLaunchState.state.setRateScheduledToShow(false);
    }

    public void rateActionTrigger() {
        AppLaunchState.state.setRateScheduledToShow(true);
    }

    // Utils

    private long calendarDaysSinceDate(long date) {
        if (date <= 0) {
            return -1L;
        } else {
            Date now = stripTimeComponents(new Date());
            Date then = stripTimeComponents(new Date(date));
            return timeDifference(now, then, TimeUnit.DAYS);
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

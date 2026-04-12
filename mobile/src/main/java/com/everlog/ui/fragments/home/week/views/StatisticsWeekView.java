package com.everlog.ui.fragments.home.week.views;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.everlog.R;
import com.everlog.data.controllers.statistics.UserStatsController;
import com.everlog.data.model.WeekDay;
import com.everlog.managers.auth.LocalUserManager;
import com.everlog.managers.preferences.SettingsManager;
import com.everlog.ui.activities.base.BaseActivity;
import com.everlog.ui.views.EmptyView;
import com.everlog.ui.views.WeekDayView;
import com.everlog.ui.views.summarycard.SummaryCardWeek;
import com.everlog.utils.DayOfWeekExtKt;
import com.everlog.utils.ViewUtils;
import com.everlog.utils.format.StatsFormatUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.vaibhavlakhera.circularprogressview.CircularProgressView;

import java.util.List;

import androidx.annotation.Nullable;

public class StatisticsWeekView implements IWeekView {

    View mParentView;
    View mContentView;
    ShimmerFrameLayout mShimmerContainer;
    EmptyView mEmptyView;
    View mWeekDaysView;
    WeekDayView mDay1View;
    WeekDayView mDay2View;
    WeekDayView mDay3View;
    WeekDayView mDay4View;
    WeekDayView mDay5View;
    WeekDayView mDay6View;
    WeekDayView mDay7View;
    SummaryCardWeek mWorkoutsCompletedSummary;
    SummaryCardWeek mTotalWeightSummary;
    SummaryCardWeek mTotalTimeSummary;
    SummaryCardWeek mAverageTimeSummary;
    CircularProgressView mGoalProgress;
    View mGoalPendingLayout;
    View mGoalAchievedLayout;
    TextView mGoalTotalLbl;

    private Context mContext;
    private StatisticsWeekListener mListener;

    @Override
    public void onCreateView(View view) {
        this.mContext = view.getContext();
        mParentView = view.findViewById(R.id.weekParentView);
        mContentView = view.findViewById(R.id.weekContentView);
        mShimmerContainer = view.findViewById(R.id.weekShimmerView);
        mEmptyView = view.findViewById(R.id.weekEmptyView);
        mWeekDaysView = view.findViewById(R.id.weekDaysView);
        mDay1View = view.findViewById(R.id.day1View);
        mDay2View = view.findViewById(R.id.day2View);
        mDay3View = view.findViewById(R.id.day3View);
        mDay4View = view.findViewById(R.id.day4View);
        mDay5View = view.findViewById(R.id.day5View);
        mDay6View = view.findViewById(R.id.day6View);
        mDay7View = view.findViewById(R.id.day7View);
        mWorkoutsCompletedSummary = view.findViewById(R.id.workoutsCompletedSummary);
        mTotalWeightSummary = view.findViewById(R.id.totalWeightSummary);
        mTotalTimeSummary = view.findViewById(R.id.totalTimeSummary);
        mAverageTimeSummary = view.findViewById(R.id.averageTimeSummary);
        mGoalProgress = view.findViewById(R.id.goalProgress);
        mGoalPendingLayout = view.findViewById(R.id.goalPendingLayout);
        mGoalAchievedLayout = view.findViewById(R.id.goalAchievedLayout);
        mGoalTotalLbl = view.findViewById(R.id.goalTotalLbl);
        setupEmptyView();
        setupClicks(view);
    }

    private void setupClicks(View view) {
        View.OnClickListener statsListener = v -> onClickStats();
        if (mWeekDaysView != null) mWeekDaysView.setOnClickListener(statsListener);
        if (mWorkoutsCompletedSummary != null) mWorkoutsCompletedSummary.setOnClickListener(statsListener);
        if (mTotalWeightSummary != null) mTotalWeightSummary.setOnClickListener(statsListener);
        if (mTotalTimeSummary != null) mTotalTimeSummary.setOnClickListener(statsListener);
        if (mAverageTimeSummary != null) mAverageTimeSummary.setOnClickListener(statsListener);
        
        View goalView = view.findViewById(R.id.goalSummary);
        if (goalView != null) goalView.setOnClickListener(v -> onClickGoal());
        
        if (mEmptyView != null) mEmptyView.setOnClickListener(v -> onClickEmptyState());
    }

    public void setListener(StatisticsWeekListener listener) {
        this.mListener = listener;
    }

    public void onClickStats() {
        if (mListener != null) {
            mListener.onClickStats();
        }
    }

    public void onClickGoal() {
        if (mListener != null) {
            mListener.onClickGoal();
        }
    }

    public void onClickEmptyState() {
        if (mListener != null) {
            mListener.onClickEmptyState();
        }
    }

    @Override
    public void toggleVisible(boolean show) {
        mWeekDaysView.setVisibility(show ? View.VISIBLE : View.GONE);
        mParentView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void toggleLoading(boolean show, BaseActivity parent) {
        parent.toggleShimmerLayout(mShimmerContainer, show, true);
        mContentView.setVisibility(View.GONE);
    }

    @Override
    public String getTitle() {
        return getString(R.string.home_week);
    }

    public void showWeekData(@Nullable UserStatsController.StatsResult stats) {
        UserStatsController.StatsResult safeStats = stats == null ? new UserStatsController.StatsResult() : stats;
        List<WeekDay> days = buildWeekdays(safeStats);
        renderWeekView(days);
        renderWeekGoal(safeStats);
        renderSummaryViews(safeStats);
        checkEmptyState(stats);
    }

    private void renderSummaryViews(UserStatsController.StatsResult stats) {
        mTotalWeightSummary.setIconPadding(ViewUtils.dpToPx(2));
        mWorkoutsCompletedSummary.setSummary(R.drawable.ic_check_circle, StatsFormatUtils.Companion.formatNumberStatsLabel(stats.getWorkoutsCompleted()), null, getString(R.string.home_week_workouts_completed));
        mTotalWeightSummary.setSummary(R.drawable.ic_weight, StatsFormatUtils.Companion.formatWeightStatsLabel(stats.getTotalWeightLifted()), SettingsManager.weightUnitAbbreviation(), getString(R.string.home_week_weight_lifted));
        mTotalTimeSummary.setSummary(R.drawable.ic_time, StatsFormatUtils.Companion.formatTimeStatsLabel(stats.getTotalWorkoutTimeMillis()), getString(R.string.hour), getString(R.string.home_week_total_time));
        mAverageTimeSummary.setSummary(R.drawable.ic_timelapse, StatsFormatUtils.Companion.formatTimeStatsLabel(stats.getAverageSessionTimeMillis()), getString(R.string.hour), getString(R.string.home_week_average_time));
    }

    private void renderWeekView(List<WeekDay> days) {
        WeekDayView[] weekDays = new WeekDayView[]{mDay1View, mDay2View, mDay3View, mDay4View, mDay5View, mDay6View, mDay7View};
        for (int i = 0; i < days.size(); i++) {
            WeekDayView dayView = weekDays[i];
            WeekDay day = days.get(i);
            dayView.setWeekDay(day);
        }
    }

    private void renderWeekGoal(UserStatsController.StatsResult stats) {
        int left = SettingsManager.manager.weeklyWorkoutsGoal() - stats.getWorkoutsCompleted();
        int progress = (stats.getWorkoutsCompleted() * 100) / SettingsManager.manager.weeklyWorkoutsGoal();
        mGoalProgress.setProgress(progress, false);
        mGoalPendingLayout.setVisibility(left > 0 ? View.VISIBLE : View.GONE);
        mGoalAchievedLayout.setVisibility(left <= 0 ? View.VISIBLE : View.GONE);
        mGoalTotalLbl.setText(left + "");
    }

    private void checkEmptyState(@Nullable UserStatsController.StatsResult stats) {
        if (stats != null) {
            boolean show = stats.getOverallWorkoutsCompleted() <= 0;
            mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            mContentView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private List<WeekDay> buildWeekdays(UserStatsController.StatsResult stats) {
        List<WeekDay> days = DayOfWeekExtKt.buildWeekDays(SettingsManager.manager.firstDayOfWeek());
        for (WeekDay weekDay : days) {
            weekDay.setActive(stats.getDayWorkoutMap().containsKey(weekDay.getDate().getDayOfMonth()));
        }
        return days;
    }

    // Utils

    private String getString(int stringResId) {
        return mContext.getString(stringResId);
    }

    // Setup

    private void setupEmptyView() {
        String name = LocalUserManager.getUser().getFirstName();
        mEmptyView.setTitle(mContext.getString(R.string.home_week_empty_title, TextUtils.isEmpty(name) ? "" : String.format(", %s", name)));
    }

    public interface StatisticsWeekListener {

        void onClickStats();

        void onClickGoal();

        void onClickEmptyState();
    }
}

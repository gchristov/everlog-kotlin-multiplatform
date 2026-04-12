package com.everlog.ui.fragments.home.week.views;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ahamed.multiviewadapter.DataListManager;
import com.ahamed.multiviewadapter.RecyclerAdapter;
import com.everlog.R;
import com.everlog.data.model.plan.ELPlan;
import com.everlog.data.model.plan.ELPlanDay;
import com.everlog.data.model.plan.ELPlanState;
import com.everlog.ui.activities.base.BaseActivity;
import com.everlog.ui.adapters.plan.PlanDayAdapter;
import com.everlog.ui.dialog.DialogBuilder;
import com.everlog.ui.navigator.ELNavigator;
import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PlanWeekView implements IWeekView {

    View mParentView;
    View mContentView;
    TextView mFinishPlanBtn;
    ShimmerFrameLayout mShimmerContainer;

    // Progress

    View mPlanProgressView;
    TextView mPlanNameLbl;
    TextView mPlanProgressLbl;
    ProgressBar mPlanProgressBar;

    // Days

    TextView mPlanWeekLbl;
    RecyclerView mPlanWeekList;

    private Context mContext;
    private final RecyclerAdapter mAdapter = new RecyclerAdapter();
    private final DataListManager<ELPlanDay> mDataListManager = new DataListManager<>(mAdapter);
    private PlanWeekListener mListener;

    @Override
    public void onCreateView(View view) {
        this.mContext = view.getContext();
        
        mParentView = view.findViewById(R.id.planParentView);
        mContentView = view.findViewById(R.id.planContentView);
        mFinishPlanBtn = view.findViewById(R.id.finishPlanBtn);
        mShimmerContainer = view.findViewById(R.id.planShimmerView);
        mPlanProgressView = view.findViewById(R.id.planProgressView);
        mPlanNameLbl = view.findViewById(R.id.planNameLbl);
        mPlanProgressLbl = view.findViewById(R.id.planProgressLbl);
        mPlanProgressBar = view.findViewById(R.id.planProgressBar);
        mPlanWeekLbl = view.findViewById(R.id.planWeekLbl);
        mPlanWeekList = view.findViewById(R.id.planWeekList);

        if (mPlanProgressView != null) {
            mPlanProgressView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onClickPlan();
                }
            });
        }
        setupList();
    }

    public void setListener(PlanWeekListener listener) {
        this.mListener = listener;
    }

    @Override
    public void toggleVisible(boolean show) {
        mPlanProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mParentView.setVisibility(show ? View.VISIBLE : View.GONE);
        mFinishPlanBtn.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onClickStart();
            }
        });
    }

    @Override
    public void toggleLoading(boolean show, BaseActivity parent) {
        parent.toggleShimmerLayout(mShimmerContainer, show, true);
        mContentView.setVisibility(View.GONE);
    }

    @Override
    public String getTitle() {
        return getString(R.string.home_week_plan);
    }

    public void showWeekData(@Nullable ELPlan plan, @Nullable ELPlanState state) {
        checkEmptyState(plan, state);
        renderFinish(plan, state);
        renderPlan(plan, state);
        renderPlanDays(plan, state);
    }

    private void renderFinish(@Nullable ELPlan plan, @Nullable ELPlanState state) {
        ELPlanDay day = null;
        if (plan != null && state != null) {
            day = plan.getNextDay();
        }
        mFinishPlanBtn.setVisibility(day == null ? View.VISIBLE : View.GONE);
    }

    private void renderPlan(@Nullable ELPlan plan, @Nullable ELPlanState state) {
        if (plan == null || state == null) {
            mPlanNameLbl.setText("--");
            mPlanProgressLbl.setText("--");
        } else {
            int progress = plan.getProgress();
            mPlanNameLbl.setText(plan.getName());
            mPlanProgressLbl.setText(String.format("%d %%", progress));
            mPlanProgressBar.setProgress(progress);
        }
    }

    private void renderPlanDays(@Nullable ELPlan plan, @Nullable ELPlanState state) {
        mDataListManager.clear();
        if (plan == null || state == null) {
            mPlanWeekLbl.setVisibility(View.INVISIBLE);
            mPlanWeekList.setVisibility(View.INVISIBLE);
        } else {
            int currentDay = plan.getDayIndex();
            int week = currentDay / 7;
            if (week >= plan.getWeeks().size()) {
                // If we've passed the last week, use the last plan week
                week = plan.getWeeks().size() - 1;
            }
            ELPlanDay day = plan.getWeeks().get(week).getDays().get(currentDay % 7);
            day.setNext(true);
            mPlanWeekLbl.setText(String.format("Week %d of %d", week + 1, plan.getWeeks().size()));
            mPlanWeekLbl.setVisibility(View.VISIBLE);
            mPlanWeekList.setVisibility(View.VISIBLE);
            mDataListManager.addAll(plan.getWeeks().get(week).getDays());
        }
        mAdapter.notifyDataSetChanged();
    }

    private void checkEmptyState(@Nullable ELPlan plan, @Nullable ELPlanState state) {
        if (plan != null && state != null) {
            mContentView.setVisibility(View.VISIBLE);
        }
    }

    // Utils

    private String getString(int stringResId) {
        return mContext.getString(stringResId);
    }

    // Setup

    private void setupList() {
        mAdapter.addDataManager(mDataListManager);
        mAdapter.registerBinder(new PlanDayAdapter.Binder(new PlanDayAdapter.OnPlanDayListener() {
            @Override
            public void onClickSkip(ELPlanDay day) {
                if (mListener != null) {
                    mListener.onClickSkip();
                }
            }

            @Override
            public void onClickStart(ELPlanDay day) {
                if (mListener != null) {
                    mListener.onClickStart();
                }
            }

            @Override
            public void onItemClicked(ELPlanDay item, int position) {
                if (item.getRoutine() != null) {
                    new ELNavigator(mContext).openRoutineDetails(item.getRoutine(), true);
                } else {
                    DialogBuilder.showOKPrompt(mContext,
                            mContext.getString(R.string.plan_details_rest_title),
                            getString(R.string.plan_details_rest_prompt));
                }
            }

            @Override
            public void onClickChooseRoutine(ELPlanDay day, int position) {
            }

            @Override
            public void onDayEdited() {
            }
        }).setOngoing(true));
        mPlanWeekList.setLayoutManager(new LinearLayoutManager(mContext));
        mPlanWeekList.setAdapter(mAdapter);
    }

    public interface PlanWeekListener {
        void onClickSkip();
        void onClickStart();
        void onClickPlan();
    }
}

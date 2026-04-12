package com.everlog.ui.activities.home.workout.details;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.everlog.R;
import com.everlog.data.model.workout.ELWorkout;
import com.everlog.databinding.ActivityWorkoutDetailsBinding;
import com.everlog.ui.activities.base.BaseActivity;
import com.everlog.ui.activities.base.BaseActivityPresenter;
import com.everlog.ui.dialog.DialogBuilder;
import com.everlog.utils.ActivityUtils;
import com.google.android.material.appbar.AppBarLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Random;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;
import rx.Observable;
import rx.subjects.PublishSubject;

import static com.everlog.constants.ELConstants.EXTRA_WORKOUT;
import static com.everlog.constants.ELConstants.EXTRA_WORKOUT_FROM_PLAN;
import static com.everlog.constants.ELConstants.EXTRA_WORKOUT_JUST_FINISHED;
import static com.everlog.managers.analytics.AnalyticsConstants.SCREEN_WORKOUT_DETAILS;

public class WorkoutDetailsActivity extends BaseActivity implements MvpViewWorkoutDetails {

    private ActivityWorkoutDetailsBinding binding;

    private PresenterWorkoutDetails mPresenter;

    private RecyclerView.LayoutManager mLayoutManager;

    private PublishSubject<Void> mOnClickSaveAsRoutine = PublishSubject.create();
    private PublishSubject<Void> mOnClickDelete = PublishSubject.create();
    private PublishSubject<Void> mOnClickShare = PublishSubject.create();

    @Override
    public void onActivityCreated() {
        setupTopBar();
        setupListView();
    }

    @NotNull
    @Override
    public String getAnalyticsScreenName() {
        return SCREEN_WORKOUT_DETAILS;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_workout_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save_as_routine) {
            mOnClickSaveAsRoutine.onNext(null);
            return true;
        } else if (id == R.id.action_delete) {
            mOnClickDelete.onNext(null);
            return true;
        } else if (id == R.id.action_share) {
            mOnClickShare.onNext(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getLayoutResId() {
        return 0;
    }

    @Override
    public View getBindingView() {
        binding = ActivityWorkoutDetailsBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Nullable
    @Override
    public BaseActivityPresenter getPresenter() {
        return mPresenter;
    }

    @Override
    public Observable<Void> onClickSaveAsRoutine() {
        return mOnClickSaveAsRoutine;
    }

    @Override
    public Observable<Void> onClickDelete() {
        return mOnClickDelete;
    }

    @Override
    public Observable<Void> onClickShare() {
        return mOnClickShare;
    }

    @Override
    public void loadItemDetails(ELWorkout workout) {
        invalidateOptionsMenu();
    }

    @Override
    public ELWorkout getItemToEdit() {
        return (ELWorkout) getIntent().getSerializableExtra(EXTRA_WORKOUT);
    }

    @Override
    public boolean isJustFinished() {
        return getIntent().getBooleanExtra(EXTRA_WORKOUT_JUST_FINISHED, false);
    }

    @Override
    public boolean performingFromPlan() {
        return getIntent().getBooleanExtra(EXTRA_WORKOUT_FROM_PLAN, false);
    }

    @Override
    public void toggleEmptyViewVisible(boolean visible) {
        if (binding.emptyView != null) {
            binding.emptyView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void showWeeklyGoalCompleteCongrats() {
        binding.achievementView
                .setTitle("Congratulations!")
                .setMensage("You achieved your weekly goal.")
                .setTextColor(ContextCompat.getColor(this, R.color.background_card))
                .setColor(R.color.main_accent_darker)
                .setIcon(R.drawable.ic_trophy)
                .show();
        binding.achievementKonfettiView.build()
                .addColors(ContextCompat.getColor(this, R.color.konfetti_lt_orange),
                        ContextCompat.getColor(this, R.color.konfetti_lt_pink),
                        ContextCompat.getColor(this, R.color.konfetti_lt_purple),
                        ContextCompat.getColor(this, R.color.konfetti_lt_yellow),
                        ContextCompat.getColor(this, R.color.main_accent))
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(new Size(12, 4f), new Size(16, 6f))
                .setPosition(-50f, binding.achievementKonfettiView.getWidth() + 50f, -50f, -50f)
                .streamFor(100, 3000L);
    }

    @Override
    public void showWorkoutCompleteCongrats() {
        String[] msgs = {"Nice work!", "Strong finish!", "Good work!", "That was great!"};
        binding.achievementView
                .setTitle(msgs[new Random().nextInt(msgs.length)])
                .setMensage("You're a step closer to your goal.")
                .setTextColor(ContextCompat.getColor(this, R.color.background_card))
                .setColor(R.color.main_accent_darker)
                .setIcon(R.drawable.ic_trophy)
                .show();
    }

    @Override
    public Observable<String> showStringPrompt(String name, DialogBuilder.StringDialogType type) {
        return DialogBuilder.showInputStringDialog(this, name, type);
    }

    @Override
    public Observable<Date> showDatePrompt(Date date) {
        return DialogBuilder.showDateTimeDialog(this, date);
    }

    @Override
    public void showSaveChangesTick() {
        ((Toolbar) binding.getRoot().findViewById(R.id.toolbar)).setNavigationIcon(R.drawable.ic_check_green);
        ((AppBarLayout) binding.getRoot().findViewById(R.id.appBar)).setExpanded(true);
    }

    // Setup

    @Override
    public void setupPresenter() {
        mPresenter = new PresenterWorkoutDetails();
    }

    private void setupTopBar() {
        ((Toolbar) binding.getRoot().findViewById(R.id.toolbar)).setTitle(R.string.workout_details_title);
        setSupportActionBar(binding.getRoot().findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActivityUtils.setupWorkoutCoverImage(binding.getRoot().findViewById(R.id.coverImage));
    }

    private void setupListView() {
        mLayoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(mPresenter.getListAdapter());
    }
}
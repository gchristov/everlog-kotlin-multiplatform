package com.everlog.ui.activities.home.exercise.create;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;

import com.everlog.R;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.databinding.ActivityExerciseCreateBinding;
import com.everlog.ui.activities.base.BaseActivity;
import com.everlog.ui.activities.base.BaseActivityPresenter;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxAdapterView;
import com.jakewharton.rxbinding.widget.RxTextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import rx.Observable;

import static com.everlog.constants.ELConstants.EXTRA_EXERCISE;
import static com.everlog.managers.analytics.AnalyticsConstants.SCREEN_EXERCISE_CREATE;

public class CreateExerciseActivity extends BaseActivity implements MvpViewCreateExercise {

    private ActivityExerciseCreateBinding binding;
    private PresenterCreateExercise mPresenter;

    @Override
    public void onActivityCreated() {
        setupTopBar();
        setupSpinner();
    }

    @NotNull
    @Override
    public String getAnalyticsScreenName() {
        return SCREEN_EXERCISE_CREATE;
    }

    @Override
    public int getLayoutResId() {
        return 0;
    }

    @Override
    public View getBindingView() {
        binding = ActivityExerciseCreateBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Nullable
    @Override
    public BaseActivityPresenter getPresenter() {
        return mPresenter;
    }

    @Override
    public ELExercise getItemToEdit() {
        return (ELExercise) getIntent().getSerializableExtra(EXTRA_EXERCISE);
    }

    @Override
    public String getExerciseName() {
        return binding.exerciseName.getText().toString().trim();
    }

    @Override
    public Observable<Void> onClickSave() {
        return RxView.clicks(binding.includeToolbar.saveBtn);
    }

    @Override
    public Observable<Integer> onCategoryChanged() {
        return RxAdapterView.itemSelections(binding.categorySpinner);
    }

    @Override
    public Observable<CharSequence> onNameChanged() {
        return RxTextView.textChanges(binding.exerciseName);
    }

    @Override
    public Observable<Void> onClickImage() {
        return RxView.clicks(binding.exerciseImg);
    }

    @Override
    public void loadExerciseDetails(ELExercise exercise, int selectedCategoryIndex) {
        getSupportActionBar().setTitle(TextUtils.isEmpty(exercise.getName()) ? R.string.create_exercise_title_new : R.string.create_exercise_title_edit);
        binding.exerciseName.setText(exercise.getName());
        binding.exerciseImg.applyMask(R.drawable.mask_square);
        binding.categorySpinner.setSelection(selectedCategoryIndex + 1); // Account for extra None type.
        setExerciseImage(exercise);
    }

    @Override
    public void setExerciseImage(ELExercise exercise) {
        binding.exerciseImg.setExercise(exercise);
        binding.photoPrompt.setVisibility(exercise.getImageUrl() != null ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getSelectedCategoryIndex() {
        return binding.categorySpinner.getSelectedItemPosition() - 1; // Account for extra None type.
    }

    // Setup

    @Override
    public void setupPresenter() {
        mPresenter = new PresenterCreateExercise();
    }

    private void setupTopBar() {
        binding.includeToolbar.toolbar.setNavigationIcon(R.drawable.ic_clear_white);
        setSupportActionBar(binding.includeToolbar.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        View appBar = (View) binding.includeToolbar.toolbar.getParent();
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.exercise_category_titles_with_none, R.layout.view_spinner_row);
        adapter.setDropDownViewResource(R.layout.view_spinner_row_dropdown);
        ViewCompat.setBackgroundTintList(binding.categorySpinner, ColorStateList.valueOf(ContextCompat
                .getColor(this, R.color.gray_3)));
        binding.categorySpinner.setAdapter(adapter);
    }
}

package com.everlog.ui.adapters.routine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.util.Header;
import com.everlog.databinding.RowHeaderRoutinePerformBinding;
import com.everlog.utils.StringExtKt;

public class PerformRoutineHeaderAdapter {

    public static class Binder extends ItemBinder<Header, ViewHolder> {

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_header_routine_perform, parent, false));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof Header;
        }

        @Override
        public void bind(ViewHolder holder, Header item) {
            holder.setItem(item);
        }
    }

    static class ViewHolder extends ItemViewHolder<Header> {

        private final RowHeaderRoutinePerformBinding binding;
        private Header item;

        ViewHolder(View itemView) {
            super(itemView);
            binding = RowHeaderRoutinePerformBinding.bind(itemView);
        }

        public void setItem(Header item) {
            this.item = item;
            renderTips();
        }

        // Render

        private void renderTips() {
            String text = itemView.getContext().getString(R.string.perform_routine_protip_1);
            binding.tip1Field.setText(StringExtKt.fromHtml(text));
            text = itemView.getContext().getString(R.string.perform_routine_protip_2);
            binding.tip2Field.setText(StringExtKt.fromHtml(text));
        }
    }
}

package com.everlog.ui.adapters;

import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.data.model.util.Footer;
import com.everlog.databinding.RowFooterLinkBinding;

public class FooterAdapter {

    public static class Binder extends ItemBinder<Footer, ViewHolder> {

        private final OnListItemListener<Footer> actionListener;

        public Binder(OnListItemListener<Footer> actionListener) {
            this.actionListener = actionListener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_footer_link, parent, false));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof Footer;
        }

        @Override
        public void bind(ViewHolder holder, Footer item) {
            holder.render();
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onItemClicked(item, holder.getAbsoluteAdapterPosition());
                }
            });
        }
    }

    static class ViewHolder extends ItemViewHolder<Footer> {

        private final RowFooterLinkBinding binding;

        ViewHolder(View itemView) {
            super(itemView);
            binding = RowFooterLinkBinding.bind(itemView);
        }

        // Render

        protected void render() {
            binding.footerText.setText(getItem().getTextResId());

            SpannableString content = new SpannableString(binding.footerLink.getContext().getString(getItem().getLinkResId()));
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            binding.footerLink.setText(content);
        }
    }
}

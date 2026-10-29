package com.everlog.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ahamed.multiviewadapter.ItemBinder;
import com.ahamed.multiviewadapter.ItemViewHolder;
import com.everlog.R;
import com.everlog.databinding.RowCoverImageBinding;
import com.everlog.managers.api.coverimages.response.CoverImage;
import com.everlog.utils.glide.ELGlideModule;

public class CoverImageAdapter {

    public static class Binder extends ItemBinder<CoverImage, ViewHolder> {

        private OnListItemListener<CoverImage> mListener;

        public Binder(OnListItemListener<CoverImage> actionListener) {
            this.mListener = actionListener;
        }

        @Override
        public ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.row_cover_image, parent, false));
        }

        @Override
        public boolean canBindData(Object item) {
            return item instanceof CoverImage;
        }

        @Override
        public void bind(ViewHolder holder, CoverImage item) {
            holder.setItem(item);
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onItemClicked(item, holder.getAbsoluteAdapterPosition());
                }
            });
        }
    }

    static class ViewHolder extends ItemViewHolder<CoverImage> {

        private final RowCoverImageBinding binding;
        private CoverImage item;

        ViewHolder(View itemView) {
            super(itemView);
            binding = RowCoverImageBinding.bind(itemView);
        }

        public void setItem(CoverImage item) {
            this.item = item;
            renderImage();
        }

        // Render

        private void renderImage() {
            binding.imageView.setBackgroundColor(item.getBackgroundColor());
            binding.imageView.ratio(item.getAspectRatio());
            ELGlideModule.loadImage(item.getUrl(), 0.1f, binding.imageView);
        }
    }
}

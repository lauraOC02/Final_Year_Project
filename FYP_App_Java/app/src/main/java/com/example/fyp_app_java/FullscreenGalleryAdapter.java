package com.example.fyp_app_java;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import com.squareup.picasso.Picasso;

import java.util.List;

public class FullscreenGalleryAdapter extends PagerAdapter {

    private Context context;
    private List<String> imageUrls;

    public FullscreenGalleryAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_fullscreen_image, container, false);

        ImageView imageView = view.findViewById(R.id.imageViewFullscreen);
        String imageUrl = imageUrls.get(position);

        Picasso.get()
                .load(imageUrl)
                .resize(1200, 800)
                .centerInside()
                .rotate(90)
                .onlyScaleDown() // Only scale down the image, don't scale up
                .into(imageView);;

        container.addView(view);

        return view;
    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}

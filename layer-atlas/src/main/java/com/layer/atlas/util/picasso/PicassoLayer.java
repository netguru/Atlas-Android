package com.layer.atlas.util.picasso;

import android.content.Context;

import com.layer.atlas.util.picasso.requesthandlers.MessagePartRequestHandler;
import com.layer.sdk.LayerClient;
import com.squareup.picasso.Picasso;

public class PicassoLayer {
    private volatile static PicassoLayer instance;
    private Picasso picasso;

    public static PicassoLayer getInstance() {
        if (instance == null) {
            synchronized (PicassoLayer.class) {
                if (instance == null) {
                    instance = new PicassoLayer();
                }
            }
        }
        return instance;
    }

    public Picasso with(Context context, LayerClient layerClient) {
        if(picasso == null) {
            picasso = new Picasso.Builder(context)
                    .addRequestHandler(new MessagePartRequestHandler(layerClient))
                    .build();
        }

        return picasso;
    }

    private PicassoLayer() { }
}

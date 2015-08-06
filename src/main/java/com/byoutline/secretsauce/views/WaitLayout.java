package com.byoutline.secretsauce.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import com.byoutline.cachedfield.CachedFieldWithArg;
import com.byoutline.cachedfield.FieldState;
import com.byoutline.cachedfield.FieldStateListener;
import com.byoutline.cachedfield.cachedendpoint.CachedEndpointWithArg;
import com.byoutline.cachedfield.cachedendpoint.EndpointState;
import com.byoutline.cachedfield.cachedendpoint.EndpointStateListener;
import com.byoutline.cachedfield.cachedendpoint.StateAndValue;
import com.byoutline.secretsauce.R;
import com.byoutline.secretsauce.utils.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Apache License 2.0
 *
 * @author Sebastian Kacprzak <nait at naitbit.com>
 */
public class WaitLayout extends FrameLayout implements FieldStateListener, EndpointStateListener {
    private static final float BASE_DENSITY = 160f;
    private static final int BASE_PROGRESSBAR_SIZE = 76;
    private final List<CachedFieldWithArg> fields = new ArrayList<>(2);
    private final List<CachedEndpointWithArg> endpoints = new ArrayList<>(2);

    View loadingBar;

    public WaitLayout(Context context) {
        super(context);
        init(context);
    }

    public WaitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WaitLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
    }

    public void showLoadingBar() {
        setLoadingBarVisibleAsync(true);
    }

    public void hideLoadingBar() {
        setLoadingBarVisibleAsync(false);
    }

    public synchronized void setLoadingBarVisibleAsync(final boolean visible) {
        // Although usually we are in UI thread, post ensures that view is fully constructed.
        this.post(new Runnable() {
            @Override
            public void run() {
                // ensure that we added loading bar
                if (loadingBar == null) {
                    addLoadingBarToView();
                } else {
                    loadingBar.setLayoutParams(getProgressBarLayoutParams());
                }
                ViewUtils.showView(loadingBar, visible);
                for (int i = 0; i < getChildCount(); i++) {
                    getChildAt(i).setEnabled(!visible);
                }
            }
        });
    }

    private void addLoadingBarToView() {
        loadingBar = new ProgressBar(new ContextThemeWrapper(getContext(), R.style.WaitLayoutProgressBar), null, R.style.WaitLayoutProgressBar);
        LayoutParams lp = getProgressBarLayoutParams();
        addView(loadingBar, lp);
    }

    private LayoutParams getProgressBarLayoutParams() {
        int max = Math.round(ViewUtils.convertDpToPixel(BASE_PROGRESSBAR_SIZE, getContext()));
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int smallerDimension = width < height ? width : height;
        if (smallerDimension > max) {
            int padding = (smallerDimension - max) / 2;
            loadingBar.setPadding(padding, padding, padding, padding);
        }
        LayoutParams lp = new LayoutParams(width, height);
        lp.gravity = Gravity.CENTER;
        return lp;
    }

    public void stopTrackingProgress() {
        for (CachedFieldWithArg field : fields) {
            field.removeStateListener(this);
        }
        for (CachedEndpointWithArg endpoint : endpoints) {
            endpoint.removeEndpointListener(this);
        }
        fields.clear();
        endpoints.clear();
    }

    public void showProgressOf(CachedFieldWithArg... newFields) {
        showProgressOf(Arrays.asList(newFields), Collections.<CachedEndpointWithArg>emptyList());
    }

    public void showProgressOf(CachedEndpointWithArg... newEndpoints) {
        showProgressOf(Collections.<CachedFieldWithArg>emptyList(), Arrays.asList(newEndpoints));
    }

    public synchronized void showProgressOf(List<CachedFieldWithArg> newFields, List<CachedEndpointWithArg> newEndpoints) {
        stopTrackingProgress();
        for (CachedFieldWithArg field : newFields) {
            field.addStateListener(this);
        }
        for (CachedEndpointWithArg endpoint : newEndpoints) {
            endpoint.addEndpointListener(this);
        }
        fields.addAll(newFields);
        endpoints.addAll(newEndpoints);
    }

    private void checkState() {
        boolean loading = false;
        List<CachedFieldWithArg> currentFields = new ArrayList<>(fields);
        List<CachedEndpointWithArg> currentEndpoints = new ArrayList<>(endpoints);
        for (CachedFieldWithArg field : currentFields) {
            if (field.getState() == FieldState.CURRENTLY_LOADING) {
                loading = true;
                break;
            }
        }
        if (!loading) {
            for (CachedEndpointWithArg endpoint : currentEndpoints) {
                if (endpoint.getStateAndValue().getState() == EndpointState.DURING_CALL) {
                    loading = true;
                    break;
                }
            }
        }
        setLoadingBarVisibleAsync(loading);
    }

    @Override
    public void fieldStateChanged(FieldState fieldState) {
        checkState();
    }

    @Override
    public void endpointStateChanged(StateAndValue endpointState) {
        checkState();
    }
}
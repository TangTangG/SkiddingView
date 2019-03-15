package com.gutang.skidding;

/**
 * Created by TCG on 2017/8/25.
 */

public class SkiddingItemObserver {

    private SkiddingView openedView = null;

    public void report(SkiddingView view, boolean open) {
        if (open && (view != null && !view.equals(openedView))) {
            if (openedView != null) {
                openedView.close();
            }
            openedView = view;
        } else if (view != null && view.equals(openedView)) {
            openedView = null;
        }
    }
}

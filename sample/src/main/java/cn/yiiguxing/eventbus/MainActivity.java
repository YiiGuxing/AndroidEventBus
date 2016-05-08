package cn.yiiguxing.eventbus;

import android.os.Bundle;

import cn.yiiguxing.event.Event;
import cn.yiiguxing.event.EventBus;
import cn.yiiguxing.event.ThreadMode;
import cn.yiiguxing.event.annotation.Subscribe;
import cn.yiiguxing.event.annotation.Tag;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        postEvents();
        new Thread(new Runnable() {
            @Override
            public void run() {
                postEvents();
            }
        }).start();
    }

    void postEvents() {
        EventBus bus = EventBus.getDefault();

        bus.post(1001, false, "STRING", new int[]{1, 2, 3, 4, 5});
        bus.post("tag", 1001, true, "STRING", new int[]{1, 2, 3, 4, 5});

        bus.post(1000);
        bus.post("tag", 0);
        bus.post("tag1", 1);
        bus.post("tag2", 2);
    }

    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    @Subscribe(tag = {EventBus.DEFAULT_TAG, "tag"}, mode = ThreadMode.ASYNC)
    void handleEventBase(Event event) {
        LogUtil.w("Override: handleEventBase - %s, thread:%s", event,
                Thread.currentThread().getName());
    }

    @Subscribe(tag = {EventBus.DEFAULT_TAG, "tag", "tag1", "tag2"})
    void testTag(@Tag String tag, int i) {
        LogUtil.w("testTag - tag='%s', i=%d", tag, i);
    }

}

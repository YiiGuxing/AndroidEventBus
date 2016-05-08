/*
 * Copyright (C) 2016 Yii.Guxing <yii.guxing@gmail.com>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.yiiguxing.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import cn.yiiguxing.event.handler.AsyncEventHandler;
import cn.yiiguxing.event.handler.EventHandler;
import cn.yiiguxing.event.handler.SimpleEventHandler;
import cn.yiiguxing.event.handler.UIThreadEventHandler;
import cn.yiiguxing.event.internal.MethodSubscriber;
import cn.yiiguxing.event.internal.SubscribeRegister;

/**
 * EventBus
 * <p/>
 * Created by Yii.Guxing on 16/1/27.
 */
public class EventBus {

    private static final String TAG = "EventBus";

    private static final String ANDROID_PREFIX = "android.";
    private static final String JAVA_PREFIX = "java.";
    private static final String SUFFIX = "$$SubRegister";

    /**
     * Default event tag.
     */
    public static final String DEFAULT_TAG = "";

    /**
     * default descriptor
     */
    static final String DESCRIPTOR = "EventBus";
    static final int DEFAULT_MAX_DATA_LENGTH = 5;

    private static final Map<Class<?>, SubscribeRegister<Object>> REGISTER =
            new LinkedHashMap<>();

    private static volatile EventBus sDefaultBus;

    /**
     * Tag-Subscriptions map
     */
    private final Map<String, CopyOnWriteArrayList<Subscription>> mSubscriptionsMap =
            new ConcurrentHashMap<>();
    /**
     * Event poster for the current thread.
     */
    private final ThreadLocal<EventDispatcher> mEventDispatcher = new ThreadLocal<EventDispatcher>() {
        protected EventDispatcher initialValue() {
            return new EventDispatcher();
        }
    };

    private final String mDescriptor;
    private final boolean mDebug;
    private final int mMaxDataLength;

    private final EventHandler mDefaultHandler;
    private final EventHandler mPostThreadHandler;
    private final EventHandler mMainThreadHandler;
    private final EventHandler mAsyncThreadHandler;


    private EventBus(Builder builder) {
        mDescriptor = builder.descriptor;
        mDebug = builder.debug;
        mMaxDataLength = builder.maxDataLength;
        mMainThreadHandler = new UIThreadEventHandler();
        mPostThreadHandler = new SimpleEventHandler();
        mDefaultHandler = builder.defaultHandler != null
                ? builder.defaultHandler
                : mPostThreadHandler;
        mAsyncThreadHandler = builder.executor != null
                ? new AsyncEventHandler(builder.executor)
                : new AsyncEventHandler();
    }

    /**
     * Returns the default event bus.
     */
    public static EventBus getDefault() {
        if (sDefaultBus == null) {
            synchronized (EventBus.class) {
                sDefaultBus = new Builder(DESCRIPTOR).create();
            }
        }

        return sDefaultBus;
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call
     * {@link #unregister(Object)} once they are no longer interested in receiving events.
     *
     * @see #unregister(Object)
     */
    public void register(@NonNull Object subscriber) {
        if (subscriber instanceof Subscriber) {
            register((Subscriber) subscriber);
            return;
        }

        registerInner(subscriber);
    }

    private void registerInner(Object subscriber) {
        Class<?> targetClass = subscriber.getClass();
        try {
            if (mDebug) Log.d(TAG, "Looking up register for " + targetClass.getName());
            SubscribeRegister<Object> viewBinder = findSubscribeRegisterForClass(targetClass);
            if (viewBinder != null) {
                if (mDebug) Log.d(TAG, String.format("Register:%s.", subscriber));

                viewBinder.register(this, subscriber);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to register subscriber for " +
                    targetClass.getName(), e);
        }
    }

    private SubscribeRegister<Object> findSubscribeRegisterForClass(Class<?> cls)
            throws IllegalAccessException, InstantiationException {
        SubscribeRegister<Object> register = REGISTER.get(cls);
        if (register != null) {
            if (mDebug) Log.d(TAG, "Cached in register map.");
            return register;
        }
        String clsName = cls.getName();
        if (clsName.startsWith(ANDROID_PREFIX) || clsName.startsWith(JAVA_PREFIX)) {
            if (mDebug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return null;
        }
        try {
            Class<?> registerClass = Class.forName(clsName + SUFFIX);
            //noinspection unchecked
            register = (SubscribeRegister<Object>) registerClass.newInstance();
            if (mDebug) Log.d(TAG, "Loaded register class.");
        } catch (ClassNotFoundException e) {
            if (mDebug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            register = findSubscribeRegisterForClass(cls.getSuperclass());
        }

        REGISTER.put(cls, register);

        return register;
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call
     * {@link #unregister(Subscriber)} once they are no longer interested in receiving events.
     *
     * @see #unregister(Subscriber)
     */
    public void register(@NonNull Subscriber subscriber) {
        register(subscriber, DEFAULT_TAG);
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call
     * {@link #unregister(Subscriber)} once they are no longer interested in receiving events.
     *
     * @see #unregister(Subscriber)
     */
    public void register(@NonNull Subscriber subscriber, @Nullable String tag) {
        register(subscriber, tag, ThreadMode.DEFAULT);
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call
     * {@link #unregister(Subscriber)} once they are no longer interested in receiving events.
     *
     * @see #unregister(Subscriber)
     */
    public void register(@NonNull Subscriber subscriber, @Nullable String tag, ThreadMode mode) {
        EventHandler handler;
        switch (mode) {
            case POST:
                handler = mPostThreadHandler;
                break;
            case ASYNC:
                handler = mAsyncThreadHandler;
                break;
            case MAIN:
                handler = mMainThreadHandler;
                break;
            case DEFAULT:
            default:
                handler = mDefaultHandler;
                break;
        }

        register(subscriber, tag, handler);
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call
     * {@link #unregister(Subscriber)} once they are no longer interested in receiving events.
     *
     * @see #unregister(Subscriber)
     */
    public void register(@NonNull Subscriber subscriber,
                         @Nullable String tag,
                         @NonNull EventHandler handler) {
        if (!(subscriber instanceof UnstableSubscriber)) {
            subscriber = new SubscriberWrapper(subscriber);
        }

        synchronized (mSubscriptionsMap) {
            CopyOnWriteArrayList<Subscription> subscriptionLists = mSubscriptionsMap.get(tag);
            if (subscriptionLists == null) {
                subscriptionLists = new CopyOnWriteArrayList<>();
                mSubscriptionsMap.put(tag, subscriptionLists);
            }

            Subscription newSubscription = new Subscription(subscriber, tag, handler);
            int index = subscriptionLists.indexOf(newSubscription);
            if (index >= 0) {
                if (!handler.equals(subscriptionLists.get(index).eventHandler)) {
                    subscriptionLists.remove(index);
                    subscriptionLists.add(index, newSubscription);

                    if (mDebug)
                        Log.d(TAG, String.format("Override:tag=%s, subscriber=%s, handler=%s.", tag,
                                subscriber, handler));
                }

                return;
            }

            if (mDebug) Log.d(TAG, String.format("Register:tag=%s, subscriber=%s, handler=%s.", tag,
                    subscriber, handler));

            subscriptionLists.add(newSubscription);
        }
    }

    /**
     * Unregisters the given subscriber.
     */
    public void unregister(@NonNull Subscriber subscriber) {
        synchronized (mSubscriptionsMap) {
            Iterator<Map.Entry<String, CopyOnWriteArrayList<Subscription>>> iterator =
                    mSubscriptionsMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CopyOnWriteArrayList<Subscription>> entry = iterator.next();
                CopyOnWriteArrayList<Subscription> subscriptions = entry.getValue();
                if (subscriptions != null) {
                    List<Subscription> foundSubscriptions = new LinkedList<>();
                    for (Subscription subscription : subscriptions) {
                        Subscriber sub = subscription.subscriber;
                        if (sub.equals(subscriber)) {
                            foundSubscriptions.add(subscription);

                            if (mDebug) Log.d(TAG, "Unregister:" + subscriber);
                        } else if ((sub instanceof UnstableSubscriber) &&
                                ((UnstableSubscriber) sub).getUnstable() == null) {
                            foundSubscriptions.add(subscription);

                            if (mDebug) Log.d(TAG, "Unregister cleared subscriber:" + subscriber);
                        }
                    }

                    subscriptions.removeAll(foundSubscriptions);
                }

                if (subscriptions == null || subscriptions.size() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    public void unregister(@Nullable String tag) {
        synchronized (mSubscriptionsMap) {
            mSubscriptionsMap.remove(tag);
        }
    }

    /**
     * Unregisters the given subscriber from all event classes.
     */
    public void unregister(Object object) {
        if (object == null)
            return;

        if (object instanceof Subscriber) {
            unregister((Subscriber) object);
            return;
        }

        if (object instanceof Class) {
            throw new IllegalArgumentException("Can not unregister " + object.getClass().getName());
        }

        boolean unregister = false;
        synchronized (mSubscriptionsMap) {
            Iterator<Map.Entry<String, CopyOnWriteArrayList<Subscription>>> iterator =
                    mSubscriptionsMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CopyOnWriteArrayList<Subscription>> entry = iterator.next();
                CopyOnWriteArrayList<Subscription> subscriptions = entry.getValue();
                if (subscriptions != null) {
                    List<Subscription> foundSubscriptions = new LinkedList<>();
                    for (Subscription subscription : subscriptions) {
                        Subscriber subscriber = subscription.subscriber;
                        if (subscriber instanceof UnstableSubscriber) {
                            Object unstable = ((UnstableSubscriber) subscriber).getUnstable();
                            if (unstable == null || unstable.equals(object)) {
                                foundSubscriptions.add(subscription);

                                if (mDebug) {
                                    if (unstable != null) {
                                        unregister |= true;
                                        Log.d(TAG, "Unregister:" + subscriber);
                                    } else {
                                        Log.d(TAG, "Unregister cleared subscriber:" + subscriber);
                                    }
                                }
                            }
                        }
                    }
                    subscriptions.removeAll(foundSubscriptions);
                }

                if (subscriptions == null || subscriptions.size() == 0) {
                    iterator.remove();
                }
            }
        }

        if (mDebug && unregister) Log.d(TAG, "Unregister:" + object);
    }

    /**
     * Posts an event to the event bus.
     *
     * @param data the event data.
     */
    public void post(Object... data) {
        post(DEFAULT_TAG, data);
    }

    /**
     * Posts an event to the event bus.
     *
     * @param tag  the event tag.
     * @param data the event data.
     */
    public void post(String tag, Object... data) {
        post(Event.obtain(this, tag, data));
    }

    /**
     * Posts the given event to the event bus.
     *
     * @param event The event.
     */
    public void post(@NonNull Event event) {
        if (event.isInUse()) {
            throw new IllegalStateException("Cannot post this event because it "
                    + "is still in use.");
        }

        if (event.getData() != null && event.getData().length > mMaxDataLength) {
            throw new IllegalStateException(
                    "Data length > " + mMaxDataLength + " : " + event.getData().length);
        }

        event.requestUse();

        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (mSubscriptionsMap) {
            subscriptions = mSubscriptionsMap.get(event.getTag());
        }

        mEventDispatcher.get().dispatchEvent(subscriptions, event);
        event.requestRecycle();
    }

    /**
     * 取消当前线程中正在Post的事件。
     * 注意：该方法只能在事件订阅方法和{@link EventHandler#handlerEvent(Subscriber, Event)}方法内调用。
     *
     * @throws IllegalStateException 如果当前线程中没有事件正在Post.
     */
    public void cancelCurrentPostingEvent() {
        EventDispatcher eventDispatcher = mEventDispatcher.get();
        if (!eventDispatcher.isPosting)
            throw new IllegalStateException("This method may only be called from inside event" +
                    " handling methods on the posting thread");

        eventDispatcher.isCanceled = true;
    }

    @Override
    public String toString() {
        return "[EventBus \'" + mDescriptor + "\']";
    }

    /**
     * Event dispatcher.
     */
    static class EventDispatcher {
        /**
         * Queues of events to dispatch.
         */
        Queue<Event> queue = new ConcurrentLinkedQueue<>();
        boolean isPosting;
        boolean isCanceled;

        void dispatchEvent(List<Subscription> subscriptions, Event event) {
            if (subscriptions == null || subscriptions.size() == 0)
                return;

            Queue<Event> eventsQueue = queue;
            eventsQueue.offer(event);
            if (isPosting)
                return;

            isPosting = true;
            try {
                while (!eventsQueue.isEmpty()) {
                    Event e = eventsQueue.poll();
                    for (Subscription subscription : subscriptions) {
                        if (isCanceled)
                            break;

                        Subscriber subscriber = subscription.subscriber;
                        if (subscriber instanceof UnstableSubscriber &&
                                ((UnstableSubscriber) subscriber).getUnstable() == null) {
                            continue;
                        }

                        dispatch(subscription, e);
                    }

                    isCanceled = false;
                }
            } finally {
                isPosting = false;
                isCanceled = false;
            }
        }

        void dispatch(Subscription subscription, Event event) {
            Subscriber subscriber = subscription.subscriber;
            if ((subscriber instanceof MethodSubscriber) &&
                    !((MethodSubscriber) subscriber).accept(event)) {
                return;
            }

            event.requestUse();
            subscription.eventHandler.handlerEvent(subscriber, event);
        }

    }

    /**
     * Event bus builder.
     */
    public static class Builder {

        String descriptor;
        boolean debug;
        int maxDataLength = DEFAULT_MAX_DATA_LENGTH;
        EventHandler defaultHandler;
        Executor executor;

        public Builder(String descriptor) {
            this.descriptor = descriptor;
        }

        /**
         * @param length The max data length. default={@value DEFAULT_MAX_DATA_LENGTH}.
         * @return The builder.
         * @throws IllegalStateException if <code>length < 0</code>.
         */
        public Builder setMaxDataLength(int length) {
            if (length < 0) {
                throw new IllegalStateException("length < 0 : " + length);
            }

            maxDataLength = length;
            return this;
        }

        /**
         * @param handler The event handler.
         * @return The builder.
         * @throws NullPointerException if handler is null.
         */
        @SuppressWarnings("all")
        public Builder setDefaultEventHandler(@NonNull EventHandler handler) {
            if (handler == null) {
                throw new NullPointerException("handler = null.");
            }

            defaultHandler = handler;
            return this;
        }

        /**
         * @param executor The executor.
         * @return The builder.
         * @throws NullPointerException if executor is null.
         */
        @SuppressWarnings("all")
        public Builder setExecutor(@NonNull Executor executor) {
            if (executor == null) {
                throw new NullPointerException("handler = null.");
            }

            this.executor = executor;
            return this;
        }

        public Builder setDebug(boolean flag) {
            debug = flag;
            return this;
        }

        public EventBus create() {
            return new EventBus(this);
        }

    }

}
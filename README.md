#AndroidEventBus

类似于[EventBus](https://github.com/greenrobot/EventBus)，但并非是使用JAVA反射来实现的，支持事件Tag、空参数、多参数。



```java
eventBus.register(this);
```

```java
@Subscribe
void onEvent(AnyEventType event) {/* Do something */}

@Subscribe(tag = "tag", mode = ThreadMode.MAIN)
void onEvent(AnyEventDataType eventData1, AnyEventDataType eventData2, ...) {
  /* Do something */
}

@Subscribe(tag = {"tag", "tag1", "tag2", ...}, mode = ThreadMode.MAIN)
void onEvent(@Tag String tag, AnyEventDataType eventData, ...) {
  /* Do something */
}
```

```java
eventBus.post(event);
eventBus.post("tag", event);
eventBus.post("tag", eventData1, eventData2, ...);
```


package com.bwts.oestest.entity;

import stormpot.Poolable;
import stormpot.Slot;

/**
 * Created by Javie on 16/4/11.
 */
public class MyObject implements Poolable {

    private Slot slot;

    public MyObject(Slot slot) {
        this.slot = slot;
    }
    public MyObject() {
    }

    public String shout() {
        return "This is my object ";
    }

    @Override
    public void release() {
        slot.release(this);
    }
}

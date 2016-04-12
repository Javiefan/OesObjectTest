package com.bwts.oestest.entity;

import stormpot.Allocator;
import stormpot.Slot;

/**
 * Created by Javie on 16/4/11.
 */
public class MyObjectAllocator implements Allocator<MyObject> {

    @Override
    public MyObject allocate(Slot slot) throws Exception {
        return new MyObject(slot);
    }

    @Override
    public void deallocate(MyObject poolable) throws Exception {

    }
}

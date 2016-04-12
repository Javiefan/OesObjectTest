package com.nisec.oes;

import stormpot.Allocator;
import stormpot.Slot;

public class OesAllocator implements Allocator<Oes>{
    @Override
    public Oes allocate(Slot slot) throws Exception {
        return new Oes(slot);
    }

    @Override
    public void deallocate(Oes poolable) throws Exception {

    }
}

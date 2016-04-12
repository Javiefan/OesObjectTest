package com.nisec.oes;

import stormpot.Poolable;
import stormpot.Slot;

/**
 * Created by Javie on 16/4/11.
 */
public class Oes extends oes_api_jni implements Poolable {
    private Slot slot;

    public Oes(Slot slot) {
        this.slot = slot;
    }

    public Oes() {}

    @Override
    public void release() {
        slot.release(this);
    }
}

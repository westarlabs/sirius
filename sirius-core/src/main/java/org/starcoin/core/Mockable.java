package org.starcoin.core;

public interface Mockable {

    /**
     * Support mock random data, just for test.
     */
    void mock(MockContext context);

}

package org.kotemaru.android.camera2sample;

/**
 * Created by tomoki on 2015/07/15.
 */
public class Fuck {

    private String key = null;
    private String value = null;

    public Fuck(String k, String v)
    {
        key = k;
        value = v;
    }
    public boolean comp(String k)
    {
        return key.equals(k);
    }

    public String getValue()
    {
        return value;
    }
}
